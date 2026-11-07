package com.sentinel.app.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
    val isOnboardingComplete: Boolean = false
)

class OnboardingViewModel(
    private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            isOnboardingComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
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
    }

    fun onPermissionResult(group: PermissionGroup, isGranted: Boolean) {
        val status = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
        _uiState.update { current ->
            when (group) {
                PermissionGroup.LOCATION -> current.copy(locationStatus = status)
                PermissionGroup.CAMERA -> current.copy(cameraStatus = status)
                PermissionGroup.MICROPHONE -> current.copy(micStatus = status)
                PermissionGroup.NOTIFICATIONS -> current.copy(notificationsStatus = status)
                PermissionGroup.BLUETOOTH -> current.copy(bluetoothStatus = status)
            }
        }
    }

    fun completeOnboarding() {
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
