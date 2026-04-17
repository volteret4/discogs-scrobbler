package com.pollo.scrobbler.data.remote.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pollo.scrobbler.data.local.entity.CollectionEntity
import com.pollo.scrobbler.data.local.entity.CollectionWithRelease
import com.pollo.scrobbler.data.local.entity.ReleaseEntity
import com.pollo.scrobbler.data.remote.dto.CollectionReleaseDto
import com.pollo.scrobbler.data.remote.dto.ReleaseDetailDto
import com.pollo.scrobbler.data.remote.dto.SearchResultDto
import com.pollo.scrobbler.domain.model.CollectionItem
import com.pollo.scrobbler.domain.model.Release
import com.pollo.scrobbler.domain.model.ReleaseContributor
import com.pollo.scrobbler.domain.model.Track

private val gson = Gson()

fun SearchResultDto.toDomain(): Release {
    val (artist, title) = parseDiscogsTitle(title)
    return Release(
        id = id,
        title = title,
        artist = cleanArtist(artist),
        year = year?.toIntOrNull(),
        barcode = barcode?.firstOrNull(),
        coverImageUrl = coverImage,
        format = format?.joinToString(", ")
    )
}

fun ReleaseDetailDto.toDomain(): Release {
    val artist = resolveArtists(artists?.map { it.name })
    val firstLabel = labels?.firstOrNull()
    val videoId = videos?.mapNotNull { extractYoutubeId(it.uri) }?.firstOrNull()
    return Release(
        id = id,
        title = title,
        artist = artist,
        year = year,
        barcode = null,
        coverImageUrl = images?.firstOrNull { it.type == "primary" }?.uri ?: images?.firstOrNull()?.uri,
        format = formats?.firstOrNull()?.name,
        tracklist = tracklist
            ?.filter { it.type != "heading" }
            ?.map { Track(it.position, it.title, it.duration) }
            ?: emptyList(),
        contributors = extraArtists
            ?.filter { !it.role.isNullOrBlank() }
            ?.map { ReleaseContributor(cleanArtist(it.name), it.role!!, it.id) }
            ?: emptyList(),
        released = released,
        country = country,
        notes = notes?.take(500),
        videoId = videoId,
        label = firstLabel?.let {
            buildString {
                append(it.name)
                if (!it.catNo.isNullOrBlank() && it.catNo != "none") append(" – ${it.catNo}")
            }
        },
        labelId = firstLabel?.id,
        genres = genres ?: emptyList(),
        styles = styles ?: emptyList()
    )
}

private fun extractYoutubeId(url: String): String? {
    val regex = Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})")
    return regex.find(url)?.groupValues?.get(1)
}

fun ReleaseDetailDto.toEntity(): ReleaseEntity {
    val artist = resolveArtists(artists?.map { it.name })
    val tracks = tracklist?.filter { it.type != "heading" }
        ?.map { Track(it.position, it.title, it.duration) } ?: emptyList()
    return ReleaseEntity(
        id = id,
        title = title,
        artist = artist,
        year = year,
        barcode = null,
        coverImageUrl = images?.firstOrNull { it.type == "primary" }?.uri ?: images?.firstOrNull()?.uri,
        format = formats?.firstOrNull()?.name,
        tracklistJson = gson.toJson(tracks)
    )
}

fun CollectionReleaseDto.toEntities(): Pair<ReleaseEntity, CollectionEntity> {
    val artist = resolveArtists(basicInfo.artists?.map { it.name })
    val release = ReleaseEntity(
        id = basicInfo.id,
        title = basicInfo.title,
        artist = artist,
        year = basicInfo.year,
        barcode = null,
        coverImageUrl = basicInfo.coverImage,
        format = basicInfo.formats?.firstOrNull()?.name,
        tracklistJson = "[]"
    )
    val item = CollectionEntity(
        releaseId = basicInfo.id,
        dateAdded = dateAdded,
        folderId = folderId
    )
    return release to item
}

fun CollectionWithRelease.toDomain(): CollectionItem {
    val trackType = object : TypeToken<List<Track>>() {}.type
    val tracks: List<Track> = try {
        gson.fromJson(release.tracklistJson, trackType)
    } catch (e: Exception) {
        emptyList()
    }
    return CollectionItem(
        releaseId = item.releaseId,
        release = Release(
            id = release.id,
            title = release.titleAlias ?: release.title,
            artist = release.artistAlias ?: release.artist,
            year = release.year,
            barcode = release.barcode,
            coverImageUrl = release.coverImageUrl,
            format = release.format,
            tracklist = tracks,
            isInCollection = true,
            titleAlias = release.titleAlias,
            artistAlias = release.artistAlias
        ),
        dateAdded = item.dateAdded,
        folderId = item.folderId
    )
}

/**
 * Joins multiple artists and strips Discogs-specific suffixes:
 * - "(N)" disambiguation numbers: "Prince (5)" → "Prince"
 * - Trailing "*" role markers: "John Lennon *" → "John Lennon"
 * - "Various" → "Various Artists"
 */
private fun resolveArtists(names: List<String>?): String {
    if (names.isNullOrEmpty()) return "Unknown"
    return names
        .map { cleanArtist(it) }
        .filter { it.isNotBlank() }
        .joinToString(" & ")
        .ifBlank { "Unknown" }
}

private fun cleanArtist(name: String): String {
    var clean = name.trim()
    clean = clean.trimEnd('*', ' ')
    clean = clean.replace(Regex("\\s*\\(\\d+\\)\\s*$"), "")
    clean = clean.trim()
    if (clean.equals("various", ignoreCase = true)) clean = "Various Artists"
    return clean
}

fun ReleaseEntity.toDomain(): Release {
    val trackType = object : TypeToken<List<Track>>() {}.type
    val tracks: List<Track> = try { gson.fromJson(tracklistJson, trackType) } catch (_: Exception) { emptyList() }
    return Release(
        id = id,
        title = titleAlias ?: title,
        artist = artistAlias ?: artist,
        year = year,
        barcode = barcode,
        coverImageUrl = coverImageUrl,
        format = format,
        tracklist = tracks,
        titleAlias = titleAlias,
        artistAlias = artistAlias,
        mbzReleaseId = mbzReleaseId
    )
}

/** Splits "Artist - Title" format used by Discogs search results */
private fun parseDiscogsTitle(raw: String): Pair<String, String> {
    val idx = raw.indexOf(" - ")
    return if (idx >= 0) raw.substring(0, idx) to raw.substring(idx + 3)
    else "Unknown" to raw
}
