package com.pollo.scrobbler.ui.screens.mbzsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pollo.scrobbler.data.remote.api.MusicBrainzApiService
import com.pollo.scrobbler.data.remote.dto.MbzReleaseGroupDto
import com.pollo.scrobbler.data.remote.dto.MbzReleaseInGroupDto
import com.pollo.scrobbler.data.remote.dto.MbzTrackDto
import com.pollo.scrobbler.domain.model.Track
import com.pollo.scrobbler.domain.repository.DiscogsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MbzStep {
    data object SearchGroups : MbzStep()
    data class BrowseReleases(val group: MbzReleaseGroupDto) : MbzStep()
    data class CompareTracklist(
        val release: MbzReleaseInGroupDto,
        val mbzTracks: List<MbzTrackDto>,
        val matchCount: Int
    ) : MbzStep()
}

data class MbzSearchUiState(
    val releaseId: Int = 0,
    val query: String = "",
    val step: MbzStep = MbzStep.SearchGroups,
    val isLoading: Boolean = false,
    val error: String? = null,
    val releaseGroups: List<MbzReleaseGroupDto> = emptyList(),
    val releases: List<MbzReleaseInGroupDto> = emptyList(),
    val existingTracks: List<Track> = emptyList(),
    val saved: Boolean = false
)

@HiltViewModel
class MbzSearchViewModel @Inject constructor(
    private val musicBrainzApi: MusicBrainzApiService,
    private val discogsRepository: DiscogsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MbzSearchUiState())
    val state: StateFlow<MbzSearchUiState> = _state.asStateFlow()

    fun init(releaseId: Int) {
        if (_state.value.releaseId == releaseId) return
        viewModelScope.launch {
            val release = discogsRepository.getLocalRelease(releaseId)
            val safeTitle = sanitize(release?.title ?: "")
            val safeArtist = sanitize(release?.artist ?: "")
            val query = buildString {
                if (safeArtist.isNotBlank()) append("artist:\"${safeArtist.lowercase()}\" AND ")
                append("releasegroup:\"${safeTitle.lowercase()}\"")
            }
            _state.value = _state.value.copy(
                releaseId = releaseId,
                query = query,
                existingTracks = release?.tracklist ?: emptyList()
            )
        }
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q, error = null)
    }

    fun searchGroups() {
        val q = _state.value.query.trim().ifBlank { return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, releaseGroups = emptyList())
            try {
                val result = musicBrainzApi.searchReleaseGroups(q)
                _state.value = _state.value.copy(
                    releaseGroups = result.releaseGroups ?: emptyList(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun selectGroup(group: MbzReleaseGroupDto) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                step = MbzStep.BrowseReleases(group),
                releases = emptyList()
            )
            try {
                delay(1000) // MBZ rate limit
                val result = musicBrainzApi.getReleasesByGroup(group.id)
                val sorted = (result.releases ?: emptyList()).sortedWith(
                    compareBy({ it.date ?: "9999" }, { it.country ?: "ZZ" })
                )
                _state.value = _state.value.copy(releases = sorted, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun selectRelease(release: MbzReleaseInGroupDto) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                delay(1000) // MBZ rate limit
                val detail = musicBrainzApi.getReleaseRecordings(release.id)
                val mbzTracks = detail.media?.flatMap { it.tracks ?: emptyList() } ?: emptyList()
                val existingTitles = _state.value.existingTracks.map { it.title.lowercase() }.toSet()
                val matchCount = mbzTracks.count { t ->
                    t.recording != null && existingTitles.contains(t.title.lowercase())
                }
                _state.value = _state.value.copy(
                    step = MbzStep.CompareTracklist(release, mbzTracks, matchCount),
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun applyMbids() {
        val step = _state.value.step as? MbzStep.CompareTracklist ?: return
        viewModelScope.launch {
            // Re-read from DB to ensure we have latest tracks (defensive)
            val existing = _state.value.existingTracks.ifEmpty {
                discogsRepository.getLocalRelease(_state.value.releaseId)?.tracklist ?: emptyList()
            }
            val mbidByTitle = step.mbzTracks
                .mapNotNull { t -> t.recording?.id?.let { t.title.lowercase() to it } }
                .toMap()
            val enriched = existing.map { track ->
                track.copy(mbid = mbidByTitle[track.title.lowercase()] ?: track.mbid)
            }
            if (enriched.isNotEmpty()) {
                discogsRepository.updateCachedTracklist(_state.value.releaseId, enriched)
            }
            // Save the selected MBZ release MBID
            discogsRepository.updateMbzReleaseId(_state.value.releaseId, step.release.id)
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun backToGroups() {
        _state.value = _state.value.copy(
            step = MbzStep.SearchGroups,
            releases = emptyList(),
            error = null
        )
    }

    fun backToReleases() {
        val group = (_state.value.step as? MbzStep.BrowseReleases)?.group
            ?: (_state.value.step as? MbzStep.CompareTracklist)?.release?.let { null }
        // Recover the group from browse step if we went forward
        _state.value = _state.value.copy(
            step = if (group != null) MbzStep.BrowseReleases(group) else MbzStep.SearchGroups,
            error = null
        )
    }

    // Store selected group so we can go back to releases list
    private var lastGroup: MbzReleaseGroupDto? = null

    fun selectGroupAndStore(group: MbzReleaseGroupDto) {
        lastGroup = group
        selectGroup(group)
    }

    fun backToReleaseList() {
        val group = lastGroup ?: return backToGroups()
        _state.value = _state.value.copy(
            step = MbzStep.BrowseReleases(group),
            error = null
        )
    }

    private fun sanitize(s: String): String =
        s.replace(Regex("[\"\\\\]"), "")
         .replace(Regex("[''\u2019\u2018\u00B4`]"), "")
         .replace(Regex("[?¿¡!;:()\\[\\]{}^~*|&+\\-_]"), " ")
         .replace(Regex("\\s+"), " ")
         .trim()
}
