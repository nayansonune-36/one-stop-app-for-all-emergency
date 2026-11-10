package com.sentinel.app.feature.medical

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sentinel.app.R
import com.sentinel.app.feature.medical.data.MedicalRepository
import com.sentinel.app.feature.medical.model.MedicalProfile
import com.sentinel.app.feature.medical.util.MedicalQrParser
import com.sentinel.app.feature.medical.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MedicalUiState(
    val profile: MedicalProfile = MedicalProfile(),
    val draftProfile: MedicalProfile = MedicalProfile(),
    val isEditing: Boolean = false,
    val isLocked: Boolean = false,
    val isUnlocked: Boolean = true,
    val pinInput: String = "",
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val ageErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null,
    @StringRes val errorMessageRes: Int? = null,
    @StringRes val infoMessageRes: Int? = null,
    val showQrDialog: Boolean = false,
    val qrBitmap: Bitmap? = null
)

class MedicalViewModel(
    private val repository: MedicalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MedicalUiState(
            profile = repository.getProfile(),
            draftProfile = repository.getProfile(),
            isLocked = repository.isLocked.value,
            isUnlocked = !repository.isLocked.value
        )
    )
    val uiState: StateFlow<MedicalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.profile.collect { p ->
                _uiState.update { current ->
                    if (!current.isEditing) {
                        current.copy(profile = p, draftProfile = p)
                    } else {
                        current.copy(profile = p)
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.isLocked.collect { locked ->
                _uiState.update { current ->
                    current.copy(
                        isLocked = locked,
                        isUnlocked = if (!locked) true else current.isUnlocked
                    )
                }
            }
        }
    }

    fun startEditing() {
        _uiState.update {
            it.copy(
                isEditing = true,
                draftProfile = it.profile,
                nameErrorRes = null,
                ageErrorRes = null,
                phoneErrorRes = null,
                errorMessageRes = null
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                isEditing = false,
                draftProfile = it.profile,
                nameErrorRes = null,
                ageErrorRes = null,
                phoneErrorRes = null,
                errorMessageRes = null
            )
        }
    }

    fun updateDraft(updater: (MedicalProfile) -> MedicalProfile) {
        _uiState.update { current ->
            current.copy(
                draftProfile = updater(current.draftProfile),
                nameErrorRes = null,
                ageErrorRes = null,
                phoneErrorRes = null,
                errorMessageRes = null
            )
        }
    }

    fun saveDraft(): Boolean {
        val draft = _uiState.value.draftProfile

        // 1. Validate Name
        if (draft.name.trim().isBlank()) {
            _uiState.update {
                it.copy(
                    nameErrorRes = R.string.medical_val_name_required,
                    errorMessageRes = R.string.medical_name_required
                )
            }
            return false
        }

        // 2. Validate Age if entered
        val ageStr = draft.age.trim()
        if (ageStr.isNotBlank()) {
            val ageInt = ageStr.toIntOrNull()
            if (ageInt == null || ageInt !in 0..130) {
                _uiState.update {
                    it.copy(
                        ageErrorRes = R.string.medical_val_invalid_age,
                        errorMessageRes = R.string.medical_val_invalid_age
                    )
                }
                return false
            }
        }

        // 3. Validate Emergency Contact Phone if entered
        val phoneStr = draft.emergencyContactPhone.trim()
        if (phoneStr.isNotBlank()) {
            val cleanPhone = phoneStr.filter { it.isDigit() || it == '+' || it == '-' || it == ' ' }
            if (cleanPhone.length < 5 || phoneStr.any { it.isLetter() }) {
                _uiState.update {
                    it.copy(
                        phoneErrorRes = R.string.medical_val_invalid_phone,
                        errorMessageRes = R.string.medical_val_invalid_phone
                    )
                }
                return false
            }
        }

        repository.saveProfile(draft)
        _uiState.update {
            it.copy(
                isEditing = false,
                nameErrorRes = null,
                ageErrorRes = null,
                phoneErrorRes = null,
                errorMessageRes = null,
                infoMessageRes = R.string.medical_saved_success
            )
        }
        return true
    }

    fun importProfile(scanned: MedicalProfile) {
        val current = _uiState.value.profile
        val merged = current.copy(
            name = scanned.name.ifBlank { current.name },
            age = scanned.age.ifBlank { current.age },
            bloodGroup = scanned.bloodGroup.ifBlank { current.bloodGroup },
            allergies = scanned.allergies.ifBlank { current.allergies },
            conditions = scanned.conditions.ifBlank { current.conditions },
            medications = scanned.medications.ifBlank { current.medications },
            emergencyContactName = scanned.emergencyContactName.ifBlank { current.emergencyContactName },
            emergencyContactPhone = scanned.emergencyContactPhone.ifBlank { current.emergencyContactPhone }
        )
        repository.saveProfile(merged)
        _uiState.update {
            it.copy(
                profile = merged,
                draftProfile = merged,
                infoMessageRes = R.string.medical_imported_success
            )
        }
    }

    fun updatePinInput(pin: String) {
        val digits = pin.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(pinInput = digits, errorMessageRes = null) }
    }

    fun unlockWithPin(): Boolean {
        val storedPin = repository.getPin()
        val entered = _uiState.value.pinInput
        if (storedPin == null || entered == storedPin) {
            _uiState.update {
                it.copy(
                    isUnlocked = true,
                    pinInput = "",
                    infoMessageRes = R.string.medical_unlocked_success
                )
            }
            return true
        } else {
            _uiState.update { it.copy(errorMessageRes = R.string.medical_lock_incorrect_pin) }
            return false
        }
    }

    fun unlockBiometric() {
        _uiState.update {
            it.copy(
                isUnlocked = true,
                pinInput = "",
                infoMessageRes = R.string.medical_unlocked_success
            )
        }
    }

    fun lockProfile() {
        if (_uiState.value.isLocked) {
            _uiState.update { it.copy(isUnlocked = false) }
        }
    }

    fun toggleQrDialog(show: Boolean) {
        if (show) {
            val prof = _uiState.value.profile
            val isProfileEmpty = prof.name.isBlank() &&
                    prof.bloodGroup.isBlank() &&
                    prof.allergies.isBlank() &&
                    prof.conditions.isBlank() &&
                    prof.medications.isBlank() &&
                    prof.emergencyContactName.isBlank() &&
                    prof.emergencyContactPhone.isBlank()

            if (isProfileEmpty) {
                _uiState.update {
                    it.copy(errorMessageRes = R.string.medical_qr_profile_empty_error)
                }
                return
            }

            val serialized = MedicalQrParser.serialize(prof)
            viewModelScope.launch(Dispatchers.Default) {
                val bitmap = QrCodeGenerator.generateQrBitmap(serialized, 600)
                _uiState.update { it.copy(showQrDialog = true, qrBitmap = bitmap) }
            }
        } else {
            _uiState.update { it.copy(showQrDialog = false) }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessageRes = null, infoMessageRes = null) }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repo = MedicalRepository(context.applicationContext)
                    return MedicalViewModel(repo) as T
                }
            }
    }
}
