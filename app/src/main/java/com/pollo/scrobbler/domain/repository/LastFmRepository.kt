package com.pollo.scrobbler.domain.repository

import com.pollo.scrobbler.domain.model.RecentScrobble
import com.pollo.scrobbler.domain.model.Track

interface LastFmRepository {
    fun getAuthUrl(): String
    suspend fun handleAuthCallback(token: String): Boolean
    suspend fun enrichTracksWithMbids(artist: String, album: String, tracks: List<Track>): List<Track>
    suspend fun scrobbleTracks(artist: String, album: String, tracks: List<Track>, startTimestamp: Long): Boolean
    suspend fun getRecentTracks(limit: Int = 200): List<RecentScrobble>
    suspend fun removeScrobble(artist: String, track: String, timestamp: Long): Boolean
    fun isAuthenticated(): Boolean
    fun getCurrentUsername(): String?
    fun logout()
}
