package com.pollo.scrobbler.domain.model

data class CollectionItem(
    val releaseId: Int,
    val release: Release,
    val dateAdded: String,
    val folderId: Int = 0
)
