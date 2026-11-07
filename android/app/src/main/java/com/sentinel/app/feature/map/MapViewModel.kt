package com.sentinel.app.feature.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sentinel.app.feature.map.data.MapAssetManager
import com.sentinel.app.feature.map.data.ShelterRepository
import com.sentinel.app.feature.map.model.Shelter
import com.sentinel.app.feature.map.model.ShelterType
import com.sentinel.app.feature.map.util.GeoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class MapUiState(
    val allShelters: List<Shelter> = emptyList(),
    val filteredShelters: List<Shelter> = emptyList(),
    val selectedTypeFilter: ShelterType? = null,
    val selectedShelter: Shelter? = null,
    val userLocation: Pair<Double, Double>? = null,
    val nearestShelter: Shelter? = null,
    val nearestDistanceKm: Double? = null,
    val hasLocationPermission: Boolean = false,
    val isEmptyRegistry: Boolean = false,
    val mapStyleUrl: String = MapAssetManager.OFFLINE_STYLE_ASSET
)

class MapViewModel(
    private val shelterRepository: ShelterRepository,
    private val mapAssetManager: MapAssetManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        val styleUrl = mapAssetManager.prepareOfflineStorage()
        val loadedShelters = shelterRepository.loadShelters()
        val isEmpty = loadedShelters.isEmpty()

        _uiState.update { current ->
            current.copy(
                allShelters = loadedShelters,
                filteredShelters = loadedShelters,
                isEmptyRegistry = isEmpty,
                mapStyleUrl = styleUrl
            )
        }
    }

    fun onFilterSelected(type: ShelterType?) {
        _uiState.update { current ->
            val filtered = if (type == null) {
                current.allShelters
            } else {
                current.allShelters.filter { it.shelterType == type }
            }
            val nearest = calculateNearest(current.userLocation, filtered)
            current.copy(
                selectedTypeFilter = type,
                filteredShelters = filtered,
                nearestShelter = nearest?.first,
                nearestDistanceKm = nearest?.second
            )
        }
    }

    fun onShelterSelected(shelter: Shelter?) {
        _uiState.update { it.copy(selectedShelter = shelter) }
    }

    fun onLocationUpdated(lat: Double, lon: Double) {
        val location = Pair(lat, lon)
        _uiState.update { current ->
            val nearest = calculateNearest(location, current.filteredShelters)
            current.copy(
                userLocation = location,
                nearestShelter = nearest?.first,
                nearestDistanceKm = nearest?.second
            )
        }
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
    }

    private fun calculateNearest(
        userLocation: Pair<Double, Double>?,
        shelters: List<Shelter>
    ): Pair<Shelter, Double>? {
        if (userLocation == null || shelters.isEmpty()) return null

        val (uLat, uLon) = userLocation
        var nearest: Shelter? = null
        var minDistance = Double.MAX_VALUE

        for (shelter in shelters) {
            val dist = GeoUtils.calculateDistanceKm(uLat, uLon, shelter.lat, shelter.lon)
            if (dist < minDistance) {
                minDistance = dist
                nearest = shelter
            }
        }

        return if (nearest != null) Pair(nearest, minDistance) else null
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val shelterRepo = ShelterRepository(appContext)
                val assetMgr = MapAssetManager(appContext)
                return MapViewModel(shelterRepo, assetMgr) as T
            }
        }
    }
}
