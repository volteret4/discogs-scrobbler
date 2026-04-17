package com.pollo.scrobbler.data.remote.api

import com.pollo.scrobbler.data.remote.dto.MbzReleaseBrowseDto
import com.pollo.scrobbler.data.remote.dto.MbzReleaseDto
import com.pollo.scrobbler.data.remote.dto.MbzReleaseGroupSearchDto
import com.pollo.scrobbler.data.remote.dto.MbzReleaseSearchDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicBrainzApiService {

    // Search releases (used in completarInfo)
    @GET("release")
    suspend fun searchRelease(
        @Query("query") query: String,
        @Query("fmt") format: String = "json",
        @Query("limit") limit: Int = 3
    ): MbzReleaseSearchDto

    // Lookup release by MBID with full recordings
    @GET("release/{mbid}")
    suspend fun getReleaseRecordings(
        @Path("mbid") mbid: String,
        @Query("inc") inc: String = "recordings",
        @Query("fmt") format: String = "json"
    ): MbzReleaseDto

    // Search release groups (for in-app MBZ search)
    @GET("release-group")
    suspend fun searchReleaseGroups(
        @Query("query") query: String,
        @Query("fmt") format: String = "json",
        @Query("limit") limit: Int = 15
    ): MbzReleaseGroupSearchDto

    // Browse releases belonging to a release group (includes track counts via inc=media)
    @GET("release")
    suspend fun getReleasesByGroup(
        @Query("release-group") releaseGroupId: String,
        @Query("inc") inc: String = "media",
        @Query("fmt") format: String = "json",
        @Query("limit") limit: Int = 50
    ): MbzReleaseBrowseDto
}
