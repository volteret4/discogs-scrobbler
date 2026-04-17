package com.pollo.scrobbler.domain.usecase

import com.pollo.scrobbler.data.remote.api.MusicBrainzApiService
import com.pollo.scrobbler.domain.model.CollectionItem
import com.pollo.scrobbler.domain.repository.DiscogsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetCollectionUseCase @Inject constructor(
    private val repository: DiscogsRepository,
    private val musicBrainzApi: MusicBrainzApiService
) {
    operator fun invoke(): Flow<List<CollectionItem>> = repository.getCollection()

    suspend fun sync() = repository.syncCollection()

    suspend fun completarInfo(onProgress: suspend (String) -> Unit) {
        // Phase 1: sync from Discogs
        onProgress("Sincronizando colección…")
        repository.syncCollection()

        // Phase 2: fetch full release detail for items without tracklist
        var items = repository.getCollection().first()
        val needsDetail = items.filter { it.release.tracklist.isEmpty() }
        if (needsDetail.isNotEmpty()) {
            needsDetail.forEachIndexed { idx, item ->
                onProgress("Tracklist ${idx + 1}/${needsDetail.size}: ${item.release.title.take(30)}")
                try {
                    repository.getReleaseDetail(item.releaseId)
                } catch (_: Exception) {}
                delay(1200) // Discogs: 60 req/min
            }
            items = repository.getCollection().first()
        }

        // Phase 3: two-step MusicBrainz enrichment
        //   Step A: search for release MBID
        //   Step B: lookup release by MBID with inc=recordings (search doesn't support inc)
        val needsMbid = items.filter { item ->
            item.release.tracklist.isNotEmpty() && item.release.tracklist.any { it.mbid == null }
        }

        if (needsMbid.isEmpty()) {
            onProgress("✓ Todos los MBIDs ya están completos")
            return
        }

        needsMbid.forEachIndexed { idx, item ->
            onProgress("MusicBrainz ${idx + 1}/${needsMbid.size}: ${item.release.title.take(30)}")
            try {
                // Step A: find release MBID
                val safeTitle = sanitizeForLucene(item.release.title)
                val safeArtist = sanitizeForLucene(item.release.artist)
                val query = "release:\"$safeTitle\" AND artist:\"$safeArtist\""
                val searchResult = musicBrainzApi.searchRelease(query)
                val releaseMbid = searchResult.releases?.firstOrNull()?.id
                    ?: return@forEachIndexed
                delay(1300)

                // Step B: fetch recordings for that specific release
                val releaseDetail = musicBrainzApi.getReleaseRecordings(releaseMbid)
                val mbidByTitle = releaseDetail.media
                    ?.flatMap { it.tracks ?: emptyList() }
                    ?.mapNotNull { t -> t.recording?.id?.let { t.title.lowercase() to it } }
                    ?.toMap() ?: return@forEachIndexed

                val enriched = item.release.tracklist.map { track ->
                    if (track.mbid != null) track
                    else track.copy(mbid = mbidByTitle[track.title.lowercase()])
                }
                if (enriched.any { it.mbid != null }) {
                    repository.updateCachedTracklist(item.releaseId, enriched)
                }
            } catch (_: Exception) {}
            delay(1300)
        }

        onProgress("✓ Completado")
    }

    // Strip characters that break Lucene phrase queries or confuse MusicBrainz tokenizer
    private fun sanitizeForLucene(s: String): String =
        s.replace(Regex("[\"\\\\]"), "")
         .replace(Regex("[''\u2019\u2018\u00B4`]"), "")
         .replace(Regex("[?¿¡!;:()\\[\\]{}^~*|&+\\-_]"), " ")
         .replace(Regex("\\s+"), " ")
         .trim()
}
