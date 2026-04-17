package com.pollo.scrobbler.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pollo.scrobbler.data.local.dao.CollectionDao
import com.pollo.scrobbler.data.local.dao.ReleaseDao
import com.pollo.scrobbler.data.local.entity.CollectionEntity
import com.pollo.scrobbler.data.local.entity.ReleaseEntity

@Database(
    entities = [ReleaseEntity::class, CollectionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ScrobblerDatabase : RoomDatabase() {
    abstract fun releaseDao(): ReleaseDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE releases ADD COLUMN titleAlias TEXT")
                db.execSQL("ALTER TABLE releases ADD COLUMN artistAlias TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE releases ADD COLUMN mbzReleaseId TEXT")
            }
        }
    }
}
