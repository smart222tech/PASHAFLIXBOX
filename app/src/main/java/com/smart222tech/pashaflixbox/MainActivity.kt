package com.smart222tech.pashaflixbox

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smart222tech.pashaflixbox.data.FavoritesStore
import kotlinx.coroutines.launch

private data class MediaEntry(
    val id: String,
    val title: String,
    val year: Int,
    val kind: String,
    val description: String
)

private val demoCatalog = listOf(
    MediaEntry("sample-film-1", "Beispiel: Der letzte Stern", 2025, "Film",
        "Demo-Titel als Platzhalter. Reale Katalogdaten werden über konfigurierte Anbieter geladen."),
    MediaEntry("sample-series-1", "Beispiel: Stadt ohne Schlaf", 2024, "Serie",
        "Demo-Titel als Platzhalter für die spätere Serien- und Episodenansicht."),
    MediaEntry("sample-film-2", "Beispiel: Reise zum Nordlicht", 2023, "Film",
        "Noch kein echter Filmstream hinterlegt.")
)

private enum class AppSection(val label: String) {
    HOME("Start"), MOVIES("Filme"), SERIES("Serien"), FAVORITES("Favoriten"), SETTINGS("Einstellungen")
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
    val store = remember(context) { FavoritesStore(context.applicationContext) }
    val favorites by store.favorites.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    var section by remember { mutableStateOf(AppSection.HOME) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<MediaEntry?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var sourceUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Bereit · Noch keine Quelle verbunden") }

    val visibleItems = demoCatalog.filter { item ->
        val sectionMatches = when (section) {
            AppSection.MOVIES -> item.kind == "Film"
            AppSection.SERIES -> item.kind == "Serie"
            AppSection.FAVORITES -> favorites.contains(item.id)
            else -> true
        }
        sectionMatches && (query.isBlank() ||
            item.title.contains(query, true) ||
            item.kind.contains(query, true) ||
            item.year.toString().contains(query))
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PASHAFLIXBOX") },
                    actions = {
                        Text(status, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 12.dp))
                    }
                )
            }
        ) { insets ->
            if (selected == null) {
                Column(
                    Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppSection.entries.forEach { item ->
                            TextButton(onClick = { section = item; query = "" }) {
                                Text(if (section == item) "• ${item.label}" else item.label)
                            }
                        }
                    }
                    if (section == AppSection.SETTINGS) {
                        SettingsScreen(
                            onAddSource = { showSourceDialog = true },
                            status = status
                        )
                    } else {
                        Text(
                            when (section) {
                                AppSection.MOVIES -> "Filme entdecken"
                                AppSection.SERIES -> "Serien entdecken"
                                AppSection.FAVORITES -> "Deine Merkliste"
                                else -> "Filme und Serien an einem Ort"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Titel, Genre oder Jahr suchen") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(visibleItems, key = { it.id }) { item ->
                                MediaCard(
                                    item = item,
                                    favorite = favorites.contains(item.id),
                                    onOpen = { selected = item },
                                    onFavorite = {
                                        scope.launch {
                                            store.setFavorite(item.id, !favorites.contains(item.id))
                                        }
                                    }
                                )
                            }
                        }
                        if (visibleItems.isEmpty()) {
                            Text("Keine Einträge vorhanden.", modifier = Modifier.padding(16.dp))
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
                    onPlay = { showSourceDialog = true }
                )
            }
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Eigene / autorisierte Videoquelle") },
            text = {
                Column {
                    Text("Gib eine direkte HTTPS-Adresse zu einem Stream ein, den du besitzt oder nutzen darfst. Unterstützte Formate hängen vom Stream ab (z. B. MP4, HLS oder DASH).")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("HTTPS-Stream-URL") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = runCatching { Uri.parse(sourceUrl.trim()) }.getOrNull()
                    if (uri?.scheme == "https" && !uri.host.isNullOrBlank()) {
                        showSourceDialog = false
                        PlayerActivityStarter.open(context, uri.toString(), selected?.title ?: "PASHAFLIXBOX")
                    } else {
                        status = "Bitte eine gültige HTTPS-Adresse eingeben"
                    }
                }) { Text("Abspielen") }
            },
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun MediaCard(
    item: MediaEntry,
    favorite: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text("${item.kind} · ${item.year}", style = MaterialTheme.typography.bodyMedium)
                Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen"
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
        Text("${item.kind} · ${item.year}")
        Text(item.description)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Quelle abspielen")
            }
            OutlinedButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (favorite) "Gemerkt" else "Merken")
            }
        }
        Text(
            "Die angezeigten Titel sind derzeit Demo-Platzhalter. Reale Kataloge und Anbieter werden schrittweise ergänzt.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SettingsScreen(onAddSource: () -> Unit, status: String) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Quellen und Wiedergabe", style = MaterialTheme.typography.titleLarge)
        Text("Verbinde später deine eigenen Server oder andere Quellen, für die du eine Nutzungserlaubnis hast.")
        Button(onClick = onAddSource) { Text("Teststream öffnen") }
        Text("Status: $status", style = MaterialTheme.typography.bodySmall)
        Text("PASHAFLIXBOX · Version 0.1.0", style = MaterialTheme.typography.labelSmall)
    }
}
