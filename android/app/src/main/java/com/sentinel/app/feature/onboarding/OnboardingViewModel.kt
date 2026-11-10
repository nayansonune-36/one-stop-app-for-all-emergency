package com.sentinel.app.feature.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sentinel.app.core.maps.BundledIndianCities
import com.sentinel.app.core.maps.HomeZone
import com.sentinel.app.core.maps.HomeZoneRepository
import com.sentinel.app.core.maps.IndianCity
import com.sentinel.app.core.maps.worker.HomePackDownloadWorker
import com.sentinel.app.core.mesh.MeshPermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PermissionStatus {
    GRANTED,
    DENIED,
    PROMPT
}

enum class PermissionGroup {
    LOCATION,
    CAMERA,
    MICROPHONE,
    NOTIFICATIONS,
    BLUETOOTH
}

data class OnboardingUiState(
    val locationStatus: PermissionStatus = PermissionStatus.PROMPT,
    val cameraStatus: PermissionStatus = PermissionStatus.PROMPT,
    val micStatus: PermissionStatus = PermissionStatus.PROMPT,
    val notificationsStatus: PermissionStatus = PermissionStatus.PROMPT,
    val bluetoothStatus: PermissionStatus = PermissionStatus.PROMPT,
    val isOnboardingComplete: Boolean = false,
    val homeZone: HomeZone = HomeZone.DEMO_GUWAHATI,
    val searchCityQuery: String = "",
    val citySearchResults: List<IndianCity> = BundledIndianCities.CITIES,
    val selectedRadiusKm: Double = HomeZone.DEFAULT_RADIUS_KM
)

class OnboardingViewModel(
    private val context: Context,
    private val homeZoneRepo: HomeZoneRepository = HomeZoneRepository.getInstance(context)
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            isOnboardingComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false),
            homeZone = homeZoneRepo.getHomeZone() ?: HomeZone.DEMO_GUWAHATI,
            citySearchResults = BundledIndianCities.CITIES
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
        tryPrefillFromGps()
    }

    fun refreshPermissions() {
        val appCtx = context.applicationContext

        val locGranted = ContextCompat.checkSelfPermission(
            appCtx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                appCtx,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val camGranted = ContextCompat.checkSelfPermission(
            appCtx,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val micGranted = ContextCompat.checkSelfPermission(
            appCtx,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appCtx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val btGranted = MeshPermissionHelper.hasAllPermissions(appCtx)

        _uiState.update { current ->
            current.copy(
                locationStatus = if (locGranted) PermissionStatus.GRANTED else current.locationStatus,
                cameraStatus = if (camGranted) PermissionStatus.GRANTED else current.cameraStatus,
                micStatus = if (micGranted) PermissionStatus.GRANTED else current.micStatus,
                notificationsStatus = if (notifGranted) PermissionStatus.GRANTED else current.notificationsStatus,
                bluetoothStatus = if (btGranted) PermissionStatus.GRANTED else current.bluetoothStatus
            )
        }

        if (locGranted) {
            tryPrefillFromGps()
        }
    }

    @SuppressLint("MissingPermission")
    fun tryPrefillFromGps() {
        val appCtx = context.applicationContext
        val hasLoc = ContextCompat.checkSelfPermission(
            appCtx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            appCtx,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLoc) return

        try {
            val fused = LocationServices.getFusedLocationProviderClient(appCtx)
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        applyCoordinatesAsHome(loc.latitude, loc.longitude)
                    }
                }
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    applyCoordinatesAsHome(loc.latitude, loc.longitude)
                }
            }
        } catch (_: Exception) {}
    }

    private fun applyCoordinatesAsHome(lat: Double, lon: Double) {
        val nearest = BundledIndianCities.findNearest(lat, lon)
        val zone = HomeZone.fromCenterAndRadius(
            id = "home_${nearest.name.lowercase().replace(" ", "_")}",
            name = nearest.name,
            centerLat = lat,
            centerLon = lon,
            radiusKm = _uiState.value.selectedRadiusKm
        )
        _uiState.update { it.copy(homeZone = zone) }
    }

    fun onCitySearchChanged(query: String) {
        val results = BundledIndianCities.search(query)
        _uiState.update { it.copy(searchCityQuery = query, citySearchResults = results) }
    }

    fun onCitySelected(city: IndianCity) {
        val zone = HomeZone.fromCenterAndRadius(
            id = "home_${city.name.lowercase().replace(" ", "_")}",
            name = city.name,
            centerLat = city.lat,
            centerLon = city.lon,
            radiusKm = _uiState.value.selectedRadiusKm
        )
        _uiState.update { it.copy(homeZone = zone, searchCityQuery = "") }
    }

    fun onRadiusChanged(radiusKm: Double) {
        val clamped = radiusKm.coerceIn(HomeZone.MIN_RADIUS_KM, HomeZone.MAX_RADIUS_KM)
        val current = _uiState.value.homeZone
        val updated = HomeZone.fromCenterAndRadius(
            id = current.id,
            name = current.name,
            centerLat = current.centerLat,
            centerLon = current.centerLon,
            radiusKm = clamped
        )
        _uiState.update { it.copy(selectedRadiusKm = clamped, homeZone = updated) }
    }

    fun onPermissionResult(group: PermissionGroup, isGranted: Boolean) {
        val status = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
        _uiState.update { current ->
            when (group) {
                PermissionGroup.LOCATION -> {
                    if (isGranted) tryPrefillFromGps()
                    current.copy(locationStatus = status)
                }
                PermissionGroup.CAMERA -> current.copy(cameraStatus = status)
                PermissionGroup.MICROPHONE -> current.copy(micStatus = status)
                PermissionGroup.NOTIFICATIONS -> current.copy(notificationsStatus = status)
                PermissionGroup.BLUETOOTH -> current.copy(bluetoothStatus = status)
            }
        }
    }

    fun completeOnboarding() {
        val zone = _uiState.value.homeZone
        homeZoneRepo.saveHomeZone(zone)
        // Enqueue background home pack download
        HomePackDownloadWorker.enqueue(context, zone, allowMobileData = false)

        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
        _uiState.update { it.copy(isOnboardingComplete = true) }
    }

    companion object {
        private const val PREFS_NAME = "sentinel_prefs"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(context.applicationContext) as T
                }
            }
    }
}
