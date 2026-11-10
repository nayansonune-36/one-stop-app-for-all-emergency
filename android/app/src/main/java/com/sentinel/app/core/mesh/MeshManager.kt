package com.sentinel.app.core.mesh

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.sentinel.app.BuildConfig
import com.sentinel.app.core.network.ConnectivityObserver
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Manages Google Nearby Connections mesh networking (P2P_CLUSTER),
 * multi-hop packet relaying (max 8 hops), deduplication, and online gateway syncing.
 */
class MeshManager(
    private val context: Context,
    private val connectivityObserver: ConnectivityObserver,
    private val retryQueue: SosRetryQueue = SosRetryQueue(context),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    companion object {
        const val TAG = "SENTINEL_MESH"
        const val SERVICE_ID = "com.sentinel.app.mesh"
        val STRATEGY: Strategy = Strategy.P2P_CLUSTER
        const val MAX_HOPS = 8
        const val MAX_SEEN_IDS = 1000
    }

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val myEndpointName = "SENTINEL-${Build.MODEL.replace(" ", "").take(6)}-${UUID.randomUUID().toString().take(4)}"

    // State flows for UI and observers
    private val _connectedPeers = MutableStateFlow<List<String>>(emptyList())
    val connectedPeers: StateFlow<List<String>> = _connectedPeers.asStateFlow()

    private val _latestReceivedSos = MutableStateFlow<SosMessage?>(null)
    val latestReceivedSos: StateFlow<SosMessage?> = _latestReceivedSos.asStateFlow()

    private val _seenMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val seenMessageIds: StateFlow<Set<String>> = _seenMessageIds.asStateFlow()

    private val _packetLogs = MutableStateFlow<List<String>>(emptyList())
    val packetLogs: StateFlow<List<String>> = _packetLogs.asStateFlow()

    private val _isMeshActive = MutableStateFlow(false)
    val isMeshActive: StateFlow<Boolean> = _isMeshActive.asStateFlow()

    private val _sentDeviceCount = MutableStateFlow(0)
    val sentDeviceCount: StateFlow<Int> = _sentDeviceCount.asStateFlow()

    private val _isDeliveredToServer = MutableStateFlow(false)
    val isDeliveredToServer: StateFlow<Boolean> = _isDeliveredToServer.asStateFlow()

    // Bounded set of seen SOS message UUIDs
    private val seenIds = LinkedHashSet<String>()
    private val pendingEndpoints = mutableSetOf<String>()
    private var periodicDiscoveryJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    init {
        // Observe connectivity changes to flush local offline retry queue
        scope.launch {
            connectivityObserver.isOnline.collect { isOnline ->
                if (isOnline) {
                    addLog("Internet connection active, flushing pending gateway queue...")
                    flushRetryQueue()
                }
            }
        }
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$time] $message"
        Log.d(TAG, logEntry)
        _packetLogs.value = (_packetLogs.value + logEntry).takeLast(100)
    }

    /**
     * Starts advertising and discovery concurrently to join the peer-to-peer cluster.
     */
    fun startMesh(forceRestart: Boolean = false) {
        if (!MeshPermissionHelper.hasAllPermissions(context)) {
            addLog("Missing mesh permissions, cannot start advertising/discovery")
            return
        }

        if (_isMeshActive.value && !forceRestart) return

        _isMeshActive.value = true
        addLog("Starting mesh node ($myEndpointName) with SERVICE_ID: $SERVICE_ID")

        try {
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
        } catch (_: Exception) {}

        startAdvertising()
        startDiscovery()
        startPeriodicDiscovery()
    }

    /**
     * Stops advertising, discovery, and disconnects all active peers.
     */
    fun stopMesh() {
        try {
            periodicDiscoveryJob?.cancel()
            periodicDiscoveryJob = null
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()
            _connectedPeers.value = emptyList()
            pendingEndpoints.clear()
            _isMeshActive.value = false
            addLog("Mesh node stopped")
        } catch (e: Exception) {
            addLog("Error stopping mesh: ${e.message}")
        }
    }

    private fun startPeriodicDiscovery() {
        periodicDiscoveryJob?.cancel()
        periodicDiscoveryJob = scope.launch {
            while (_isMeshActive.value) {
                delay(15000)
                if (_isMeshActive.value && _connectedPeers.value.isEmpty()) {
                    addLog("Refreshing discovery scan...")
                    try {
                        connectionsClient.stopDiscovery()
                        delay(400)
                        if (_isMeshActive.value) {
                            startDiscovery()
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Discovery refresh error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        connectionsClient.startAdvertising(
            myEndpointName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            addLog("Advertising started successfully")
        }.addOnFailureListener { e ->
            val code = (e as? ApiException)?.statusCode ?: -1
            addLog("Failed to start advertising ($code): ${e.message}")
        }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            addLog("Discovery started successfully")
        }.addOnFailureListener { e ->
            val code = (e as? ApiException)?.statusCode ?: -1
            addLog("Failed to start discovery ($code): ${e.message}")
        }
    }

    // Callbacks for discovering peers
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            addLog("Discovered peer: ${info.endpointName} ($endpointId)")
            if (_connectedPeers.value.contains(endpointId) || pendingEndpoints.contains(endpointId)) {
                return
            }

            // Deterministic tie-breaker:
            // Compare endpoint names lexicographically.
            // Node with smaller name initiates connection request.
            // Node with larger name waits for incoming request to avoid bidirectional clash.
            val shouldInitiate = myEndpointName < info.endpointName
            if (shouldInitiate) {
                pendingEndpoints.add(endpointId)
                addLog("Initiating connection to ${info.endpointName} ($endpointId)...")
                connectionsClient.requestConnection(
                    myEndpointName,
                    endpointId,
                    connectionLifecycleCallback
                ).addOnFailureListener { e ->
                    val code = (e as? ApiException)?.statusCode ?: -1
                    pendingEndpoints.remove(endpointId)
                    addLog("Failed to request connection to $endpointId ($code): ${e.message}")
                }
            } else {
                addLog("Discovered ${info.endpointName}, waiting for peer to initiate connection...")
                // Fallback: If no connection initiated by peer within 4s, attempt connection
                scope.launch {
                    delay(4000)
                    if (_isMeshActive.value && !_connectedPeers.value.contains(endpointId) && !pendingEndpoints.contains(endpointId)) {
                        pendingEndpoints.add(endpointId)
                        addLog("Fallback initiating connection to $endpointId...")
                        connectionsClient.requestConnection(
                            myEndpointName,
                            endpointId,
                            connectionLifecycleCallback
                        ).addOnFailureListener { e ->
                            val code = (e as? ApiException)?.statusCode ?: -1
                            pendingEndpoints.remove(endpointId)
                            addLog("Fallback request failed for $endpointId ($code): ${e.message}")
                        }
                    }
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            addLog("Lost visibility of peer $endpointId")
            pendingEndpoints.remove(endpointId)
        }
    }

    // Callbacks for connection lifecycle
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            addLog("Connection initiated from ${info.endpointName} ($endpointId), auto-accepting...")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnFailureListener { e ->
                    val code = (e as? ApiException)?.statusCode ?: -1
                    addLog("Failed to accept connection from $endpointId ($code): ${e.message}")
                }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            pendingEndpoints.remove(endpointId)
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    addLog("Successfully connected to peer: $endpointId")
                    if (!_connectedPeers.value.contains(endpointId)) {
                        _connectedPeers.value = _connectedPeers.value + endpointId
                    }
                }
                ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT -> {
                    addLog("Already connected to peer: $endpointId")
                    if (!_connectedPeers.value.contains(endpointId)) {
                        _connectedPeers.value = _connectedPeers.value + endpointId
                    }
                }
                else -> {
                    addLog("Connection rejected/failed for $endpointId: code ${resolution.status.statusCode} ${resolution.status.statusMessage}")
                    _connectedPeers.value = _connectedPeers.value - endpointId
                    // Restart discovery after brief delay so devices can rediscover each other
                    scope.launch {
                        delay(2000)
                        if (_isMeshActive.value && _connectedPeers.value.isEmpty()) {
                            try {
                                connectionsClient.stopDiscovery()
                                delay(300)
                                if (_isMeshActive.value) {
                                    startDiscovery()
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            addLog("Peer disconnected: $endpointId")
            pendingEndpoints.remove(endpointId)
            _connectedPeers.value = _connectedPeers.value - endpointId
            // Restart discovery to reconnect when peer comes back
            scope.launch {
                delay(1000)
                if (_isMeshActive.value && _connectedPeers.value.isEmpty()) {
                    try {
                        connectionsClient.stopDiscovery()
                        delay(300)
                        if (_isMeshActive.value) {
                            startDiscovery()
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Callbacks for receiving payloads
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                val sos = SosMessage.fromByteArray(bytes) ?: return
                handleIncomingSos(sos, senderEndpointId = endpointId)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Transfers are instantaneous for small byte arrays
        }
    }

    /**
     * Broadcasts a locally generated emergency SOS alert to all connected peers and queues for gateway sync.
     */
    fun broadcastSos(sos: SosMessage) {
        _isDeliveredToServer.value = false
        _sentDeviceCount.value = 0
        markAsSeen(sos.id)
        _latestReceivedSos.value = sos
        addLog("Initiating SOS broadcast: id=${sos.id}, lat=${sos.lat}, lon=${sos.lon}")

        val peers = _connectedPeers.value
        if (peers.isNotEmpty()) {
            sendPayloadToPeers(peers, sos)
            _sentDeviceCount.value = peers.size
            addLog("Broadcasted SOS ${sos.id} directly to ${peers.size} peer(s)")
        } else {
            addLog("No active peers connected. Beacon queued locally for mesh/gateway relay.")
        }

        // Push to local retry queue and attempt upload if online
        scope.launch {
            retryQueue.enqueue(sos)
            flushRetryQueue()
        }
    }

    fun resetDeliveryState() {
        _isDeliveredToServer.value = false
        _sentDeviceCount.value = 0
    }

    /**
     * Multi-hop relay and gateway ingestion for received SOS messages.
     */
    private fun handleIncomingSos(sos: SosMessage, senderEndpointId: String) {
        val isNew = markAsSeen(sos.id)
        if (!isNew) {
            addLog("Dropped duplicate SOS packet: ${sos.id}")
            return
        }

        _latestReceivedSos.value = sos
        addLog("Received new SOS ${sos.id} (hops: ${sos.hops}) from peer $senderEndpointId")

        // 1. Gateway upload (if this device has internet access)
        scope.launch {
            retryQueue.enqueue(sos)
            flushRetryQueue()
        }

        // 2. Multi-hop relay forward (if hops < MAX_HOPS)
        if (sos.hops < MAX_HOPS) {
            val forwardedSos = sos.copy(hops = sos.hops + 1)
            val recipientPeers = _connectedPeers.value.filter { it != senderEndpointId }
            if (recipientPeers.isNotEmpty()) {
                sendPayloadToPeers(recipientPeers, forwardedSos)
                addLog("Relayed SOS ${sos.id} (hop count: ${forwardedSos.hops}) to ${recipientPeers.size} peer(s)")
            } else {
                addLog("No other peers to forward SOS ${sos.id}")
            }
        } else {
            addLog("SOS ${sos.id} reached maximum hop limit ($MAX_HOPS), halting relay")
        }
    }

    private fun sendPayloadToPeers(peerIds: List<String>, message: SosMessage) {
        try {
            val payload = Payload.fromBytes(message.toByteArray())
            connectionsClient.sendPayload(peerIds, payload)
        } catch (e: Exception) {
            addLog("Failed to send payload to peers: ${e.message}")
        }
    }

    private fun markAsSeen(sosId: String): Boolean {
        synchronized(seenIds) {
            if (seenIds.contains(sosId)) {
                return false
            }
            if (seenIds.size >= MAX_SEEN_IDS) {
                val oldest = seenIds.iterator().next()
                seenIds.remove(oldest)
            }
            seenIds.add(sosId)
            _seenMessageIds.value = seenIds.toSet()
            return true
        }
    }

    /**
     * Flushes the persistent offline retry queue to the backend server when online.
     */
    suspend fun flushRetryQueue() {
        if (!connectivityObserver.isOnline.value) {
            return
        }

        val pending = retryQueue.getAll()
        if (pending.isEmpty()) return

        withContext(Dispatchers.IO) {
            val endpointUrl = "${BuildConfig.BACKEND_BASE_URL.trimEnd('/')}/sos"
            for (sos in pending) {
                try {
                    val body = sos.toByteArray().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url(endpointUrl)
                        .post(body)
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            retryQueue.remove(sos.id)
                            _isDeliveredToServer.value = true
                            addLog("Successfully synced SOS ${sos.id} to gateway ($endpointUrl)")
                        } else {
                            addLog("Gateway responded with error ${response.code} for SOS ${sos.id}")
                        }
                    }
                } catch (e: Exception) {
                    addLog("Gateway sync failed for SOS ${sos.id}: ${e.message}")
                }
            }
        }
    }
}
