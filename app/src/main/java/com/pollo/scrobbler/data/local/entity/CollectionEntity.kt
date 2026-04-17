package com.pollo.scrobbler.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collection_items",
    foreignKeys = [ForeignKey(
        entity = ReleaseEntity::class,
        parentColumns = ["id"],
        childColumns = ["releaseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("releaseId")]
)
data class CollectionEntity(
    @PrimaryKey val releaseId: Int,
    val dateAdded: String,
    val folderId: Int
)
