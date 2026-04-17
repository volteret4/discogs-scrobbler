package com.pollo.scrobbler.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pollo.scrobbler.data.local.entity.ReleaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ReleaseDao {

    @Query("SELECT * FROM releases WHERE id = :id")
    abstract suspend fun getRelease(id: Int): ReleaseEntity?

    // INSERT IGNORE — returns -1 if the row already exists (no delete → no cascade)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRelease(release: ReleaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertReleases(releases: List<ReleaseEntity>): List<Long>

    @Update
    abstract suspend fun updateRelease(release: ReleaseEntity)

    @Transaction
    open suspend fun upsertRelease(release: ReleaseEntity) {
        if (insertRelease(release) == -1L) updateRelease(release)
    }

    // Partial update that preserves tracklistJson (used when syncing collection basic info)
    @Query("UPDATE releases SET title=:title, artist=:artist, year=:year, barcode=:barcode, coverImageUrl=:coverImageUrl, format=:format WHERE id=:id")
    abstract suspend fun updateBasicInfo(
        id: Int, title: String, artist: String, year: Int?,
        barcode: String?, coverImageUrl: String?, format: String?
    )

    @Transaction
    open suspend fun upsertReleases(releases: List<ReleaseEntity>) {
        val results = insertReleases(releases)
        val toUpdate = releases.indices
            .filter { results[it] == -1L }
            .map { releases[it] }
        // Use partial update to avoid overwriting enriched tracklistJson
        toUpdate.forEach { r ->
            updateBasicInfo(r.id, r.title, r.artist, r.year, r.barcode, r.coverImageUrl, r.format)
        }
    }

    @Query("UPDATE releases SET tracklistJson = :json WHERE id = :id")
    abstract suspend fun updateTracklist(id: Int, json: String)

    @Query("UPDATE releases SET titleAlias = :titleAlias, artistAlias = :artistAlias WHERE id = :id")
    abstract suspend fun updateAlias(id: Int, titleAlias: String?, artistAlias: String?)

    @Query("UPDATE releases SET mbzReleaseId = :mbzReleaseId WHERE id = :id")
    abstract suspend fun updateMbzReleaseId(id: Int, mbzReleaseId: String?)

    @Delete
    abstract suspend fun deleteRelease(release: ReleaseEntity)
}
