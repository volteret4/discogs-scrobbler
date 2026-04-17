package com.pollo.scrobbler.domain.model

data class Release(
    val id: Int,
    val title: String,
    val artist: String,
    val year: Int?,
    val barcode: String?,
    val coverImageUrl: String?,
    val format: String?,
    val tracklist: List<Track> = emptyList(),
    val isInCollection: Boolean = false,
    val contributors: List<ReleaseContributor> = emptyList(),
    val released: String? = null,
    val country: String? = null,
    val notes: String? = null,
    val videoId: String? = null,
    val label: String? = null,
    val labelId: Int? = null,
    val genres: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
    val titleAlias: String? = null,
    val artistAlias: String? = null,
    val mbzReleaseId: String? = null
)
