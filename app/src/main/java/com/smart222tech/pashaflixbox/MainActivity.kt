package com.smart222tech.pashaflixbox

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smart222tech.pashaflixbox.data.AppStore
import com.smart222tech.pashaflixbox.data.M3uPlaylistRepository
import com.smart222tech.pashaflixbox.data.PlaylistItem
import kotlinx.coroutines.launch

private data class MediaEntry(
    val id: String,
    val title: String,
    val kind: String,
    val description: String,
    val streamUrl: String? = null
)

private enum class AppSection(val label: String) {
    HOME("Start"), MOVIES("Filme"), SERIES("Serien"),
    FAVORITES("Merkliste"), HISTORY("Verlauf"), SETTINGS("Einstellungen")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PashaFlixBoxApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PashaFlixBoxApp() {
    val context = LocalContext.current
    val store = remember(context) { AppStore(context) }
    val favorites by store.favorites.collectAsState(initial = emptySet())
    val history by store.history.collectAsState(initial = emptyList())
    val savedPlaylistUrl by store.playlistUrl.collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    var playlistItems by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var section by remember { mutableStateOf(AppSection.HOME) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<MediaEntry?>(null) }
    var playlistUrl by remember { mutableStateOf("") }
    var directUrl by remember { mutableStateOf("") }
    var showDirectDialog by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Noch keine Playlist geladen") }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(savedPlaylistUrl) {
        if (savedPlaylistUrl.isNotBlank() && playlistUrl.isBlank()) playlistUrl = savedPlaylistUrl
    }

    val catalogue = remember(playlistItems) {
        playlistItems.map { item ->
            MediaEntry(
                id = item.id,
                title = item.title,
                kind = if (item.isSeries) "Serie" else "Film",
                description = listOf(item.group, "Playlist").filter(String::isNotBlank).joinToString(" · "),
                streamUrl = item.streamUrl
            )
        }
    }

    val visibleItems = when (section) {
        AppSection.FAVORITES -> catalogue.filter { favorites.contains(it.id) }
        AppSection.HISTORY -> history.mapNotNull { id -> catalogue.find { it.id == id } }
        AppSection.MOVIES -> catalogue.filter { it.kind == "Film" }
        AppSection.SERIES -> catalogue.filter { it.kind == "Serie" }
        else -> catalogue
    }.filter { item ->
        query.isBlank() || item.title.contains(query, true) ||
            item.description.contains(query, true)
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PASHAFLIXBOX") },
                    actions = {
                        if (loading) CircularProgressIndicator(
                            modifier = Modifier.size(22.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                        IconButton(onClick = { section = AppSection.SETTINGS }) {
                            Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                        }
                    }
                )
            }
        ) { insets ->
            if (selected == null) {
                Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp)) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(AppSection.entries.filter { it != AppSection.SETTINGS }) { item ->
                            FilterChip(
                                selected = section == item,
                                onClick = { section = item; query = "" },
                                label = { Text(item.label) }
                            )
                        }
                    }
                    if (section == AppSection.SETTINGS) {
                        SettingsScreen(
                            playlistUrl = playlistUrl,
                            onPlaylistUrlChange = { playlistUrl = it },
                            onLoad = {
                                val parsed = Uri.parse(playlistUrl.trim())
                                if (parsed.scheme !in listOf("http", "https") || parsed.host.isNullOrBlank()) {
                                    status = "Bitte eine gültige HTTP(S)-Playlist-Adresse eingeben"
                                } else {
                                    loading = true
                                    status = "Playlist wird geladen …"
                                    scope.launch {
                                        try {
                                            val loaded = M3uPlaylistRepository.load(playlistUrl.trim())
                                            playlistItems = loaded
                                            store.savePlaylistUrl(playlistUrl)
                                            status = loaded.size.toString() + " Einträge geladen"
                                            section = AppSection.HOME
                                        } catch (error: Exception) {
                                            status = error.message ?: "Playlist konnte nicht geladen werden"
                                        } finally {
                                            loading = false
                                        }
                                    }
                                }
                            },
                            onDirectPlay = { showDirectDialog = true },
                            onClearHistory = { scope.launch { store.clearHistory() } },
                            status = status,
                            loading = loading
                        )
                    } else {
                        Text(
                            when (section) {
                                AppSection.MOVIES -> "Filme"
                                AppSection.SERIES -> "Serien"
                                AppSection.FAVORITES -> "Deine Merkliste"
                                AppSection.HISTORY -> "Zuletzt abgespielt"
                                else -> "Deine Medien an einem Ort"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Titel oder Kategorie suchen") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                        Spacer(Modifier.height(10.dp))
                        if (catalogue.isEmpty()) {
                            EmptyCatalogue(onSettings = { section = AppSection.SETTINGS })
                        } else if (visibleItems.isEmpty()) {
                            Text("Keine passenden Einträge gefunden.", modifier = Modifier.padding(16.dp))
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(9.dp),
                                contentPadding = PaddingValues(bottom = 20.dp)
                            ) {
                                items(visibleItems, key = { it.id }) { item ->
                                    MediaCard(
                                        item = item,
                                        favorite = favorites.contains(item.id),
                                        onOpen = { selected = item },
                                        onFavorite = {
                                            scope.launch { store.setFavorite(item.id, !favorites.contains(item.id)) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                DetailScreen(
                    item = selected!!,
                    favorite = favorites.contains(selected!!.id),
                    onBack = { selected = null },
                    onFavorite = {
                        val id = selected!!.id
                        scope.launch { store.setFavorite(id, !favorites.contains(id)) }
                    },
                    onPlay = {
                        val item = selected!!
                        if (!item.streamUrl.isNullOrBlank()) {
                            scope.launch { store.recordPlayback(item.id) }
                            PlayerActivityStarter.open(context, item.streamUrl, item.title)
                        } else showDirectDialog = true
                    }
                )
            }
        }
    }

    if (showDirectDialog) {
        AlertDialog(
            onDismissRequest = { showDirectDialog = false },
            title = { Text("Direkte Videoquelle") },
            text = {
                Column {
                    Text("HTTPS-Adresse zu einer Videodatei oder einem HLS-/DASH-Stream, den du nutzen darfst.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = directUrl,
                        onValueChange = { directUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Video-URL") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = runCatching { Uri.parse(directUrl.trim()) }.getOrNull()
                    if (uri?.scheme == "https" && !uri.host.isNullOrBlank()) {
                        showDirectDialog = false
                        selected?.let { item -> scope.launch { store.recordPlayback(item.id) } }
                        PlayerActivityStarter.open(context, uri.toString(), selected?.title ?: "PASHAFLIXBOX")
                    } else status = "Bitte eine gültige HTTPS-Video-URL eingeben"
                }) { Text("Abspielen") }
            },
            dismissButton = { TextButton(onClick = { showDirectDialog = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun MediaCard(item: MediaEntry, favorite: Boolean, onOpen: () -> Unit, onFavorite: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).focusable(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(item.kind, style = MaterialTheme.typography.bodySmall)
                if (item.description.isNotBlank()) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) "Aus Merkliste entfernen" else "Zur Merkliste hinzufügen"
                )
            }
        }
    }
}

@Composable
private fun DetailScreen(
    item: MediaEntry,
    favorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onPlay: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Zurück") }
        Text(item.title, style = MaterialTheme.typography.headlineMedium)
        Text(listOf(item.kind, item.description).filter(String::isNotBlank).joinToString(" · "))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Abspielen")
            }
            OutlinedButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (favorite) "Gemerkt" else "Merken")
            }
        }
        Text("Wiedergabe über Media3 für kompatible HTTP(S)-Videoquellen.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyCatalogue(onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Dein Katalog ist noch leer", style = MaterialTheme.typography.titleLarge)
        Text("Verbinde eine eigene oder autorisierte M3U-Playlist, um deine Medien hier anzuzeigen.")
        Button(onClick = onSettings) { Text("Playlist verbinden") }
    }
}

@Composable
private fun SettingsScreen(
    playlistUrl: String,
    onPlaylistUrlChange: (String) -> Unit,
    onLoad: () -> Unit,
    onDirectPlay: () -> Unit,
    onClearHistory: () -> Unit,
    status: String,
    loading: Boolean
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Deine Medienquellen", style = MaterialTheme.typography.titleLarge)
        Text("Füge eine M3U-Playlist von deinem eigenen Server oder einem Anbieter hinzu, dessen Nutzung du autorisiert hast.")
        OutlinedTextField(
            value = playlistUrl,
            onValueChange = onPlaylistUrlChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("M3U-Playlist-URL") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Button(onClick = onLoad, enabled = !loading && playlistUrl.isNotBlank()) {
            Text(if (loading) "Lädt …" else "Playlist laden")
        }
        OutlinedButton(onClick = onDirectPlay) { Text("Direkte Video-URL testen") }
        OutlinedButton(onClick = onClearHistory) { Text("Wiedergabeverlauf löschen") }
        Text("Status: " + status, style = MaterialTheme.typography.bodySmall)
        Text("Unterstützt: M3U mit HTTP(S)-Streams und Media3-kompatible Videoquellen.",
            style = MaterialTheme.typography.bodySmall)
        Text("PASHAFLIXBOX · Version 0.1.0", style = MaterialTheme.typography.labelSmall)
    }
}
