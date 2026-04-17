package com.pollo.scrobbler.domain.model

data class ReleaseContributor(
    val name: String,
    val role: String,
    val discogsArtistId: Int? = null
)
