package com.pollo.scrobbler.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pollo.scrobbler.data.local.entity.CollectionEntity
import com.pollo.scrobbler.data.local.entity.CollectionWithRelease
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Transaction
    @Query("SELECT * FROM collection_items ORDER BY dateAdded DESC")
    fun getCollection(): Flow<List<CollectionWithRelease>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<CollectionEntity>)

    @Query("DELETE FROM collection_items")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM collection_items")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) > 0 FROM collection_items WHERE releaseId = :releaseId")
    suspend fun isInCollection(releaseId: Int): Boolean
}
