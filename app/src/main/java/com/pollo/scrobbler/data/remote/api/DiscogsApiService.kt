package com.pollo.scrobbler.data.remote.api

import com.pollo.scrobbler.data.remote.dto.AddToCollectionResponseDto
import com.pollo.scrobbler.data.remote.dto.CollectionResponseDto
import com.pollo.scrobbler.data.remote.dto.IdentityDto
import com.pollo.scrobbler.data.remote.dto.ReleaseDetailDto
import com.pollo.scrobbler.data.remote.dto.SearchResponseDto
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscogsApiService {

    @GET("database/search")
    suspend fun searchByBarcode(
        @Query("barcode") barcode: String,
        @Query("per_page") perPage: Int = 10
    ): SearchResponseDto

    @GET("database/search")
    suspend fun searchByQuery(
        @Query("q") query: String,
        @Query("type") type: String = "release",
        @Query("per_page") perPage: Int = 20
    ): SearchResponseDto

    @GET("releases/{id}")
    suspend fun getRelease(@Path("id") id: Int): ReleaseDetailDto

    @GET("users/{username}/collection/folders/0/releases")
    suspend fun getCollection(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "added",
        @Query("sort_order") sortOrder: String = "desc"
    ): CollectionResponseDto

    @GET("oauth/identity")
    suspend fun getIdentity(): IdentityDto

    @POST("users/{username}/collection/folders/0/releases/{releaseId}")
    suspend fun addToCollection(
        @Path("username") username: String,
        @Path("releaseId") releaseId: Int,
        @Body body: RequestBody
    ): AddToCollectionResponseDto

    // Returns raw form-encoded: oauth_token=...&oauth_token_secret=...
    @GET("oauth/access_token")
    suspend fun getAccessToken(): ResponseBody
}
