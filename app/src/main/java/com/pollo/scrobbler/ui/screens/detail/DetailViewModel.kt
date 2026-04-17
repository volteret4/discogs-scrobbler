package com.pollo.scrobbler.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pollo.scrobbler.data.local.AppPreferences
import com.pollo.scrobbler.domain.model.Release
import com.pollo.scrobbler.domain.repository.DiscogsRepository
import com.pollo.scrobbler.domain.usecase.ScrobbleAlbumUseCase
import com.pollo.scrobbler.domain.usecase.SearchReleaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val release: Release? = null,
    val isLoading: Boolean = false,
    val isScrobbling: Boolean = false,
    val scrobbleResult: String? = null,
    val isInCollection: Boolean = false,
    val isAddingToCollection: Boolean = false,
    val error: String? = null,
    val showTechInfo: Boolean = false,
    val showAliasDialog: Boolean = false,
    val isSavingAlias: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val searchReleaseUseCase: SearchReleaseUseCase,
    private val scrobbleAlbumUseCase: ScrobbleAlbumUseCase,
    private val discogsRepository: DiscogsRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.showTechInfo.collect { value ->
                _state.value = _state.value.copy(showTechInfo = value)
            }
        }
    }

    fun loadRelease(releaseId: Int) {
        if (_state.value.release?.id == releaseId && !_state.value.isLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, release = null)
            try {
                val release = searchReleaseUseCase.getDetail(releaseId)
                val inCollection = discogsRepository.isInCollection(releaseId)
                _state.value = _state.value.copy(release = release, isInCollection = inCollection, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun scrobble() {
        val release = _state.value.release ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isScrobbling = true, scrobbleResult = null)
            val result = scrobbleAlbumUseCase(release)
            _state.value = _state.value.copy(
                isScrobbling = false,
                scrobbleResult = if (result.isSuccess) "Scrobbled ${release.tracklist.size} tracks!"
                                 else result.exceptionOrNull()?.message ?: "Scrobble failed"
            )
        }
    }

    fun addToCollection() {
        val releaseId = _state.value.release?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isAddingToCollection = true, scrobbleResult = null)
            val success = discogsRepository.addToCollection(releaseId)
            _state.value = _state.value.copy(
                isAddingToCollection = false,
                isInCollection = success || _state.value.isInCollection,
                scrobbleResult = if (success) "Added to collection!" else "Could not add to collection"
            )
        }
    }

    fun openAliasDialog() {
        _state.value = _state.value.copy(showAliasDialog = true)
    }

    fun closeAliasDialog() {
        _state.value = _state.value.copy(showAliasDialog = false)
    }

    fun saveAlias(titleAlias: String, artistAlias: String) {
        val release = _state.value.release ?: return
        val newTitleAlias = titleAlias.trim().ifBlank { null }
        val newArtistAlias = artistAlias.trim().ifBlank { null }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingAlias = true)
            discogsRepository.updateAlias(release.id, newTitleAlias, newArtistAlias)
            // Reload to get updated effective title/artist
            val updated = searchReleaseUseCase.getDetail(release.id)
            _state.value = _state.value.copy(
                release = updated,
                isSavingAlias = false,
                showAliasDialog = false
            )
        }
    }

    fun forceReload(releaseId: Int) {
        viewModelScope.launch {
            // Read directly from local DB — no network call, reflects just-saved MBIDs immediately
            val local = discogsRepository.getLocalRelease(releaseId) ?: return@launch
            val current = _state.value.release
            _state.value = _state.value.copy(
                release = if (current != null && current.id == releaseId) {
                    // Merge fresh tracklist/MBID data into the existing full-detail release
                    current.copy(
                        tracklist = local.tracklist,
                        mbzReleaseId = local.mbzReleaseId,
                        titleAlias = local.titleAlias,
                        artistAlias = local.artistAlias,
                        title = local.title,
                        artist = local.artist
                    )
                } else {
                    local
                }
            )
        }
    }

    fun clearScrobbleResult() {
        _state.value = _state.value.copy(scrobbleResult = null)
    }
}
