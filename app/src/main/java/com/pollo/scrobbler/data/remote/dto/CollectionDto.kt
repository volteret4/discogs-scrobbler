package com.pollo.scrobbler.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CollectionResponseDto(
    val releases: List<CollectionReleaseDto> = emptyList(),
    val pagination: PaginationDto? = null
)

data class CollectionReleaseDto(
    val id: Int,
    @SerializedName("date_added") val dateAdded: String,
    @SerializedName("folder_id") val folderId: Int,
    @SerializedName("basic_information") val basicInfo: BasicInfoDto
)

data class BasicInfoDto(
    val id: Int,
    val title: String,
    val year: Int?,
    val formats: List<FormatDto>?,
    @SerializedName("cover_image") val coverImage: String?,
    val artists: List<ArtistDto>?,
    val labels: List<LabelDto>?
)

data class IdentityDto(
    val id: Int,
    val username: String,
    @SerializedName("resource_url") val resourceUrl: String
)

data class AddToCollectionResponseDto(
    @SerializedName("instance_id") val instanceId: Int,
    @SerializedName("resource_url") val resourceUrl: String
)
