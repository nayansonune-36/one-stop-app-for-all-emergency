package com.sentinel.app.feature.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sentinel.app.core.maps.HomePackStatus
import com.sentinel.app.core.maps.HomeZone
import com.sentinel.app.core.maps.HomeZoneRepository
import com.sentinel.app.core.network.ConnectivityObserver
import com.sentinel.app.core.network.NetworkConnectivityObserver
import com.sentinel.app.feature.map.data.MapAssetManager
import com.sentinel.app.feature.map.data.ShelterRepository
import com.sentinel.app.feature.map.model.Shelter
import com.sentinel.app.feature.map.model.ShelterType
import com.sentinel.app.feature.map.tiles.LocalTileServer
import com.sentinel.app.feature.map.tiles.SourceOrderManager
import com.sentinel.app.feature.map.util.ShelterDistanceCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val allShelters: List<Shelter> = emptyList(),
    val filteredShelters: List<Shelter> = emptyList(),
    val availableStates: List<String> = emptyList(),
    val selectedStateFilter: String? = null,
    val selectedTypeFilter: ShelterType? = null,
    val selectedShelter: Shelter? = null,
    val selectedCluster: List<Shelter>? = null,
    val userLocation: Pair<Double, Double>? = null,
    val nearestShelter: Shelter? = null,
    val nearestDistanceKm: Double? = null,
    val hasLocationPermission: Boolean = false,
    val isEmptyRegistry: Boolean = false,
    val mapStyleUrl: String = MapAssetManager.OPENFREEMAP_STYLE_URL,
    val mapStyleJson: String = "",
    val cameraMoveEvent: Pair<Pair<Double, Double>, Double>? = null,
    val homeZone: HomeZone? = null,
    val homePackStatus: HomePackStatus = HomePackStatus.NotConfigured,
    val isOnline: Boolean = true,
    val isInsideHomeZone: Boolean = false,
    val showDetailedMapUnavailableCard: Boolean = false
)

class MapViewModel(
    private val shelterRepository: ShelterRepository,
    private val mapAssetManager: MapAssetManager,
    private val homeZoneRepository: HomeZoneRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val localTileServer: LocalTileServer
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        val styleJson = mapAssetManager.getOfflineStyleJson()
        mapAssetManager.prepareOfflineStorage()
        val loadedShelters = shelterRepository.loadShelters()
        val isEmpty = loadedShelters.isEmpty()
        val states = loadedShelters.map { it.state }.filter { it.isNotBlank() }.distinct().sorted()
        val initialHomeZone = homeZoneRepository.getHomeZone()

        _uiState.update { current ->
            current.copy(
                allShelters = loadedShelters,
                filteredShelters = loadedShelters,
                availableStates = states,
                isEmptyRegistry = isEmpty,
                mapStyleUrl = MapAssetManager.OPENFREEMAP_STYLE_URL,
                mapStyleJson = styleJson,
                homeZone = initialHomeZone,
                showDetailedMapUnavailableCard = false
            )
        }

        // Cache emergency shelters in HomeZone if set
        homeZoneRepository.cacheSheltersForHomeZone(loadedShelters)

        // Observe network connectivity
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                localTileServer.updateOnlineState(online)
                _uiState.update { current ->
                    current.copy(
                        isOnline = online,
                        showDetailedMapUnavailableCard = false
                    )
                }
            }
        }

        // Observe Home Pack status
        viewModelScope.launch {
            homeZoneRepository.homePackStatus.collect { status ->
                _uiState.update { it.copy(homePackStatus = status) }
            }
        }
    }

    fun onStateFilterSelected(state: String?) {
        _uiState.update { current ->
            val newState = if (state == current.selectedStateFilter) null else state
            val filtered = filterShelters(current.allShelters, newState, current.selectedTypeFilter)
            val nearest = calculateNearest(current.userLocation, filtered)

            val cameraTarget = if (newState == null) {
                // Tapping "All" centers on Guwahati at zoom 12 (never all-India zoom)
                Pair(Pair(26.1445, 91.7362), 12.0)
            } else {
                val regionShelters = current.allShelters.filter { it.state.equals(newState, ignoreCase = true) }
                if (regionShelters.isNotEmpty()) {
                    val avgLat = regionShelters.map { it.lat }.average()
                    val avgLon = regionShelters.map { it.lon }.average()
                    Pair(Pair(avgLat, avgLon), 12.0)
                } else {
                    Pair(Pair(26.1445, 91.7362), 12.0)
                }
            }

            current.copy(
                selectedStateFilter = newState,
                filteredShelters = filtered,
                nearestShelter = nearest?.first,
                nearestDistanceKm = nearest?.second,
                cameraMoveEvent = cameraTarget,
                selectedShelter = null,
                selectedCluster = null
            )
        }
    }

    fun onTypeFilterSelected(type: ShelterType?) {
        _uiState.update { current ->
            val newType = if (type == current.selectedTypeFilter) null else type
            val filtered = filterShelters(current.allShelters, current.selectedStateFilter, newType)
            val nearest = calculateNearest(current.userLocation, filtered)
            current.copy(
                selectedTypeFilter = newType,
                filteredShelters = filtered,
                nearestShelter = nearest?.first,
                nearestDistanceKm = nearest?.second,
                selectedShelter = null,
                selectedCluster = null
            )
        }
    }

    fun onShelterSelected(shelter: Shelter?) {
        _uiState.update { it.copy(selectedShelter = shelter, selectedCluster = null) }
    }

    fun onClusterSelected(cluster: List<Shelter>?) {
        _uiState.update { it.copy(selectedCluster = cluster, selectedShelter = null) }
    }

    fun onCameraMoveHandled() {
        _uiState.update { it.copy(cameraMoveEvent = null) }
    }

    fun onLocationUpdated(lat: Double, lon: Double) {
        val location = Pair(lat, lon)
        _uiState.update { current ->
            val nearest = calculateNearest(location, current.filteredShelters)
            val insideHome = SourceOrderManager.isInsideHomeZone(lat, lon, current.homeZone)
            val showCard = !current.isOnline && !insideHome
            current.copy(
                userLocation = location,
                nearestShelter = nearest?.first,
                nearestDistanceKm = nearest?.second,
                isInsideHomeZone = insideHome,
                showDetailedMapUnavailableCard = showCard
            )
        }
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
    }

    fun refreshHomeZone() {
        val updatedZone = homeZoneRepository.getHomeZone()
        _uiState.update { current ->
            val insideHome = SourceOrderManager.isInsideHomeZone(
                current.userLocation?.first ?: (updatedZone?.centerLat ?: 0.0),
                current.userLocation?.second ?: (updatedZone?.centerLon ?: 0.0),
                updatedZone
            )
            val showCard = !current.isOnline && !insideHome
            current.copy(
                homeZone = updatedZone,
                isInsideHomeZone = insideHome,
                showDetailedMapUnavailableCard = showCard
            )
        }
        localTileServer.reloadReaders()
    }

    private fun filterShelters(
        shelters: List<Shelter>,
        state: String?,
        type: ShelterType?
    ): List<Shelter> {
        return shelters.filter { s ->
            (state == null || s.state.equals(state, ignoreCase = true)) &&
            (type == null || s.shelterType == type)
        }
    }

    fun calculateNearest(
        userLocation: Pair<Double, Double>?,
        shelters: List<Shelter>
    ): Pair<Shelter, Double>? {
        return ShelterDistanceCalculator.findNearestShelter(userLocation, shelters)
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val shelterRepo = ShelterRepository(appContext)
                val assetMgr = MapAssetManager(appContext)
                val homeRepo = HomeZoneRepository.getInstance(appContext)
                val connObs = NetworkConnectivityObserver(appContext)
                val tileServer = LocalTileServer.getInstance(appContext)
                return MapViewModel(shelterRepo, assetMgr, homeRepo, connObs, tileServer) as T
            }
        }
    }
}
