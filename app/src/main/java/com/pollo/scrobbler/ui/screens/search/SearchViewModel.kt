package com.pollo.scrobbler.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pollo.scrobbler.domain.model.Release
import com.pollo.scrobbler.domain.usecase.SearchReleaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val results: List<Release> = emptyList(),
    val isLoading: Boolean = false,
    val query: String = "",
    val error: String? = null,
    val isBarcode: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchReleaseUseCase: SearchReleaseUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun search(query: String) {
        if (query == _state.value.query && _state.value.results.isNotEmpty()) return
        val isBarcode = query.matches(Regex("\\d{8,14}"))
        _state.value = SearchUiState(isLoading = true, query = query, isBarcode = isBarcode)
        viewModelScope.launch {
            try {
                val results = if (isBarcode) searchReleaseUseCase.byBarcode(query)
                              else searchReleaseUseCase.byQuery(query)
                _state.value = _state.value.copy(results = results, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}
