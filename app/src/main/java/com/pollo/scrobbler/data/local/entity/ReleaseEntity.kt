package com.pollo.scrobbler.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "releases")
data class ReleaseEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val artist: String,
    val year: Int?,
    val barcode: String?,
    val coverImageUrl: String?,
    val format: String?,
    val tracklistJson: String = "[]",
    val titleAlias: String? = null,
    val artistAlias: String? = null,
    val mbzReleaseId: String? = null
)
