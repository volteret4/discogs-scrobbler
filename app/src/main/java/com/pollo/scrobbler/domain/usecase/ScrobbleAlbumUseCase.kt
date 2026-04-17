package com.pollo.scrobbler.domain.usecase

import com.pollo.scrobbler.domain.model.Release
import com.pollo.scrobbler.domain.repository.DiscogsRepository
import com.pollo.scrobbler.domain.repository.LastFmRepository
import javax.inject.Inject

class ScrobbleAlbumUseCase @Inject constructor(
    private val lastFmRepository: LastFmRepository,
    private val discogsRepository: DiscogsRepository
) {
    suspend operator fun invoke(release: Release): Result<Unit> {
        if (!lastFmRepository.isAuthenticated())
            return Result.failure(IllegalStateException("Not authenticated with Last.fm"))
        if (release.tracklist.isEmpty())
            return Result.failure(IllegalStateException("No tracklist available"))

        // Enrich with MBIDs if not already cached
        val tracks = if (release.tracklist.any { it.mbid == null }) {
            val enriched = lastFmRepository.enrichTracksWithMbids(
                release.artist, release.title, release.tracklist
            )
            // Persist to Room so next scrobble uses cached MBIDs
            if (enriched.any { it.mbid != null }) {
                discogsRepository.updateCachedTracklist(release.id, enriched)
            }
            enriched
        } else {
            release.tracklist
        }

        val timestamp = System.currentTimeMillis() / 1000
        val success = lastFmRepository.scrobbleTracks(release.artist, release.title, tracks, timestamp)
        return if (success) Result.success(Unit) else Result.failure(Exception("Scrobble failed"))
    }
}
