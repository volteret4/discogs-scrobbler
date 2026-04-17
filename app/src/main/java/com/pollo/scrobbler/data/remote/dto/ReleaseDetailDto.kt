package com.pollo.scrobbler.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ReleaseDetailDto(
    val id: Int,
    val title: String,
    val year: Int?,
    @SerializedName("artists_sort") val artistsSort: String?,
    val artists: List<ArtistDto>?,
    val images: List<ImageDto>?,
    val formats: List<FormatDto>?,
    val tracklist: List<TrackDto>?,
    val labels: List<LabelDto>?,
    val genres: List<String>?,
    val styles: List<String>?,
    @SerializedName("extraartists") val extraArtists: List<ExtraArtistDto>?,
    val released: String?,
    val country: String?,
    val notes: String?,
    val videos: List<VideoDto>?
)

data class ArtistDto(val name: String, val id: Int?)
data class ImageDto(val uri: String, val type: String?)
data class FormatDto(val name: String, val descriptions: List<String>?)
data class TrackDto(
    val position: String,
    val title: String,
    val duration: String?,
    @SerializedName("type_") val type: String? = null
)
data class LabelDto(val name: String, val id: Int?, @SerializedName("catno") val catNo: String?)
data class ExtraArtistDto(val name: String, val id: Int?, val role: String?)
data class VideoDto(val uri: String, val title: String?, val description: String?)
