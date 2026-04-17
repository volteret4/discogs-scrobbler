package com.pollo.scrobbler.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LastFmSessionResponseDto(
    val session: LastFmSessionDto?
)

data class LastFmSessionDto(
    val name: String,
    val key: String,
    val subscriber: Int
)

data class LastFmScrobbleResponseDto(
    val scrobbles: LastFmScrobblesWrapper?
)

data class LastFmScrobblesWrapper(
    @SerializedName("@attr") val attr: LastFmScrobbleAttr?
)

data class LastFmScrobbleAttr(
    val accepted: Int,
    val ignored: Int
)

data class LastFmAlbumInfoResponseDto(
    val album: LastFmAlbumDto?
)

data class LastFmAlbumDto(
    val name: String?,
    val artist: String?,
    val mbid: String?,
    val tracks: LastFmTracksWrapper?
)

data class LastFmTracksWrapper(
    val track: List<LastFmTrackDto>?
)

data class LastFmTrackDto(
    val name: String?,
    val mbid: String?,
    val duration: String?,
    @SerializedName("@attr") val attr: LastFmTrackAttr?
)

data class LastFmTrackAttr(val rank: Int?)

data class LastFmRecentTracksResponseDto(
    @SerializedName("recenttracks") val recentTracks: LastFmRecentTracksWrapper?
)

data class LastFmRecentTracksWrapper(
    val track: List<LastFmRecentTrackDto>?
)

data class LastFmRecentTrackDto(
    val artist: LastFmTextDto?,
    val name: String?,
    val album: LastFmTextDto?,
    val date: LastFmDateDto?,
    val image: List<LastFmImageDto>?,
    @SerializedName("@attr") val attr: LastFmNowPlayingAttr?
)

data class LastFmTextDto(@SerializedName("#text") val text: String?)
data class LastFmDateDto(val uts: String?)
data class LastFmImageDto(@SerializedName("#text") val text: String?, val size: String?)
data class LastFmNowPlayingAttr(val nowplaying: String?)
