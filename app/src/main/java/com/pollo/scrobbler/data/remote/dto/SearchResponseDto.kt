package com.pollo.scrobbler.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SearchResponseDto(
    val results: List<SearchResultDto> = emptyList(),
    val pagination: PaginationDto? = null
)

data class SearchResultDto(
    val id: Int,
    val title: String,
    val year: String?,
    val format: List<String>?,
    @SerializedName("cover_image") val coverImage: String?,
    val barcode: List<String>?,
    val type: String?
)

data class PaginationDto(
    val pages: Int,
    val page: Int,
    val items: Int
)
