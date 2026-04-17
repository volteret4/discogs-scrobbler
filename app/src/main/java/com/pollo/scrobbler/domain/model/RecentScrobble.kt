package com.pollo.scrobbler.domain.model

data class RecentScrobble(
    val artist: String,
    val track: String,
    val album: String,
    val timestamp: Long,
    val imageUrl: String?,
    val isNowPlaying: Boolean = false
)
