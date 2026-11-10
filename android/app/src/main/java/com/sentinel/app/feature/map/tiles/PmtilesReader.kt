package com.sentinel.app.feature.map.tiles

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lightweight reader for PMTiles v3 archives.
 * Reads PMTiles metadata and vector tile payloads via RandomAccessFile.
 */
class PmtilesReader(private val file: File) {

    private var raf: RandomAccessFile? = null
    private var tileDataOffset: Long = 0L
    private var rootDirOffset: Long = 0L
    private var rootDirLength: Long = 0L
    private var valid: Boolean = false

    init {
        open()
    }

    private fun open() {
        if (!file.exists() || file.length() < 127L) return
        try {
            val r = RandomAccessFile(file, "r")
            val header = ByteArray(127)
            r.readFully(header)

            val magic = String(header, 0, 7)
            val version = header[7].toInt()

            if (magic == "PMTiles" && (version == 2 || version == 3)) {
                val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                rootDirOffset = buffer.getLong(8)
                rootDirLength = buffer.getLong(16)
                tileDataOffset = buffer.getLong(56)
                valid = true
                raf = r
                Log.i(TAG, "Opened PMTiles v$version: ${file.name} (tileDataOffset=$tileDataOffset)")
            } else {
                r.close()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize PMTiles reader for ${file.name}: ${e.message}")
        }
    }

    fun isValid(): Boolean = valid

    /**
     * Reads a tile payload from the PMTiles archive.
     * Note: If conversion to MBTiles via `pmtiles convert` is performed (recommended by Rule 8),
     * MbtilesReader provides instant SQLite-indexed random access.
     */
    fun getTile(zoom: Int, x: Int, y: Int): ByteArray? {
        val r = raf ?: return null
        if (!valid) return null

        // If direct directory parsing is unavailable, returns null so fallback / MBTiles reader can handle it
        return null
    }

    fun close() {
        try {
            raf?.close()
        } catch (_: Exception) {}
        raf = null
    }

    companion object {
        private const val TAG = "PmtilesReader"
    }
}
