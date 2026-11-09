package com.sentinel.app.feature.medical

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sentinel.app.R
import com.sentinel.app.feature.medical.model.MedicalProfile
import com.sentinel.app.feature.medical.util.MedicalQrParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QrScanUiState(
    val hasCameraPermission: Boolean = false,
    val permissionRequested: Boolean = false,
    val scannedProfile: MedicalProfile? = null,
    val isTorchOn: Boolean = false,
    @StringRes val scanErrorRes: Int? = null,
    val isScanning: Boolean = true
)

class QrScanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QrScanUiState())
    val uiState: StateFlow<QrScanUiState> = _uiState.asStateFlow()

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update {
            it.copy(
                hasCameraPermission = isGranted,
                permissionRequested = true
            )
        }
    }

    fun onBarcodeRawDetected(raw: String) {
        if (!_uiState.value.isScanning || _uiState.value.scannedProfile != null) {
            return
        }

        val parsed = MedicalQrParser.parse(raw)
        if (parsed != null) {
            _uiState.update {
                it.copy(
                    scannedProfile = parsed,
                    isScanning = false,
                    scanErrorRes = null
                )
            }
        } else {
            _uiState.update {
                it.copy(scanErrorRes = R.string.qr_invalid_code)
            }
        }
    }

    fun toggleTorch(): Boolean {
        val next = !_uiState.value.isTorchOn
        _uiState.update { it.copy(isTorchOn = next) }
        return next
    }

    fun resetScan() {
        _uiState.update {
            it.copy(
                scannedProfile = null,
                scanErrorRes = null,
                isScanning = true
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(scanErrorRes = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QrScanViewModel() as T
            }
        }
    }
}
