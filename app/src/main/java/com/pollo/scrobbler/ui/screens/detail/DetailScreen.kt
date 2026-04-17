package com.pollo.scrobbler.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pollo.scrobbler.domain.model.Release
import com.pollo.scrobbler.domain.model.ReleaseContributor
import com.pollo.scrobbler.domain.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    releaseId: Int,
    viewModel: DetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onMbzSearch: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(releaseId) { viewModel.loadRelease(releaseId) }

    LaunchedEffect(state.scrobbleResult) {
        state.scrobbleResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearScrobbleResult()
        }
    }

    val currentRelease = state.release
    if (state.showAliasDialog && currentRelease != null) {
        AliasDialog(
            currentTitle = currentRelease.titleAlias ?: currentRelease.title,
            currentArtist = currentRelease.artistAlias ?: currentRelease.artist,
            isSaving = state.isSavingAlias,
            onDismiss = { viewModel.closeAliasDialog() },
            onSave = { title, artist -> viewModel.saveAlias(title, artist) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.release?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.release != null) {
                        IconButton(onClick = { viewModel.openAliasDialog() }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar alias")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    text = "Error: ${state.error}",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
                state.release != null -> ReleaseDetail(
                    release = state.release!!,
                    isScrobbling = state.isScrobbling,
                    isInCollection = state.isInCollection,
                    isAddingToCollection = state.isAddingToCollection,
                    showTechInfo = state.showTechInfo,
                    onScrobble = { viewModel.scrobble() },
                    onAddToCollection = { viewModel.addToCollection() },
                    onMbzSearch = onMbzSearch
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReleaseDetail(
    release: Release,
    isScrobbling: Boolean,
    isInCollection: Boolean,
    isAddingToCollection: Boolean,
    showTechInfo: Boolean,
    onScrobble: () -> Unit,
    onAddToCollection: () -> Unit,
    onMbzSearch: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // Cover art
        AsyncImage(
            model = release.coverImageUrl,
            contentDescription = release.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )

        // Metadata + actions
        Column(modifier = Modifier.padding(16.dp)) {
            Text(release.title, style = MaterialTheme.typography.headlineMedium)
            Text(
                release.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val dateStr = release.released ?: release.year?.toString()
                dateStr?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                release.country?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                release.format?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }

            release.label?.let { label ->
                Spacer(modifier = Modifier.height(4.dp))
                if (release.labelId != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.discogs.com/label/${release.labelId}")))
                        }
                    )
                } else {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (release.genres.isNotEmpty() || release.styles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    release.genres.forEach { genre ->
                        SuggestionChip(onClick = {}, label = { Text(genre, style = MaterialTheme.typography.labelSmall) })
                    }
                    release.styles.forEach { style ->
                        SuggestionChip(onClick = {}, label = { Text(style, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onAddToCollection,
                enabled = !isInCollection && !isAddingToCollection,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAddingToCollection) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text(if (isInCollection) "En colección" else if (isAddingToCollection) "Añadiendo…" else "Añadir a colección")
            }

            if (release.tracklist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onScrobble,
                    enabled = !isScrobbling,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isScrobbling) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text(if (isScrobbling) "Scrobbleando…" else "Scrobble (${release.tracklist.size} pistas)")
                }

                if (showTechInfo) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val mbidCount = release.tracklist.count { it.mbid != null }
                    val total = release.tracklist.size
                    val mbidColor = when {
                        mbidCount == total && total > 0 -> MaterialTheme.colorScheme.primary
                        mbidCount > 0                  -> MaterialTheme.colorScheme.tertiary
                        else                           -> MaterialTheme.colorScheme.error
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MBIDs: $mbidCount/$total pistas",
                            style = MaterialTheme.typography.labelSmall,
                            color = mbidColor
                        )
                        Text(
                            text = "Buscar en MusicBrainz →",
                            style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onMbzSearch() }
                        )
                    }
                }
            }
        }

        // YouTube thumbnail — below buttons, click opens YouTube app/browser
        if (release.videoId != null) {
            HorizontalDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${release.videoId}"))
                        )
                    }
            ) {
                AsyncImage(
                    model = "https://img.youtube.com/vi/${release.videoId}/hqdefault.jpg",
                    contentDescription = "Vídeo de YouTube",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = "Reproducir en YouTube",
                    tint = Color.White,
                    modifier = Modifier.size(72.dp).align(Alignment.Center)
                )
            }
        }

        HorizontalDivider()

        // Credits
        if (release.contributors.isNotEmpty()) {
            val grouped = release.contributors.groupBy { it.role }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Créditos", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                grouped.forEach { (role, people) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(
                            "$role: ",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(0.35f)
                        )
                        Column(modifier = Modifier.weight(0.65f)) {
                            people.forEach { contributor -> ContributorLink(contributor, context) }
                        }
                    }
                }
            }
            HorizontalDivider()
        }

        // Notes
        if (!release.notes.isNullOrBlank()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Notas", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(release.notes, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
        }

        // Tracklist
        if (release.tracklist.isNotEmpty()) {
            Text(
                "Tracklist",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            release.tracklist.forEach { track ->
                TrackRow(track)
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun ContributorLink(contributor: ReleaseContributor, context: android.content.Context) {
    if (contributor.discogsArtistId != null) {
        Text(
            text = contributor.name,
            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.discogs.com/artist/${contributor.discogsArtistId}")))
            }
        )
    } else {
        Text(contributor.name, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AliasDialog(
    currentTitle: String,
    currentArtist: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(currentTitle) }
    var artist by rememberSaveable { mutableStateOf(currentArtist) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Alias para búsqueda") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Estos nombres se usarán en MusicBrainz y Last.fm. Deja vacío para usar el nombre de Discogs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista") },
                    singleLine = true,
                    enabled = !isSaving
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, artist) }, enabled = !isSaving) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
        }
    )
}

private fun sanitizeForLucene(s: String): String =
    s.replace(Regex("[\"\\\\]"), "")
     .replace(Regex("[''\u2019\u2018]"), "")
     .replace(Regex("[+\\-!(){}\\[\\]^~*?:|&]"), " ")
     .replace(Regex("\\s+"), " ")
     .trim()

@Composable
private fun TrackRow(track: Track) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = track.position,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(text = track.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        track.duration?.let {
            Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
