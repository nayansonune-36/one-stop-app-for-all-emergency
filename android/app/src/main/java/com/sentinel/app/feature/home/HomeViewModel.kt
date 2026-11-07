package com.sentinel.app.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sentinel.app.R
import com.sentinel.app.core.network.ConnectivityObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val isOnline: Boolean = false,
    val userName: String? = null,
    val greetingRes: Int = R.string.greeting_afternoon,
    val holdProgress: Float = 0f,
    val isHolding: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val totalGuidesCount: Int = 10
)

class HomeViewModel(
    private val context: Context,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var holdJob: Job? = null

    init {
        // Observe connectivity
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
        refreshGreetingAndUser()
    }

    fun refreshGreetingAndUser() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }

        val prefs = context.getSharedPreferences("sentinel_medical_prefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("med_name", null)?.takeIf { it.isNotBlank() }

        _uiState.update {
            it.copy(
                greetingRes = greetingRes,
                userName = savedName
            )
        }
    }

    fun startHolding() {
        if (_uiState.value.isHolding) return
        _uiState.update { it.copy(isHolding = true, holdProgress = 0f) }
        holdJob?.cancel()
        holdJob = viewModelScope.launch {
            val totalSteps = 60
            val stepDelay = 3000L / totalSteps
            for (step in 1..totalSteps) {
                delay(stepDelay)
                val progress = step.toFloat() / totalSteps
                _uiState.update { it.copy(holdProgress = progress) }
            }
            // 3-second hold completed -> open confirm step
            _uiState.update { it.copy(isHolding = false, holdProgress = 1f, showConfirmDialog = true) }
        }
    }

    fun stopHolding() {
        holdJob?.cancel()
        holdJob = null
        _uiState.update { it.copy(isHolding = false, holdProgress = 0f) }
    }

    fun showConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false, holdProgress = 0f) }
    }

    companion object {
        fun provideFactory(context: Context, connectivityObserver: ConnectivityObserver): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(context.applicationContext, connectivityObserver) as T
                }
            }
    }
}
