package com.pollo.scrobbler.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isDiscogsAuthenticated, state.pendingLastFmStep) {
        if (state.isDiscogsAuthenticated && !state.pendingLastFmStep) onAuthenticated()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Scrobbler",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Collector companion for vinyl & CD",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
        } else if (state.pendingLastFmStep) {
            LastFmStep(
                onLogin = { viewModel.loginWithLastFm() },
                onSkip = { viewModel.skipLastFm() },
                error = state.error
            )
        } else {
            DiscogsStep(
                onLogin = { viewModel.loginWithDiscogs() },
                error = state.error
            )
        }
    }
}

@Composable
private fun DiscogsStep(onLogin: () -> Unit, error: String?) {
    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
        Text("Connect with Discogs")
    }
    error?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LastFmStep(onLogin: () -> Unit, onSkip: () -> Unit, error: String?) {
    Text(
        text = "Connect Last.fm",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Link your Last.fm account to scrobble your plays",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
        Text("Connect with Last.fm")
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
        Text("Skip for now")
    }
    error?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}
