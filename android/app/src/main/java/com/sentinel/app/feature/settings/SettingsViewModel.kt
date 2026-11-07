package com.sentinel.app.feature.settings

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sentinel.app.core.mesh.MeshPermissionHelper
import com.sentinel.app.feature.medical.data.MedicalRepository
import com.sentinel.app.feature.onboarding.PermissionGroup
import com.sentinel.app.feature.onboarding.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val currentLanguage: String = "en",
    val userName: String? = null,
    val notificationsEnabled: Boolean = true,
    val isMedicalLocked: Boolean = false,
    val hasPinSet: Boolean = false,
    val isPinDialogOpen: Boolean = false,
    val pinInput: String = "",
    val locationStatus: PermissionStatus = PermissionStatus.PROMPT,
    val cameraStatus: PermissionStatus = PermissionStatus.PROMPT,
    val micStatus: PermissionStatus = PermissionStatus.PROMPT,
    val notificationsStatus: PermissionStatus = PermissionStatus.PROMPT,
    val bluetoothStatus: PermissionStatus = PermissionStatus.PROMPT,
    val toastMessage: String? = null
)

class SettingsViewModel(
    private val context: Context,
    private val medicalRepository: MedicalRepository
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            currentLanguage = resolveCurrentLanguage(),
            userName = medicalRepository.getProfile().name.takeIf { it.isNotBlank() },
            notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
            isMedicalLocked = medicalRepository.isLocked.value,
            hasPinSet = medicalRepository.getPin() != null
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
        refreshProfile()
    }

    fun refreshProfile() {
        val name = medicalRepository.getProfile().name.takeIf { it.isNotBlank() }
        val locked = medicalRepository.isLocked.value
        val hasPin = medicalRepository.getPin() != null
        _uiState.update {
            it.copy(
                userName = name,
                isMedicalLocked = locked,
                hasPinSet = hasPin
            )
        }
    }

    fun switchLanguage(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        _uiState.update { it.copy(currentLanguage = languageCode, toastMessage = "LANGUAGE_SAVED") }
    }

    fun toggleNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun toggleMedicalLock(locked: Boolean) {
        medicalRepository.setLocked(locked)
        _uiState.update { it.copy(isMedicalLocked = locked) }
    }

    fun openPinDialog() {
        _uiState.update { it.copy(isPinDialogOpen = true, pinInput = "") }
    }

    fun closePinDialog() {
        _uiState.update { it.copy(isPinDialogOpen = false, pinInput = "") }
    }

    fun updatePinInput(pin: String) {
        val digits = pin.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(pinInput = digits) }
    }

    fun savePin(): Boolean {
        val pin = _uiState.value.pinInput
        if (pin.length == 4) {
            medicalRepository.setPin(pin)
            _uiState.update {
                it.copy(
                    hasPinSet = true,
                    isPinDialogOpen = false,
                    pinInput = "",
                    toastMessage = "PIN_SAVED"
                )
            }
            return true
        }
        return false
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

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun resolveCurrentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            val tag = locales.get(0)?.language ?: "en"
            if (tag.startsWith("hi")) "hi" else "en"
        } else {
            "en"
        }
    }

    companion object {
        private const val PREFS_NAME = "sentinel_settings_prefs"
        private const val KEY_NOTIFICATIONS = "safety_notifications"

        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appCtx = context.applicationContext
                    val medicalRepo = MedicalRepository(appCtx)
                    return SettingsViewModel(appCtx, medicalRepo) as T
                }
            }
    }
}
