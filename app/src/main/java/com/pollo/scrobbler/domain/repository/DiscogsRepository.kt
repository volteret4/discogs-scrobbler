package com.pollo.scrobbler.domain.repository

import com.pollo.scrobbler.domain.model.CollectionItem
import com.pollo.scrobbler.domain.model.Release
import kotlinx.coroutines.flow.Flow

interface DiscogsRepository {
    suspend fun searchByBarcode(barcode: String): List<Release>
    suspend fun searchByQuery(query: String): List<Release>
    suspend fun getReleaseDetail(releaseId: Int): Release
    fun getCollection(): Flow<List<CollectionItem>>
    suspend fun syncCollection()
    suspend fun isAuthenticated(): Boolean
    suspend fun getRequestTokenUrl(): String
    suspend fun handleOAuthCallback(oauthToken: String, oauthVerifier: String): Boolean
    fun getCurrentUsername(): String?
    suspend fun updateCachedTracklist(releaseId: Int, tracks: List<com.pollo.scrobbler.domain.model.Track>)
    suspend fun addToCollection(releaseId: Int): Boolean
    suspend fun isInCollection(releaseId: Int): Boolean
    suspend fun updateAlias(releaseId: Int, titleAlias: String?, artistAlias: String?)
    suspend fun updateMbzReleaseId(releaseId: Int, mbzReleaseId: String?)
    suspend fun getLocalRelease(releaseId: Int): com.pollo.scrobbler.domain.model.Release?
    fun logout()
}
