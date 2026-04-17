package com.pollo.scrobbler.di

import com.pollo.scrobbler.data.remote.interceptor.EncryptedOAuthTokenProvider
import com.pollo.scrobbler.data.remote.interceptor.OAuthTokenProvider
import com.pollo.scrobbler.data.repository.DiscogsRepositoryImpl
import com.pollo.scrobbler.data.repository.LastFmRepositoryImpl
import com.pollo.scrobbler.domain.repository.DiscogsRepository
import com.pollo.scrobbler.domain.repository.LastFmRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOAuthTokenProvider(impl: EncryptedOAuthTokenProvider): OAuthTokenProvider

    @Binds
    @Singleton
    abstract fun bindDiscogsRepository(impl: DiscogsRepositoryImpl): DiscogsRepository

    @Binds
    @Singleton
    abstract fun bindLastFmRepository(impl: LastFmRepositoryImpl): LastFmRepository
}
