package com.sentinel.app.feature.map.tiles

import android.content.Context
import android.util.Log
import com.sentinel.app.BuildConfig
import com.sentinel.app.core.maps.HomeZoneRepository
import com.sentinel.app.core.network.ConnectivityObserver
import com.sentinel.app.core.network.NetworkConnectivityObserver
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Embedded local HTTP tile proxy server running on 127.0.0.1.
 * Mediates all MapLibre tile requests and enforces the strict hybrid Source Order:
 *
 * 1. Home Pack: Always used for tiles intersecting the user's Home Zone bbox (offline or online).
 * 2. Live Source: Fetched from BuildConfig.TILES_BASE_URL when online outside Home Zone.
 * 3. India Base: Bundled coarse fallback for the rest of India when offline outside Home Zone.
 * 4. Overzoom: Returns HTTP 204 to trigger parent tile overzoom without errors or blank screens.
 *
 * Strictly NEVER contacts tile.openstreetmap.org.
 */
class LocalTileServer(
    private val context: Context,
    private val homeZoneRepo: HomeZoneRepository = HomeZoneRepository.getInstance(context)
) {

    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newFixedThreadPool(8)
    private var isRunning = false
    var serverPort: Int = 0
        private set

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var homePackReader: MbtilesReader? = null
    private var baseIndiaReader: MbtilesReader? = null

    private val connectivityObserver: ConnectivityObserver = NetworkConnectivityObserver(context)
    private var isOnlineCached = false

    private val tilePattern = Pattern.compile("^/tiles/(\\d+)/(\\d+)/(\\d+)\\.pbf.*")

    fun start(): Int {
        if (isRunning && serverPort > 0) return serverPort

        try {
            // Bind to loopback interface on an ephemeral port
            val ss = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            serverSocket = ss
            serverPort = ss.localPort
            isRunning = true

            initReaders()

            executor.execute {
                while (isRunning && !ss.isClosed) {
                    try {
                        val client = ss.accept()
                        executor.execute { handleClient(client) }
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            }
            Log.i(TAG, "LocalTileServer running at http://127.0.0.1:$serverPort/")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocalTileServer: ${e.message}", e)
        }
        return serverPort
    }

    fun getTileUrlTemplate(): String {
        return "http://127.0.0.1:$serverPort/tiles/{z}/{x}/{y}.pbf"
    }

    fun reloadReaders() {
        initReaders()
    }

    private fun initReaders() {
        // Home pack reader
        val homePackFile = homeZoneRepo.getHomePackFile()
        if (homePackFile.exists() && homePackFile.name.endsWith(".mbtiles")) {
            homePackReader?.close()
            homePackReader = MbtilesReader(homePackFile)
        }

        // Base India reader (assets or filesDir)
        val mapsDir = File(context.filesDir, "maps")
        val baseIndiaFile = File(mapsDir, "india_base.mbtiles")
        if (baseIndiaFile.exists()) {
            baseIndiaReader?.close()
            baseIndiaReader = MbtilesReader(baseIndiaFile)
        }
    }

    fun updateOnlineState(online: Boolean) {
        isOnlineCached = online
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10000
            val input = socket.getInputStream()
            val requestLine = readLine(input) ?: return

            val parts = requestLine.split(" ")
            if (parts.size < 2 || parts[0] != "GET") {
                sendResponse(socket.getOutputStream(), 400, "Bad Request", null)
                return
            }

            val path = parts[1]
            val matcher = tilePattern.matcher(path)
            if (!matcher.matches()) {
                sendResponse(socket.getOutputStream(), 404, "Not Found", null)
                return
            }

            val z = matcher.group(1)?.toIntOrNull() ?: 0
            val x = matcher.group(2)?.toIntOrNull() ?: 0
            val y = matcher.group(3)?.toIntOrNull() ?: 0

            val tileBytes = resolveTile(z, x, y)
            if (tileBytes != null && tileBytes.isNotEmpty()) {
                sendTileResponse(socket.getOutputStream(), tileBytes)
            } else {
                // Return 204 No Content for overzoom handling without client error
                sendResponse(socket.getOutputStream(), 204, "No Content", null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling client request: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun resolveTile(z: Int, x: Int, y: Int): ByteArray? {
        val homeZone = homeZoneRepo.getHomeZone()
        val hasHomePack = homeZoneRepo.getHomePackFile().exists()
        val hasLive = BuildConfig.TILES_BASE_URL.isNotBlank()
        val hasBase = baseIndiaReader != null

        val choice = SourceOrderManager.selectSource(
            z = z,
            x = x,
            y = y,
            homeZone = homeZone,
            isOnline = isOnlineCached,
            hasHomePack = hasHomePack,
            hasLiveSource = hasLive,
            hasIndiaBase = hasBase
        )

        when (choice) {
            TileSourceChoice.HOME_PACK -> {
                homePackReader?.getTile(z, x, y)?.let { return it }
                // If not in local home reader, try live if online
                if (isOnlineCached && hasLive) {
                    fetchLiveTile(z, x, y)?.let { return it }
                }
                // Fall back to base
                return baseIndiaReader?.getTile(z, x, y)
            }
            TileSourceChoice.LIVE_SOURCE -> {
                fetchLiveTile(z, x, y)?.let { return it }
                // Fallback to base on live failure
                return baseIndiaReader?.getTile(z, x, y)
            }
            TileSourceChoice.INDIA_BASE -> {
                return baseIndiaReader?.getTile(z, x, y)
            }
            TileSourceChoice.OVERZOOM_OR_EMPTY -> {
                return null
            }
        }
    }

    private fun fetchLiveTile(z: Int, x: Int, y: Int): ByteArray? {
        val baseUrl = BuildConfig.TILES_BASE_URL
        if (baseUrl.isBlank()) return null

        val url = if (baseUrl.contains("{z}")) {
            baseUrl.replace("{z}", z.toString())
                .replace("{x}", x.toString())
                .replace("{y}", y.toString())
        } else {
            "${baseUrl.trimEnd('/')}/$z/$x/$y.pbf"
        }

        return try {
            val reqBuilder = Request.Builder().url(url)
            if (BuildConfig.TILES_API_KEY.isNotBlank()) {
                reqBuilder.addHeader("x-api-key", BuildConfig.TILES_API_KEY)
            }
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.bytes()
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sendTileResponse(output: OutputStream, bytes: ByteArray) {
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/x-protobuf\r\n" +
                "Content-Encoding: gzip\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Cache-Control: max-age=86400\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendResponse(output: OutputStream, code: Int, status: String, body: String?) {
        val bodyBytes = body?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        val header = "HTTP/1.1 $code $status\r\n" +
                "Content-Type: text/plain\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        if (bodyBytes.isNotEmpty()) output.write(bodyBytes)
        output.flush()
    }

    private fun readLine(input: InputStream): String? {
        val baos = ByteArrayOutputStream()
        var c: Int
        while (input.read().also { c = it } != -1) {
            if (c == '\n'.code) break
            if (c != '\r'.code) baos.write(c)
        }
        return if (baos.size() == 0 && c == -1) null else baos.toString("UTF-8")
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        homePackReader?.close()
        baseIndiaReader?.close()
    }

    companion object {
        private const val TAG = "LocalTileServer"

        @Volatile
        private var INSTANCE: LocalTileServer? = null

        fun getInstance(context: Context): LocalTileServer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalTileServer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
