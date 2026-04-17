package com.pollo.scrobbler.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pollo.scrobbler.ui.screens.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // Discogs section
            Text("Discogs", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (state.isDiscogsAuthenticated) {
                Text("Connected as: ${state.discogsUsername}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.logoutDiscogs() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect Discogs")
                }
            } else {
                Button(onClick = { viewModel.loginWithDiscogs() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect Discogs")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            // Preferences section
            Text("Preferencias", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.showTechInfo,
                    onCheckedChange = { viewModel.setShowTechInfo(it) }
                )
                Text("Toda la info (MBIDs, MusicBrainz)", style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            // Last.fm section
            Text("Last.fm", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (state.isLastFmAuthenticated) {
                Text("Connected as: ${state.lastFmUsername}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.logoutLastFm() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect Last.fm")
                }
            } else {
                Button(onClick = { viewModel.loginWithLastFm() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect Last.fm")
                }
            }
        }
    }
}
