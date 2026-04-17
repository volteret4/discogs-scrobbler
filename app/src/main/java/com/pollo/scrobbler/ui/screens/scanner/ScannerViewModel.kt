package com.pollo.scrobbler.ui.screens.scanner

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ScannerUiState(
    val isScanning: Boolean = true,
    val lastScannedBarcode: String? = null,
    val hasCameraPermission: Boolean = false
)

@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    fun onCameraPermissionGranted() {
        _state.value = _state.value.copy(hasCameraPermission = true)
    }

    fun onBarcodeDetected(barcode: String) {
        if (barcode == _state.value.lastScannedBarcode) return
        _state.value = _state.value.copy(isScanning = false, lastScannedBarcode = barcode)
    }

    fun resetScanner() {
        _state.value = _state.value.copy(isScanning = true, lastScannedBarcode = null)
    }
}
