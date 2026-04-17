package com.pollo.scrobbler.data.remote.interceptor

interface OAuthTokenProvider {
    fun getAccessToken(): String?
    fun getAccessTokenSecret(): String?
    fun saveAccessTokens(token: String, secret: String)
    fun savePendingRequestToken(token: String, secret: String)
    fun getPendingRequestTokenSecret(): String?
    fun clearTokens()
    fun getUsername(): String?
    fun saveUsername(username: String)
}
