package com.pollo.scrobbler.data.repository

import android.util.Base64
import com.pollo.scrobbler.data.local.dao.CollectionDao
import com.pollo.scrobbler.data.local.dao.ReleaseDao
import com.pollo.scrobbler.data.remote.api.DiscogsApiService
import com.pollo.scrobbler.data.remote.interceptor.OAuthTokenProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pollo.scrobbler.data.remote.mapper.toDomain
import com.pollo.scrobbler.data.remote.mapper.toEntities
import com.pollo.scrobbler.data.remote.mapper.toEntity
import com.pollo.scrobbler.domain.model.CollectionItem
import com.pollo.scrobbler.domain.model.Release
import com.pollo.scrobbler.domain.model.Track
import com.pollo.scrobbler.domain.repository.DiscogsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Named

class DiscogsRepositoryImpl @Inject constructor(
    private val api: DiscogsApiService,
    private val releaseDao: ReleaseDao,
    private val collectionDao: CollectionDao,
    private val tokenProvider: OAuthTokenProvider,
    @Named("consumerKey") private val consumerKey: String,
    @Named("consumerSecret") private val consumerSecret: String,
    @Named("rawOkHttp") private val rawOkHttpClient: OkHttpClient
) : DiscogsRepository {

    override suspend fun searchByBarcode(barcode: String): List<Release> =
        api.searchByBarcode(barcode).results.map { it.toDomain() }

    override suspend fun searchByQuery(query: String): List<Release> =
        api.searchByQuery(query).results.map { it.toDomain() }

    private val gson = Gson()

    override suspend fun getReleaseDetail(releaseId: Int): Release {
        val detail = api.getRelease(releaseId)
        val newEntity = detail.toEntity()

        // Preserve MBIDs and aliases — Discogs never returns them
        val existing = releaseDao.getRelease(releaseId)
        val mergedTracks = if (existing != null && existing.tracklistJson != "[]") {
            mergeTracklistMbids(detail.toDomain().tracklist, existing.tracklistJson)
        } else {
            detail.toDomain().tracklist
        }

        releaseDao.upsertRelease(newEntity.copy(
            tracklistJson = gson.toJson(mergedTracks),
            titleAlias = existing?.titleAlias,
            artistAlias = existing?.artistAlias,
            mbzReleaseId = existing?.mbzReleaseId
        ))
        return detail.toDomain().copy(
            tracklist = mergedTracks,
            titleAlias = existing?.titleAlias,
            artistAlias = existing?.artistAlias,
            title = existing?.titleAlias ?: detail.title,
            artist = existing?.artistAlias ?: detail.toDomain().artist,
            mbzReleaseId = existing?.mbzReleaseId
        )
    }

    private fun mergeTracklistMbids(newTracks: List<Track>, oldJson: String): List<Track> {
        val type = object : TypeToken<List<Track>>() {}.type
        val oldTracks: List<Track> = try { gson.fromJson(oldJson, type) } catch (_: Exception) { return newTracks }
        val mbidByTitle = oldTracks.filter { it.mbid != null }.associate { it.title.lowercase() to it.mbid!! }
        if (mbidByTitle.isEmpty()) return newTracks
        return newTracks.map { track ->
            if (track.mbid != null) track else track.copy(mbid = mbidByTitle[track.title.lowercase()])
        }
    }

    override fun getCollection(): Flow<List<CollectionItem>> =
        collectionDao.getCollection().map { items -> items.map { it.toDomain() } }

    override suspend fun syncCollection() {
        val username = tokenProvider.getUsername() ?: return
        var page = 1
        val allReleases = mutableListOf<com.pollo.scrobbler.data.local.entity.ReleaseEntity>()
        val allItems = mutableListOf<com.pollo.scrobbler.data.local.entity.CollectionEntity>()
        do {
            val response = api.getCollection(username, page = page)
            response.releases.forEach { dto ->
                val (release, item) = dto.toEntities()
                allReleases += release
                allItems += item
            }
            val totalPages = response.pagination?.pages ?: 1
            page++
        } while (page <= totalPages)
        releaseDao.upsertReleases(allReleases)
        collectionDao.clearAll()
        collectionDao.upsertItems(allItems)
    }

    override suspend fun isAuthenticated(): Boolean = tokenProvider.getAccessToken() != null

    override suspend fun getRequestTokenUrl(): String = withContext(Dispatchers.IO) {
        val callbackUrl = "com.pollo.scrobbler://oauth-callback"
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val nonce = UUID.randomUUID().toString().replace("-", "")

        val params = linkedMapOf(
            "oauth_callback" to callbackUrl,
            "oauth_consumer_key" to consumerKey,
            "oauth_nonce" to nonce,
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to timestamp,
            "oauth_version" to "1.0"
        )
        val baseUrl = "https://api.discogs.com/oauth/request_token"
        params["oauth_signature"] = sign("POST", baseUrl, params, consumerSecret, "")

        val authHeader = "OAuth " + params.entries.joinToString(", ") { (k, v) -> """$k="${encode(v)}"""" }

        val request = Request.Builder()
            .url(baseUrl)
            .header("Authorization", authHeader)
            .post(ByteArray(0).toRequestBody())
            .build()

        val response = rawOkHttpClient.newCall(request).execute()
        val body = response.use { it.body?.string() }
            ?: throw Exception("Empty response from Discogs")
        check(response.isSuccessful) { "Request token failed (${response.code}): $body" }

        val parsed = parseFormEncoded(body)
        val requestToken = parsed["oauth_token"]
            ?: throw Exception("No oauth_token in response: $body")
        val requestTokenSecret = parsed["oauth_token_secret"]
            ?: throw Exception("No oauth_token_secret in response: $body")

        tokenProvider.savePendingRequestToken(requestToken, requestTokenSecret)
        "https://www.discogs.com/oauth/authorize?oauth_token=$requestToken"
    }

    override suspend fun handleOAuthCallback(oauthToken: String, oauthVerifier: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val tokenSecret = tokenProvider.getPendingRequestTokenSecret() ?: return@withContext false
                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val nonce = UUID.randomUUID().toString().replace("-", "")

                val params = linkedMapOf(
                    "oauth_consumer_key" to consumerKey,
                    "oauth_nonce" to nonce,
                    "oauth_signature_method" to "HMAC-SHA1",
                    "oauth_timestamp" to timestamp,
                    "oauth_token" to oauthToken,
                    "oauth_verifier" to oauthVerifier,
                    "oauth_version" to "1.0"
                )
                val baseUrl = "https://api.discogs.com/oauth/access_token"
                params["oauth_signature"] = sign("POST", baseUrl, params, consumerSecret, tokenSecret)

                val authHeader = "OAuth " + params.entries.joinToString(", ") { (k, v) -> """$k="${encode(v)}"""" }

                val request = Request.Builder()
                    .url(baseUrl)
                    .header("Authorization", authHeader)
                    .post(ByteArray(0).toRequestBody())
                    .build()

                val response = rawOkHttpClient.newCall(request).execute()
                val body = response.use { it.body?.string() } ?: return@withContext false
                if (!response.isSuccessful) return@withContext false

                val parsed = parseFormEncoded(body)
                val accessToken = parsed["oauth_token"] ?: return@withContext false
                val accessTokenSecret = parsed["oauth_token_secret"] ?: return@withContext false
                tokenProvider.saveAccessTokens(accessToken, accessTokenSecret)

                val identity = api.getIdentity()
                tokenProvider.saveUsername(identity.username)
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun updateCachedTracklist(releaseId: Int, tracks: List<com.pollo.scrobbler.domain.model.Track>) {
        val json = com.google.gson.Gson().toJson(tracks)
        releaseDao.updateTracklist(releaseId, json)
    }

    override suspend fun addToCollection(releaseId: Int): Boolean {
        val username = tokenProvider.getUsername() ?: return false
        return try {
            api.addToCollection(username, releaseId, ByteArray(0).toRequestBody())
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US)
                .format(java.util.Date())
            collectionDao.upsertItems(listOf(com.pollo.scrobbler.data.local.entity.CollectionEntity(
                releaseId = releaseId,
                dateAdded = now,
                folderId = 0
            )))
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun isInCollection(releaseId: Int): Boolean =
        collectionDao.isInCollection(releaseId)

    override suspend fun updateAlias(releaseId: Int, titleAlias: String?, artistAlias: String?) =
        releaseDao.updateAlias(releaseId, titleAlias, artistAlias)

    override suspend fun updateMbzReleaseId(releaseId: Int, mbzReleaseId: String?) =
        releaseDao.updateMbzReleaseId(releaseId, mbzReleaseId)

    override suspend fun getLocalRelease(releaseId: Int): com.pollo.scrobbler.domain.model.Release? =
        releaseDao.getRelease(releaseId)?.toDomain()

    override fun getCurrentUsername(): String? = tokenProvider.getUsername()

    override fun logout() = tokenProvider.clearTokens()

    // HMAC-SHA1 OAuth signature
    private fun sign(
        method: String,
        url: String,
        params: Map<String, String>,
        consumerSecret: String,
        tokenSecret: String
    ): String {
        val sortedParams = params.entries
            .sortedWith(compareBy({ it.key }, { it.value }))
            .joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
        val baseString = "${encode(method)}&${encode(url)}&${encode(sortedParams)}"
        val signingKey = "${encode(consumerSecret)}&${encode(tokenSecret)}"
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(signingKey.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        return Base64.encodeToString(mac.doFinal(baseString.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun parseFormEncoded(body: String): Map<String, String> =
        body.split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
}
