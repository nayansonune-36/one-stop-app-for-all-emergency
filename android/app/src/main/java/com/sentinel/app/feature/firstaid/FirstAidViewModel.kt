package com.sentinel.app.feature.firstaid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sentinel.app.feature.firstaid.data.FirstAidRepository
import com.sentinel.app.feature.firstaid.model.FirstAidGuide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class FirstAidUiState(
    val guides: List<FirstAidGuide> = emptyList(),
    val filteredGuides: List<FirstAidGuide> = emptyList(),
    val searchQuery: String = "",
    val selectedGuide: FirstAidGuide? = null,
    val totalCount: Int = 0
)

class FirstAidViewModel(
    private val repository: FirstAidRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirstAidUiState())
    val uiState: StateFlow<FirstAidUiState> = _uiState.asStateFlow()

    init {
        val isHindi = context.resources.configuration.locales[0]?.language == "hi"
        loadGuides(isHindi)
    }

    fun loadGuides(isHindi: Boolean) {
        val loaded = repository.loadGuides(isHindi)
        _uiState.update { current ->
            val filtered = filterGuides(loaded, current.searchQuery)
            current.copy(
                guides = loaded,
                filteredGuides = filtered,
                totalCount = loaded.size,
                selectedGuide = current.selectedGuide?.let { sel -> loaded.firstOrNull { it.id == sel.id } }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredGuides = filterGuides(current.guides, query)
            )
        }
    }

    fun onClearSearch() {
        onSearchQueryChanged("")
    }

    fun onSelectGuide(guide: FirstAidGuide?) {
        _uiState.update { it.copy(selectedGuide = guide) }
    }

    fun onSelectGuideById(id: String) {
        val guide = _uiState.value.guides.firstOrNull { it.id == id }
        _uiState.update { it.copy(selectedGuide = guide) }
    }

    private fun filterGuides(list: List<FirstAidGuide>, query: String): List<FirstAidGuide> {
        if (query.isBlank()) return list
        val lower = query.trim().lowercase(Locale.ROOT)
        return list.filter {
            it.title.lowercase(Locale.ROOT).contains(lower) ||
                    it.summary.lowercase(Locale.ROOT).contains(lower) ||
                    it.subtitle.lowercase(Locale.ROOT).contains(lower) ||
                    it.steps.any { step -> step.lowercase(Locale.ROOT).contains(lower) }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val repo = FirstAidRepository(appContext)
                    return FirstAidViewModel(repo, appContext) as T
                }
            }
    }
}
