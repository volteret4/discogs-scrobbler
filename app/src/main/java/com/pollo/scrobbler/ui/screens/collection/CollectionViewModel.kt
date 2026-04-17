package com.pollo.scrobbler.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pollo.scrobbler.domain.model.CollectionItem
import com.pollo.scrobbler.domain.usecase.GetCollectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val items: List<CollectionItem> = emptyList(),
    val filteredItems: List<CollectionItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isCompletingInfo: Boolean = false,
    val completarInfoProgress: String? = null,
    val query: String = "",
    val error: String? = null
)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val getCollectionUseCase: GetCollectionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionUiState())
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    init {
        observeCollection()
    }

    private fun observeCollection() {
        viewModelScope.launch {
            getCollectionUseCase()
                .catch { e -> _state.value = _state.value.copy(error = e.message) }
                .collect { items ->
                    val filtered = filterItems(items, _state.value.query)
                    _state.value = _state.value.copy(items = items, filteredItems = filtered)
                }
        }
    }

    fun syncCollection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true, error = null)
            try {
                getCollectionUseCase.sync()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Sync failed: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isSyncing = false)
            }
        }
    }

    fun completarInfo() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCompletingInfo = true, completarInfoProgress = null, error = null)
            try {
                getCollectionUseCase.completarInfo { progress ->
                    _state.value = _state.value.copy(completarInfoProgress = progress)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isCompletingInfo = false, completarInfoProgress = null)
            }
        }
    }

    fun onSearch(query: String) {
        val filtered = filterItems(_state.value.items, query)
        _state.value = _state.value.copy(query = query, filteredItems = filtered)
    }

    private fun filterItems(items: List<CollectionItem>, query: String): List<CollectionItem> {
        if (query.isBlank()) return items
        val q = query.lowercase()
        return items.filter { item ->
            item.release.title.lowercase().contains(q) ||
            item.release.artist.lowercase().contains(q)
        }
    }
}
