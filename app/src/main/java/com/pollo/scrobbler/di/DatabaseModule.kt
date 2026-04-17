package com.pollo.scrobbler.di

import android.content.Context
import androidx.room.Room
import com.pollo.scrobbler.data.local.ScrobblerDatabase
import com.pollo.scrobbler.data.local.dao.CollectionDao
import com.pollo.scrobbler.data.local.dao.ReleaseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScrobblerDatabase =
        Room.databaseBuilder(context, ScrobblerDatabase::class.java, "scrobbler.db")
            .addMigrations(ScrobblerDatabase.MIGRATION_1_2, ScrobblerDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideReleaseDao(db: ScrobblerDatabase): ReleaseDao = db.releaseDao()

    @Provides
    fun provideCollectionDao(db: ScrobblerDatabase): CollectionDao = db.collectionDao()
}
