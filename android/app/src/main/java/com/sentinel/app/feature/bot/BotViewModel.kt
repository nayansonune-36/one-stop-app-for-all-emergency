package com.sentinel.app.feature.bot

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sentinel.app.R
import com.sentinel.app.core.network.ConnectivityObserver
import com.sentinel.app.feature.bot.data.BotRepository
import com.sentinel.app.feature.bot.model.ChatMessage
import com.sentinel.app.feature.bot.model.ChatMessageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class BotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isOnline: Boolean = true,
    val isLoading: Boolean = false,
    val isCritical: Boolean = false,
    val inputText: String = "",
    val suggestedChips: List<String> = emptyList()
)

class BotViewModel(
    private val connectivityObserver: ConnectivityObserver,
    private val repository: BotRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BotUiState())
    val uiState: StateFlow<BotUiState> = _uiState.asStateFlow()

    init {
        val initialGreeting = context.getString(R.string.bot_initial_greeting)
        val chips = listOf(
            context.getString(R.string.bot_suggested_1),
            context.getString(R.string.bot_suggested_2),
            context.getString(R.string.bot_suggested_3),
            context.getString(R.string.bot_suggested_4)
        )

        _uiState.update {
            it.copy(
                messages = listOf(ChatMessage(role = "assistant", content = initialGreeting)),
                suggestedChips = chips
            )
        }

        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(text: String? = null) {
        val content = (text ?: _uiState.value.inputText).trim()
        if (content.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(role = "user", content = content)
        val updatedMessages = _uiState.value.messages + userMessage

        _uiState.update {
            it.copy(
                messages = updatedMessages,
                inputText = "",
                isLoading = true
            )
        }

        val isHindi = context.resources.configuration.locales[0]?.language == "hi"
        val lang = if (isHindi) "hi" else "en"

        val historyItems = updatedMessages.takeLast(10).map {
            ChatMessageItem(role = it.role, content = it.content)
        }

        viewModelScope.launch {
            val result = repository.sendMessage(lang, historyItems)
            result.onSuccess { response ->
                val assistantMessage = ChatMessage(
                    role = "assistant",
                    content = response.reply,
                    isCritical = response.critical
                )
                _uiState.update { current ->
                    current.copy(
                        messages = current.messages + assistantMessage,
                        isLoading = false,
                        isCritical = response.critical || current.isCritical
                    )
                }
            }.onFailure { error ->
                val errorMsg = if (lang == "hi") {
                    "सर्वर से जुड़ने में त्रुटि हुई। कृपया पुनः प्रयास करें।"
                } else {
                    "Connection error to triage service. Please try again."
                }
                val assistantMessage = ChatMessage(
                    role = "assistant",
                    content = errorMsg,
                    isCritical = false
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun dismissCriticalBanner() {
        _uiState.update { it.copy(isCritical = false) }
    }

    companion object {
        fun provideFactory(
            context: Context,
            connectivityObserver: ConnectivityObserver
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val repo = BotRepository()
                return BotViewModel(connectivityObserver, repo, appContext) as T
            }
        }
    }
}
