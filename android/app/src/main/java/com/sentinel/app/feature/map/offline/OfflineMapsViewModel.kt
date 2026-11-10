package com.sentinel.app.feature.map.offline

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sentinel.app.core.maps.BundledIndianCities
import com.sentinel.app.core.maps.HomePackStatus
import com.sentinel.app.core.maps.HomeZone
import com.sentinel.app.core.maps.HomeZoneRepository
import com.sentinel.app.core.maps.IndianCity
import com.sentinel.app.core.maps.worker.HomePackDownloadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OfflineMapsUiState(
    val homeZone: HomeZone? = null,
    val homePackStatus: HomePackStatus = HomePackStatus.NotConfigured,
    val homePackSizeMb: Double = 0.0,
    val availableStorageMb: Double = 0.0,
    val allowMobileData: Boolean = false,
    val isChangingZone: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<IndianCity> = emptyList(),
    val customRadiusKm: Double = HomeZone.DEFAULT_RADIUS_KM
)

class OfflineMapsViewModel(
    private val repository: HomeZoneRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineMapsUiState())
    val uiState: StateFlow<OfflineMapsUiState> = _uiState.asStateFlow()

    init {
        refreshState()

        viewModelScope.launch {
            repository.homePackStatus.collect { status ->
                _uiState.update {
                    it.copy(
                        homePackStatus = status,
                        homePackSizeMb = repository.getHomePackSizeMb(),
                        availableStorageMb = repository.getAvailableStorageMb()
                    )
                }
            }
        }
    }

    fun refreshState() {
        val zone = repository.getHomeZone() ?: HomeZone.DEMO_GUWAHATI
        _uiState.update {
            it.copy(
                homeZone = zone,
                homePackSizeMb = repository.getHomePackSizeMb(),
                availableStorageMb = repository.getAvailableStorageMb(),
                allowMobileData = repository.isAllowMobileData(),
                customRadiusKm = zone.radiusKm,
                searchResults = BundledIndianCities.CITIES
            )
        }
    }

    fun setAllowMobileData(allow: Boolean) {
        repository.setAllowMobileData(allow)
        _uiState.update { it.copy(allowMobileData = allow) }
    }

    fun redownloadHomePack() {
        val zone = _uiState.value.homeZone ?: return
        HomePackDownloadWorker.enqueue(context, zone, _uiState.value.allowMobileData)
    }

    fun deleteHomePack() {
        repository.deleteHomePack()
        refreshState()
    }

    fun openChangeZone() {
        _uiState.update {
            it.copy(
                isChangingZone = true,
                searchQuery = "",
                searchResults = BundledIndianCities.CITIES
            )
        }
    }

    fun closeChangeZone() {
        _uiState.update { it.copy(isChangingZone = false) }
    }

    fun onSearchQueryChanged(query: String) {
        val results = BundledIndianCities.search(query)
        _uiState.update { it.copy(searchQuery = query, searchResults = results) }
    }

    fun onRadiusChanged(radiusKm: Double) {
        _uiState.update { it.copy(customRadiusKm = radiusKm.coerceIn(HomeZone.MIN_RADIUS_KM, HomeZone.MAX_RADIUS_KM)) }
    }

    fun selectCity(city: IndianCity) {
        val newZone = HomeZone.fromCenterAndRadius(
            id = "home_${city.name.lowercase().replace(" ", "_")}",
            name = city.name,
            centerLat = city.lat,
            centerLon = city.lon,
            radiusKm = _uiState.value.customRadiusKm
        )
        repository.saveHomeZone(newZone)
        _uiState.update {
            it.copy(
                homeZone = newZone,
                isChangingZone = false
            )
        }
        // Enqueue download for newly selected home area
        HomePackDownloadWorker.enqueue(context, newZone, _uiState.value.allowMobileData)
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val repo = HomeZoneRepository.getInstance(appContext)
                return OfflineMapsViewModel(repo, appContext) as T
            }
        }
    }
}
