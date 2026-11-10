package com.sentinel.app.feature.sos

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.sentinel.app.core.medical.DefaultMedicalSummaryProvider
import com.sentinel.app.core.medical.MedicalSummaryProvider
import com.sentinel.app.core.mesh.MeshManager
import com.sentinel.app.core.mesh.MeshPermissionHelper
import com.sentinel.app.core.mesh.SosMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

enum class SosStatusStep {
    IDLE,
    SEARCHING,
    SENT_TO_DEVICES,
    DELIVERED_SERVER
}

data class SosUiState(
    val isBroadcasting: Boolean = false,
    val statusStep: SosStatusStep = SosStatusStep.IDLE,
    val peersCount: Int = 0,
    val sentDeviceCount: Int = 0,
    val isDeliveredToServer: Boolean = false,
    val hops: Int = 0,
    val elapsedSeconds: Long = 0L,
    val hasPermissions: Boolean = false,
    val isBluetoothEnabled: Boolean = true,
    val isLocationEnabled: Boolean = true,
    val showConfirmDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val showDebugPanel: Boolean = false,
    val activeSosId: String? = null,
    val lastKnownLat: Double? = null,
    val lastKnownLon: Double? = null,
    val locationUnavailable: Boolean = false,
    val connectedPeers: List<String> = emptyList(),
    val seenMessageIds: Set<String> = emptySet(),
    val packetLogs: List<String> = emptyList()
)

private data class MeshPeerSnapshot(
    val peers: List<String>,
    val seenIds: Set<String>,
    val logs: List<String>,
    val sentCount: Int
)

class SosViewModel(
    private val context: Context,
    private val meshManager: MeshManager,
    private val medicalSummaryProvider: MedicalSummaryProvider = DefaultMedicalSummaryProvider()
) : ViewModel() {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow(
        SosUiState(hasPermissions = MeshPermissionHelper.hasAllPermissions(context))
    )
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    init {
        checkPermissions()
        refreshHardwareStates()

        viewModelScope.launch {
            val peerFlow = combine(
                meshManager.connectedPeers,
                meshManager.seenMessageIds,
                meshManager.packetLogs,
                meshManager.sentDeviceCount
            ) { peers, seenIds, logs, sentCount ->
                MeshPeerSnapshot(peers, seenIds, logs, sentCount)
            }

            peerFlow.combine(meshManager.isDeliveredToServer) { snapshot, delivered ->
                Pair(snapshot, delivered)
            }.collect { (snapshot, delivered) ->
                _uiState.update { current ->
                    val step = when {
                        !current.isBroadcasting -> SosStatusStep.IDLE
                        delivered -> SosStatusStep.DELIVERED_SERVER
                        snapshot.sentCount > 0 -> SosStatusStep.SENT_TO_DEVICES
                        else -> SosStatusStep.SEARCHING
                    }
                    current.copy(
                        peersCount = snapshot.peers.size,
                        connectedPeers = snapshot.peers,
                        seenMessageIds = snapshot.seenIds,
                        packetLogs = snapshot.logs,
                        sentDeviceCount = snapshot.sentCount,
                        isDeliveredToServer = delivered,
                        statusStep = step
                    )
                }
            }
        }
    }

    fun refreshHardwareStates() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val btEnabled = bluetoothManager?.adapter?.isEnabled == true

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        _uiState.update {
            it.copy(
                isBluetoothEnabled = btEnabled,
                isLocationEnabled = locEnabled
            )
        }
    }

    fun checkPermissions() {
        val granted = MeshPermissionHelper.hasAllPermissions(context)
        _uiState.update { it.copy(hasPermissions = granted) }
        if (granted) {
            meshManager.startMesh()
        }
    }

    fun restartMesh() {
        checkPermissions()
        refreshHardwareStates()
        meshManager.startMesh(forceRestart = true)
    }

    fun onPermissionsResult() {
        checkPermissions()
        refreshHardwareStates()
    }

    fun showConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    fun openCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = true) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false) }
    }

    fun toggleDebugPanel() {
        _uiState.update { it.copy(showDebugPanel = !it.showDebugPanel) }
    }

    fun dismissDebugPanel() {
        _uiState.update { it.copy(showDebugPanel = false) }
    }

    /**
     * Broadcasts emergency SOS with current GPS coordinates and Medical ID summary.
     */
    @SuppressLint("MissingPermission")
    fun triggerSos() {
        refreshHardwareStates()
        _uiState.update {
            it.copy(
                showConfirmDialog = false,
                isBroadcasting = true,
                elapsedSeconds = 0L
            )
        }

        // Start elapsed seconds timer
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var elapsed = 0L
            while (true) {
                delay(1000L)
                elapsed++
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
            }
        }

        viewModelScope.launch {
            var lat: Double? = null
            var lon: Double? = null
            var unavailable = false

            try {
                if (MeshPermissionHelper.hasAllPermissions(context)) {
                    val location: Location? = fusedLocationClient.lastLocation.await()
                    if (location != null) {
                        lat = location.latitude
                        lon = location.longitude
                    } else {
                        unavailable = true
                    }
                } else {
                    unavailable = true
                }
            } catch (_: Exception) {
                unavailable = true
            }

            _uiState.update {
                it.copy(
                    lastKnownLat = lat,
                    lastKnownLon = lon,
                    locationUnavailable = unavailable || lat == null
                )
            }

            val medicalSummary = medicalSummaryProvider.getMedicalSummary()
            val sos = SosMessage(
                id = UUID.randomUUID().toString(),
                ts = System.currentTimeMillis(),
                lat = lat ?: 0.0,
                lon = lon ?: 0.0,
                hops = 0,
                name = "SENTINEL-${Build.MODEL}",
                medical = medicalSummary
            )

            _uiState.update { it.copy(activeSosId = sos.id, hops = 0) }
            meshManager.broadcastSos(sos)
        }
    }

    fun cancelSos() {
        timerJob?.cancel()
        timerJob = null
        meshManager.resetDeliveryState()
        _uiState.update {
            it.copy(
                isBroadcasting = false,
                activeSosId = null,
                showCancelDialog = false,
                elapsedSeconds = 0L,
                statusStep = SosStatusStep.IDLE
            )
        }
    }

    companion object {
        fun provideFactory(
            context: Context,
            meshManager: MeshManager,
            medicalSummaryProvider: MedicalSummaryProvider = DefaultMedicalSummaryProvider()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SosViewModel(context.applicationContext, meshManager, medicalSummaryProvider) as T
            }
        }
    }
}
