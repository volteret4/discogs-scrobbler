package com.pollo.scrobbler.data.remote.api

import com.pollo.scrobbler.data.remote.dto.LastFmAlbumInfoResponseDto
import com.pollo.scrobbler.data.remote.dto.LastFmRecentTracksResponseDto
import com.pollo.scrobbler.data.remote.dto.LastFmScrobbleResponseDto
import com.pollo.scrobbler.data.remote.dto.LastFmSessionResponseDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LastFmApiService {

    @POST(".")
    @FormUrlEncoded
    suspend fun getSession(
        @Field("method") method: String = "auth.getSession",
        @Field("token") token: String,
        @Field("api_key") apiKey: String,
        @Field("api_sig") apiSig: String,
        @Field("format") format: String = "json"
    ): LastFmSessionResponseDto

    @GET(".")
    suspend fun getAlbumInfo(
        @Query("method") method: String = "album.getInfo",
        @Query("artist") artist: String,
        @Query("album") album: String,
        @Query("api_key") apiKey: String,
        @Query("autocorrect") autocorrect: Int = 1,
        @Query("format") format: String = "json"
    ): LastFmAlbumInfoResponseDto

    @GET(".")
    suspend fun getRecentTracks(
        @Query("method") method: String = "user.getrecenttracks",
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 200,
        @Query("format") format: String = "json"
    ): LastFmRecentTracksResponseDto

    @POST(".")
    @FormUrlEncoded
    suspend fun removeScrobble(
        @Field("method") method: String = "library.removeScrobble",
        @Field("artist") artist: String,
        @Field("track") track: String,
        @Field("timestamp") timestamp: Long,
        @Field("api_key") apiKey: String,
        @Field("api_sig") apiSig: String,
        @Field("sk") sessionKey: String,
        @Field("format") format: String = "json"
    ): LastFmScrobbleResponseDto

    @POST(".")
    @FormUrlEncoded
    suspend fun scrobble(
        @Field("method") method: String = "track.scrobble",
        @Field("artist[0]") artist: String,
        @Field("track[0]") track: String,
        @Field("timestamp[0]") timestamp: Long,
        @Field("album[0]") album: String,
        @Field("trackNumber[0]") trackNumber: String,
        @Field("mbid[0]") mbid: String? = null,
        @Field("api_key") apiKey: String,
        @Field("api_sig") apiSig: String,
        @Field("sk") sessionKey: String,
        @Field("format") format: String = "json"
    ): LastFmScrobbleResponseDto
}
