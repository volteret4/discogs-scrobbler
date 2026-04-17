package com.pollo.scrobbler

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pollo.scrobbler.ui.navigation.AppNavigation
import com.pollo.scrobbler.ui.screens.auth.AuthViewModel
import com.pollo.scrobbler.ui.theme.ScrobblerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLink(intent)
        setContent {
            ScrobblerTheme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        when (uri.host) {
            "oauth-callback" -> {
                val token = uri.getQueryParameter("oauth_token") ?: return
                val verifier = uri.getQueryParameter("oauth_verifier") ?: return
                authViewModel.handleDiscogsCallback(token, verifier)
            }
            "lastfm-callback" -> {
                val token = uri.getQueryParameter("token") ?: return
                authViewModel.handleLastFmCallback(token)
            }
        }
    }
}
