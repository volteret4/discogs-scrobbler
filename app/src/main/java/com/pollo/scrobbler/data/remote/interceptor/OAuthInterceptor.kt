package com.pollo.scrobbler.data.remote.interceptor

import android.util.Base64
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLEncoder
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class OAuthInterceptor(
    private val consumerKey: String,
    private val consumerSecret: String,
    private val tokenProvider: OAuthTokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val accessToken = tokenProvider.getAccessToken()
        val accessTokenSecret = tokenProvider.getAccessTokenSecret()

        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val nonce = SecureRandom().nextLong().and(Long.MAX_VALUE).toString()

        val oauthParams = mutableMapOf(
            "oauth_consumer_key" to consumerKey,
            "oauth_nonce" to nonce,
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to timestamp,
            "oauth_version" to "1.0"
        )
        if (accessToken != null) oauthParams["oauth_token"] = accessToken

        // Include URL query params in signature
        val allParams = oauthParams.toMutableMap()
        val url = original.url
        for (i in 0 until url.querySize) {
            allParams[url.queryParameterName(i)] = url.queryParameterValue(i) ?: ""
        }

        val baseUrl = "${url.scheme}://${url.host}${url.encodedPath}"
        val baseString = buildBaseString(original.method, baseUrl, allParams)
        val signingKey = "${encode(consumerSecret)}&${encode(accessTokenSecret ?: "")}"
        oauthParams["oauth_signature"] = hmacSha1(signingKey, baseString)

        val authHeader = "OAuth " + oauthParams.entries
            .joinToString(", ") { (k, v) -> """$k="${encode(v)}"""" }

        return chain.proceed(
            original.newBuilder().header("Authorization", authHeader).build()
        )
    }

    private fun buildBaseString(method: String, url: String, params: Map<String, String>): String {
        val normalized = params.entries
            .sortedWith(compareBy({ it.key }, { it.value }))
            .joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
        return "${encode(method)}&${encode(url)}&${encode(normalized)}"
    }

    private fun hmacSha1(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        return Base64.encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
