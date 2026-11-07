package com.sentinel.app.feature.medical

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val errorMessage: String? = null,
    val infoMessage: String? = null,
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
        _uiState.update { it.copy(isEditing = true, draftProfile = it.profile) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false, draftProfile = it.profile) }
    }

    fun updateDraft(updater: (MedicalProfile) -> MedicalProfile) {
        _uiState.update { it.copy(draftProfile = updater(it.draftProfile)) }
    }

    fun saveDraft(): Boolean {
        val draft = _uiState.value.draftProfile
        if (draft.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "NAME_REQUIRED") }
            return false
        }
        repository.saveProfile(draft)
        _uiState.update {
            it.copy(
                isEditing = false,
                errorMessage = null,
                infoMessage = "SAVED_SUCCESS"
            )
        }
        return true
    }

    fun importProfile(scanned: MedicalProfile) {
        val current = _uiState.value.profile
        val merged = current.copy(
            name = scanned.name.ifBlank { current.name },
            age = scanned.age.ifBlank { current.age },
            blood = scanned.blood.ifBlank { current.blood },
            allergies = scanned.allergies.ifBlank { current.allergies },
            conditions = scanned.conditions.ifBlank { current.conditions },
            medications = scanned.medications.ifBlank { current.medications },
            contactName = scanned.contactName.ifBlank { current.contactName },
            contactPhone = scanned.contactPhone.ifBlank { current.contactPhone },
            notes = scanned.notes.ifBlank { current.notes }
        )
        repository.saveProfile(merged)
        _uiState.update {
            it.copy(
                profile = merged,
                draftProfile = merged,
                infoMessage = "IMPORTED_SUCCESS"
            )
        }
    }

    fun updatePinInput(pin: String) {
        val digits = pin.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(pinInput = digits, errorMessage = null) }
    }

    fun unlockWithPin(): Boolean {
        val storedPin = repository.getPin()
        val entered = _uiState.value.pinInput
        // If no PIN set or PIN matches
        if (storedPin == null || entered == storedPin) {
            _uiState.update { it.copy(isUnlocked = true, pinInput = "", infoMessage = "UNLOCKED_SUCCESS") }
            return true
        } else {
            _uiState.update { it.copy(errorMessage = "INCORRECT_PIN") }
            return false
        }
    }

    fun unlockBiometric() {
        _uiState.update { it.copy(isUnlocked = true, infoMessage = "UNLOCKED_SUCCESS") }
    }

    fun toggleQrDialog(show: Boolean) {
        if (show) {
            val serialized = MedicalQrParser.serialize(_uiState.value.profile)
            viewModelScope.launch(Dispatchers.Default) {
                val bitmap = QrCodeGenerator.generateQrBitmap(serialized, 600)
                _uiState.update { it.copy(showQrDialog = true, qrBitmap = bitmap) }
            }
        } else {
            _uiState.update { it.copy(showQrDialog = false) }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
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
