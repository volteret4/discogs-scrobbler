package com.pollo.scrobbler.domain.model

data class Track(
    val position: String,
    val title: String,
    val duration: String?,
    val mbid: String? = null
)
