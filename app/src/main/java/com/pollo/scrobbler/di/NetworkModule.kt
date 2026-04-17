package com.pollo.scrobbler.di

import com.pollo.scrobbler.BuildConfig
import com.pollo.scrobbler.data.remote.api.DiscogsApiService
import com.pollo.scrobbler.data.remote.api.LastFmApiService
import com.pollo.scrobbler.data.remote.api.MusicBrainzApiService
import com.pollo.scrobbler.data.remote.interceptor.OAuthInterceptor
import com.pollo.scrobbler.data.remote.interceptor.OAuthTokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Named("consumerKey")
    fun provideConsumerKey(): String = BuildConfig.DISCOGS_CONSUMER_KEY

    @Provides
    @Named("consumerSecret")
    fun provideConsumerSecret(): String = BuildConfig.DISCOGS_CONSUMER_SECRET

    @Provides
    @Named("lastfmApiKey")
    fun provideLastFmApiKey(): String = BuildConfig.LASTFM_API_KEY

    @Provides
    @Named("lastfmApiSecret")
    fun provideLastFmApiSecret(): String = BuildConfig.LASTFM_API_SECRET

    @Provides
    @Singleton
    @Named("rawOkHttp")
    fun provideRawOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    @Provides
    @Singleton
    fun provideDiscogsOkHttpClient(
        tokenProvider: OAuthTokenProvider,
        @Named("consumerKey") consumerKey: String,
        @Named("consumerSecret") consumerSecret: String
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(OAuthInterceptor(consumerKey, consumerSecret, tokenProvider))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    @Provides
    @Singleton
    fun provideDiscogsApi(okHttpClient: OkHttpClient): DiscogsApiService =
        Retrofit.Builder()
            .baseUrl("https://api.discogs.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DiscogsApiService::class.java)

    @Provides
    @Singleton
    fun provideLastFmApi(@Named("rawOkHttp") okHttpClient: OkHttpClient): LastFmApiService =
        Retrofit.Builder()
            .baseUrl("https://ws.audioscrobbler.com/2.0/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastFmApiService::class.java)

    @Provides
    @Singleton
    fun provideMusicBrainzApi(): MusicBrainzApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "discogs-scrobbler/1.0 (https://github.com/volteret4/discogs-scrobbler)")
                        .build()
                )
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                        else HttpLoggingInterceptor.Level.NONE
            })
            .build()
        return Retrofit.Builder()
            .baseUrl("https://musicbrainz.org/ws/2/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApiService::class.java)
    }
}
