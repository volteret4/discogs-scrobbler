package com.pollo.scrobbler.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pollo.scrobbler.data.remote.api.LastFmApiService
import com.pollo.scrobbler.domain.model.RecentScrobble
import com.pollo.scrobbler.domain.model.Track
import com.pollo.scrobbler.domain.repository.LastFmRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LastFmRepositoryImpl @Inject constructor(
    private val api: LastFmApiService,
    @ApplicationContext private val context: Context,
    @Named("lastfmApiKey") private val apiKey: String,
    @Named("lastfmApiSecret") private val apiSecret: String
) : LastFmRepository {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, "lastfm_session", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getAuthUrl(): String =
        "https://www.last.fm/api/auth/?api_key=$apiKey&cb=com.pollo.scrobbler://lastfm-callback"

    override suspend fun handleAuthCallback(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("api_key" to apiKey, "method" to "auth.getSession", "token" to token)
            val response = api.getSession(token = token, apiKey = apiKey, apiSig = sign(params))
            val session = response.session ?: return@withContext false
            prefs.edit().putString("session_key", session.key).putString("username", session.name).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun enrichTracksWithMbids(
        artist: String,
        album: String,
        tracks: List<Track>
    ): List<Track> = withContext(Dispatchers.IO) {
        try {
            val info = api.getAlbumInfo(artist = artist, album = album, apiKey = apiKey)
            val mbidByName = info.album?.tracks?.track
                ?.filter { !it.mbid.isNullOrBlank() }
                ?.associate { it.name!!.lowercase() to it.mbid!! }
                ?: return@withContext tracks

            tracks.map { track ->
                if (track.mbid != null) track
                else track.copy(mbid = mbidByName[track.title.lowercase()])
            }
        } catch (e: Exception) {
            tracks // non-fatal: scrobble without MBIDs if lookup fails
        }
    }

    override suspend fun scrobbleTracks(
        artist: String,
        album: String,
        tracks: List<Track>,
        startTimestamp: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val sessionKey = prefs.getString("session_key", null) ?: return@withContext false
        var allSucceeded = true

        tracks.forEachIndexed { index, track ->
            try {
                val timestamp = startTimestamp + (index * 210L)
                val params = buildMap {
                    put("album[0]", album)
                    put("api_key", apiKey)
                    put("artist[0]", artist)
                    track.mbid?.let { put("mbid[0]", it) }
                    put("method", "track.scrobble")
                    put("sk", sessionKey)
                    put("timestamp[0]", timestamp.toString())
                    put("track[0]", track.title)
                    put("trackNumber[0]", track.position)
                }
                val result = api.scrobble(
                    artist = artist,
                    track = track.title,
                    timestamp = timestamp,
                    album = album,
                    trackNumber = track.position,
                    mbid = track.mbid,
                    apiKey = apiKey,
                    apiSig = sign(params),
                    sessionKey = sessionKey
                )
                if (result.scrobbles?.attr?.accepted == 0) allSucceeded = false
            } catch (e: Exception) {
                allSucceeded = false
            }
        }
        allSucceeded
    }

    override suspend fun getRecentTracks(limit: Int): List<RecentScrobble> = withContext(Dispatchers.IO) {
        val username = prefs.getString("username", null) ?: return@withContext emptyList()
        try {
            val response = api.getRecentTracks(user = username, apiKey = apiKey, limit = limit)
            response.recentTracks?.track?.mapNotNull { dto ->
                val isNowPlaying = dto.attr?.nowplaying == "true"
                val timestamp = dto.date?.uts?.toLongOrNull() ?: if (isNowPlaying) 0L else return@mapNotNull null
                val imageUrl = dto.image?.lastOrNull { it.size == "large" }?.text
                    ?.takeIf { it.isNotBlank() }
                    ?: dto.image?.lastOrNull()?.text?.takeIf { it.isNotBlank() }
                RecentScrobble(
                    artist = dto.artist?.text ?: "",
                    track = dto.name ?: "",
                    album = dto.album?.text ?: "",
                    timestamp = timestamp,
                    imageUrl = imageUrl,
                    isNowPlaying = isNowPlaying
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun removeScrobble(artist: String, track: String, timestamp: Long): Boolean =
        withContext(Dispatchers.IO) {
            val sessionKey = prefs.getString("session_key", null) ?: return@withContext false
            try {
                val params = mapOf(
                    "api_key" to apiKey,
                    "artist" to artist,
                    "method" to "library.removeScrobble",
                    "sk" to sessionKey,
                    "timestamp" to timestamp.toString(),
                    "track" to track
                )
                api.removeScrobble(
                    artist = artist,
                    track = track,
                    timestamp = timestamp,
                    apiKey = apiKey,
                    apiSig = sign(params),
                    sessionKey = sessionKey
                )
                true
            } catch (e: Exception) {
                false
            }
        }

    override fun isAuthenticated(): Boolean = prefs.getString("session_key", null) != null
    override fun getCurrentUsername(): String? = prefs.getString("username", null)
    override fun logout() = prefs.edit().clear().apply()

    private fun sign(params: Map<String, String>): String {
        val base = params.entries.sortedBy { it.key }.joinToString("") { (k, v) -> "$k$v" }
        return md5(base + apiSecret)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray(Charsets.UTF_8)))
            .toString(16).padStart(32, '0')
    }
}
