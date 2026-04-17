package com.pollo.scrobbler.ui.screens.busqueda

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

data class BusquedaUiState(
    val query: String = "",
    val results: List<Release> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val scanMode: Boolean = false,
    val hasCameraPermission: Boolean = false
)

@HiltViewModel
class BusquedaViewModel @Inject constructor(
    private val searchReleaseUseCase: SearchReleaseUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BusquedaUiState())
    val state: StateFlow<BusquedaUiState> = _state.asStateFlow()

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun search() {
        val q = _state.value.query.trim()
        if (q.isBlank()) return
        val isBarcode = q.matches(Regex("\\d{8,14}"))
        _state.value = _state.value.copy(isLoading = true, error = null, scanMode = false)
        viewModelScope.launch {
            try {
                val results = if (isBarcode) searchReleaseUseCase.byBarcode(q)
                              else searchReleaseUseCase.byQuery(q)
                _state.value = _state.value.copy(results = results, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun onBarcodeDetected(barcode: String) {
        _state.value = _state.value.copy(query = barcode, scanMode = false)
        search()
    }

    fun toggleScanMode() {
        _state.value = _state.value.copy(scanMode = !_state.value.scanMode)
    }

    fun onCameraPermissionGranted() {
        _state.value = _state.value.copy(hasCameraPermission = true)
    }

    fun clearResults() {
        _state.value = BusquedaUiState()
    }
}
