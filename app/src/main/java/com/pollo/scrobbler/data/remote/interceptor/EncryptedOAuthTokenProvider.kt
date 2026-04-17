package com.pollo.scrobbler.data.remote.interceptor

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedOAuthTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : OAuthTokenProvider {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "discogs_oauth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getAccessToken(): String? = prefs.getString("access_token", null)
    override fun getAccessTokenSecret(): String? = prefs.getString("access_token_secret", null)
    override fun getPendingRequestTokenSecret(): String? = prefs.getString("pending_token_secret", null)
    override fun getUsername(): String? = prefs.getString("username", null)

    override fun saveAccessTokens(token: String, secret: String) {
        prefs.edit().putString("access_token", token).putString("access_token_secret", secret).apply()
    }

    override fun savePendingRequestToken(token: String, secret: String) {
        prefs.edit().putString("pending_token", token).putString("pending_token_secret", secret).apply()
    }

    override fun saveUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

    override fun clearTokens() {
        prefs.edit().clear().apply()
    }
}
