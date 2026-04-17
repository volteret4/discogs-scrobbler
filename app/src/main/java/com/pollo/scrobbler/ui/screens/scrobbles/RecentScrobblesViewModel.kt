package com.pollo.scrobbler.ui.screens.scrobbles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pollo.scrobbler.domain.model.RecentScrobble
import com.pollo.scrobbler.domain.repository.LastFmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecentScrobblesUiState(
    val scrobbles: List<RecentScrobble> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecentScrobblesViewModel @Inject constructor(
    private val lastFmRepository: LastFmRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecentScrobblesUiState())
    val state: StateFlow<RecentScrobblesUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val scrobbles = lastFmRepository.getRecentTracks(limit = 200)
                _state.value = _state.value.copy(
                    scrobbles = scrobbles.filter { !it.isNowPlaying }.take(100),
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}
