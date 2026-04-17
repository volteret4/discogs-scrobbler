package com.pollo.scrobbler.ui.screens.mbzsearch

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pollo.scrobbler.data.remote.dto.MbzReleaseGroupDto
import com.pollo.scrobbler.data.remote.dto.MbzReleaseInGroupDto
import com.pollo.scrobbler.data.remote.dto.MbzTrackDto
import com.pollo.scrobbler.domain.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MbzSearchScreen(
    releaseId: Int,
    viewModel: MbzSearchViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onMbidsSaved: () -> Unit = onBack
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(releaseId) { viewModel.init(releaseId) }

    LaunchedEffect(state.saved) {
        if (state.saved) onMbidsSaved()
    }

    val title = when (val s = state.step) {
        is MbzStep.SearchGroups -> "Buscar en MusicBrainz"
        is MbzStep.BrowseReleases -> s.group.title
        is MbzStep.CompareTracklist -> s.release.title
    }

    val onBackPressed: () -> Unit = when (state.step) {
        is MbzStep.SearchGroups -> onBack
        is MbzStep.BrowseReleases -> { { viewModel.backToGroups() } }
        is MbzStep.CompareTracklist -> { { viewModel.backToReleaseList() } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val step = state.step) {
                is MbzStep.SearchGroups -> SearchGroupsContent(
                    query = state.query,
                    isLoading = state.isLoading,
                    error = state.error,
                    groups = state.releaseGroups,
                    onQueryChange = { viewModel.onQueryChange(it) },
                    onSearch = { viewModel.searchGroups() },
                    onGroupClick = { viewModel.selectGroupAndStore(it) }
                )
                is MbzStep.BrowseReleases -> BrowseReleasesContent(
                    isLoading = state.isLoading,
                    error = state.error,
                    releases = state.releases,
                    existingTrackCount = state.existingTracks.size,
                    onReleaseClick = { viewModel.selectRelease(it) }
                )
                is MbzStep.CompareTracklist -> CompareTracklistContent(
                    release = step.release,
                    mbzTracks = step.mbzTracks,
                    existingTracks = state.existingTracks,
                    matchCount = step.matchCount,
                    isLoading = state.isLoading,
                    onApply = { viewModel.applyMbids() },
                    onBack = { viewModel.backToReleaseList() }
                )
            }
        }
    }
}

private val SYNTAX_HINTS_ROW1 = listOf("artist:", "release:", "releasegroup:", "AND")
private val SYNTAX_HINTS_ROW2 = listOf("type:album", "type:single", "country:", "date:")

@Composable
private fun SearchGroupsContent(
    query: String,
    isLoading: Boolean,
    error: String?,
    groups: List<MbzReleaseGroupDto>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onGroupClick: (MbzReleaseGroupDto) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("artist:\"pink floyd\" AND releasegroup:\"dark side\"") },
                modifier = Modifier.weight(1f),
                minLines = 2,
                maxLines = 4,
                trailingIcon = {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            )
            IconButton(onClick = onSearch, enabled = !isLoading) {
                Icon(Icons.Filled.Search, contentDescription = "Buscar")
            }
        }

        // Syntax hint chips — two rows
        @OptIn(ExperimentalLayoutApi::class)
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            listOf(SYNTAX_HINTS_ROW1, SYNTAX_HINTS_ROW2).forEach { row ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { token ->
                        SuggestionChip(
                            onClick = { onQueryChange(if (query.isBlank()) token else "$query $token") },
                            label = { Text(token, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // Docs link
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Sintaxis Lucene de MusicBrainz ↗",
                style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://musicbrainz.org/doc/Indexed_Search_Syntax")))
                }
            )
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        LazyColumn {
            items(groups) { group ->
                ReleaseGroupRow(group, onClick = { onGroupClick(group) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ReleaseGroupRow(group: MbzReleaseGroupDto, onClick: () -> Unit) {
    val artist = group.artistCredit?.firstOrNull()?.let { it.name ?: it.artist?.name } ?: ""
    val year = group.firstReleaseDate?.take(4) ?: ""
    val type = group.primaryType ?: ""

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(group.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (artist.isNotBlank()) {
                Text(artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (year.isNotBlank()) Text(year, style = MaterialTheme.typography.labelSmall)
            if (type.isNotBlank()) Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BrowseReleasesContent(
    isLoading: Boolean,
    error: String?,
    releases: List<MbzReleaseInGroupDto>,
    existingTrackCount: Int,
    onReleaseClick: (MbzReleaseInGroupDto) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (existingTrackCount > 0) {
            Text(
                "Tu tracklist tiene $existingTrackCount canciones",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        LazyColumn {
            items(releases) { release ->
                ReleaseRow(release, existingTrackCount, onClick = { onReleaseClick(release) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ReleaseRow(release: MbzReleaseInGroupDto, existingCount: Int, onClick: () -> Unit) {
    val tracks = release.totalTracks
    val countColor = if (existingCount > 0 && tracks == existingCount)
        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                release.date?.take(4)?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                release.country?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                release.status?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            val formats = release.media?.mapNotNull { it.format }?.distinct()?.joinToString(" + ")
            if (!formats.isNullOrBlank()) {
                Text(formats, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = "$tracks canciones",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (tracks == existingCount) FontWeight.Bold else FontWeight.Normal),
            color = countColor
        )
    }
}

@Composable
private fun CompareTracklistContent(
    release: MbzReleaseInGroupDto,
    mbzTracks: List<MbzTrackDto>,
    existingTracks: List<Track>,
    matchCount: Int,
    isLoading: Boolean,
    onApply: () -> Unit,
    onBack: () -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val existingTitles = existingTracks.map { it.title.lowercase() }.toSet()
    val total = mbzTracks.size

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                release.date?.take(4)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                release.country?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val matchColor = when {
                matchCount == total && total > 0 -> MaterialTheme.colorScheme.primary
                matchCount > 0 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Text(
                "Coinciden $matchCount/$total canciones",
                style = MaterialTheme.typography.titleSmall,
                color = matchColor
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Otro release")
            }
            Button(
                onClick = onApply,
                enabled = matchCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("Aplicar MBIDs")
            }
        }

        HorizontalDivider()

        // Track list
        LazyColumn {
            items(mbzTracks) { track ->
                val matched = existingTitles.contains(track.title.lowercase()) && track.recording != null
                TrackCompareRow(track, matched)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            }
        }
    }
}

@Composable
private fun TrackCompareRow(track: MbzTrackDto, matched: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = track.number ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp).size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            track.recording?.id?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (matched) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Coincide",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).padding(start = 4.dp)
            )
        }
    }
}
