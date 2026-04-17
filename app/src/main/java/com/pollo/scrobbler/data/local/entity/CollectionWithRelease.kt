package com.pollo.scrobbler.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CollectionWithRelease(
    @Embedded val item: CollectionEntity,
    @Relation(parentColumn = "releaseId", entityColumn = "id")
    val release: ReleaseEntity
)
