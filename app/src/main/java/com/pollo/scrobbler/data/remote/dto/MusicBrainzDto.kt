package com.pollo.scrobbler.data.remote.dto

import com.google.gson.annotations.SerializedName

// --- existing search/lookup ---

data class MbzReleaseSearchDto(
    val releases: List<MbzReleaseDto>?
)

data class MbzReleaseDto(
    val id: String,
    val title: String,
    val media: List<MbzMediaDto>?
)

data class MbzMediaDto(
    val tracks: List<MbzTrackDto>?,
    @SerializedName("track-count") val trackCount: Int?,
    val format: String?
)

data class MbzTrackDto(
    val id: String,
    val title: String,
    val number: String?,
    val recording: MbzRecordingDto?
)

data class MbzRecordingDto(val id: String, val title: String?)

// --- release group search ---

data class MbzReleaseGroupSearchDto(
    @SerializedName("release-groups") val releaseGroups: List<MbzReleaseGroupDto>?
)

data class MbzReleaseGroupDto(
    val id: String,
    val title: String,
    @SerializedName("primary-type") val primaryType: String?,
    @SerializedName("first-release-date") val firstReleaseDate: String?,
    @SerializedName("artist-credit") val artistCredit: List<MbzArtistCreditDto>?,
    val score: Int?
)

data class MbzArtistCreditDto(
    val name: String?,
    val artist: MbzArtistDto?
)

data class MbzArtistDto(val id: String?, val name: String?)

// --- browse releases by release-group ---

data class MbzReleaseBrowseDto(
    val releases: List<MbzReleaseInGroupDto>?,
    @SerializedName("release-count") val releaseCount: Int?
)

data class MbzReleaseInGroupDto(
    val id: String,
    val title: String,
    val date: String?,
    val country: String?,
    val status: String?,
    val media: List<MbzMediaDto>?
) {
    val totalTracks: Int get() = media?.sumOf { it.trackCount ?: it.tracks?.size ?: 0 } ?: 0
}
