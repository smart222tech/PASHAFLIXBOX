package com.smart222tech.pashaflixbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smart222tech.pashaflixbox.data.AppStore
import com.smart222tech.pashaflixbox.data.CatalogEpisode
import com.smart222tech.pashaflixbox.data.CatalogMedia
import com.smart222tech.pashaflixbox.data.StremioCatalogRepository
import com.smart222tech.pashaflixbox.data.StreamOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Shelf(val title: String, val type: String?) {
    HOME("Start", null), MOVIES("Filme", "movie"), SERIES("Serien", "series"),
    FAVORITES("Merkliste", null), HISTORY("Verlauf", null), SETTINGS("Einstellungen", null)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PashaFlixApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PashaFlixApp() {
    val context = LocalContext.current
    val repository = remember { StremioCatalogRepository(context) }
    val store = remember { AppStore(context) }
    val favorites by store.favorites.collectAsState(initial = emptySet())
    val history by store.history.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var shelf by remember { mutableStateOf(Shelf.HOME) }
    var search by remember { mutableStateOf("") }
    var movies by remember { mutableStateOf<List<CatalogMedia>>(emptyList()) }
    var series by remember { mutableStateOf<List<CatalogMedia>>(emptyList()) }
    var selected by remember { mutableStateOf<CatalogMedia?>(null) }
    var episodes by remember { mutableStateOf<List<CatalogEpisode>>(emptyList()) }
    var streams by remember { mutableStateOf<List<StreamOption>>(emptyList()) }
    var selectedEpisode by remember { mutableStateOf<CatalogEpisode?>(null) }
    var addonUrl by remember { mutableStateOf(repository.addonUrl()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Каталог готовится к загрузке") }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(shelf, search) {
        if (shelf == Shelf.SETTINGS || shelf == Shelf.FAVORITES || shelf == Shelf.HISTORY || selected != null) return@LaunchedEffect
        delay(if (search.isBlank()) 100 else 450)
        busy = true
        try {
            when (shelf) {
                Shelf.HOME -> {
                    val m = repository.loadCatalog("movie", search)
                    val s = repository.loadCatalog("series", search)
                    movies = m
                    series = s
                    status = "Каталог загружен: ${m.size} фильмов · ${s.size} сериалов"
                }
                Shelf.MOVIES -> {
                    movies = repository.loadCatalog("movie", search)
                    status = "${movies.size} фильмов"
                }
                Shelf.SERIES -> {
                    series = repository.loadCatalog("series", search)
                    status = "${series.size} сериалов"
                }
                else -> Unit
            }
        } catch (error: Exception) {
            status = error.message ?: "Не удалось загрузить каталог"
        } finally {
            busy = false
        }
    }

    LaunchedEffect(selected?.id) {
        val item = selected ?: return@LaunchedEffect
        episodes = emptyList()
        streams = emptyList()
        selectedEpisode = null
        busy = true
        try {
            val (full, eps) = repository.loadMeta(item)
            selected = full
            episodes = eps
            status = "Карточка загружена"
        } catch (error: Exception) {
            status = error.message ?: "Не удалось загрузить описание"
        } finally {
            busy = false
        }
    }

    val allItems = (movies + series).distinctBy { it.type + ":" + it.id }
    val visibleItems = when (shelf) {
        Shelf.MOVIES -> movies
        Shelf.SERIES -> series
        Shelf.FAVORITES -> allItems.filter { favorites.contains(it.id) }
        Shelf.HISTORY -> allItems.sortedBy { history.indexOf(it.id).let { index -> if (index < 0) Int.MAX_VALUE else index } }
            .filter { history.contains(it.id) }
        else -> allItems
    }.filter { search.isBlank() || it.title.contains(search, true) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PASHAFLIX") },
                    actions = {
                        if (busy) CircularProgressIndicator(Modifier.size(22.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                        IconButton(onClick = { shelf = Shelf.SETTINGS; selected = null }) {
                            Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf(Shelf.HOME, Shelf.MOVIES, Shelf.SERIES, Shelf.FAVORITES).forEach { item ->
                        NavigationBarItem(
                            selected = shelf == item && selected == null,
                            onClick = { shelf = item; selected = null; search = "" },
                            icon = { Text(when (item) { Shelf.HOME -> "⌂"; Shelf.MOVIES -> "▣"; Shelf.SERIES -> "▤"; else -> "♥" }) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        ) { padding ->
            when {
                shelf == Shelf.SETTINGS -> SettingsPage(
                    modifier = Modifier.padding(padding),
                    addonUrl = addonUrl,
                    onAddonChange = { addonUrl = it },
                    onSave = {
                        repository.saveAddonUrl(addonUrl)
                        status = "Stremio-Addon gespeichert. Suche jetzt nach einem Film."
                        shelf = Shelf.HOME
                        search = ""
                    },
                    status = status
                )
                selected != null -> DetailPage(
                    modifier = Modifier.padding(padding),
                    media = selected!!,
                    episodes = episodes,
                    streams = streams,
                    selectedEpisode = selectedEpisode,
                    busy = busy,
                    status = status,
                    onBack = { selected = null; streams = emptyList() },
                    onFavorite = { scope.launch { store.setFavorite(selected!!.id, !favorites.contains(selected!!.id)) } },
                    isFavorite = favorites.contains(selected!!.id),
                    onEpisode = { episode ->
                        selectedEpisode = episode
                        streams = emptyList()
                        scope.launch {
                            busy = true
                            try { streams = repository.findStreams(selected!!, episode) }
                            catch (error: Exception) { status = error.message ?: "Keine Quellen gefunden" }
                            finally { busy = false }
                        }
                    },
                    onFindStreams = {
                        scope.launch {
                            busy = true
                            try { streams = repository.findStreams(selected!!, selectedEpisode) }
                            catch (error: Exception) { status = error.message ?: "Keine Quellen gefunden" }
                            finally { busy = false }
                        }
                    },
                    onPlay = { stream ->
                        scope.launch { store.recordPlayback(selected!!.id) }
                        PlayerActivityStarter.open(context, stream.url, selected!!.title)
                    }
                )
                else -> CatalogPage(
                    modifier = Modifier.padding(padding),
                    shelf = shelf,
                    search = search,
                    onSearch = { search = it },
                    items = visibleItems,
                    favorites = favorites,
                    busy = busy,
                    status = status,
                    onSelect = { selected = it },
                    onFavorite = { item -> scope.launch { store.setFavorite(item.id, !favorites.contains(item.id)) } },
                    onOpenSettings = { shelf = Shelf.SETTINGS }
                )
            }
        }
    }
}

@Composable
private fun CatalogPage(
    modifier: Modifier,
    shelf: Shelf,
    search: String,
    onSearch: (String) -> Unit,
    items: List<CatalogMedia>,
    favorites: Set<String>,
    busy: Boolean,
    status: String,
    onSelect: (CatalogMedia) -> Unit,
    onFavorite: (CatalogMedia) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true,
            label = { Text("Film oder Serie suchen") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        Text(status, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 7.dp))
        if (items.isEmpty() && !busy) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (search.isBlank()) "Katalog ist leer" else "Keine Treffer")
                Text("Der öffentliche Metadatenkatalog liefert Filminformationen. Zum Abspielen wird ein kompatibler Stremio-Addon benötigt.",
                    style = MaterialTheme.typography.bodySmall)
                Button(onClick = onOpenSettings) { Text("Quellen einrichten") }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(items, key = { it.type + it.id }) { item ->
                    CatalogCard(
                        media = item,
                        favorite = favorites.contains(item.id),
                        onClick = { onSelect(item) },
                        onFavorite = { onFavorite(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(media: CatalogMedia, favorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick).focusable(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = media.poster,
                contentDescription = media.title,
                modifier = Modifier.size(width = 88.dp, height = 126.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(media.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(media.year, media.rating?.let { "★ $it" }, media.type).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall)
                Text(media.description.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Merken")
            }
        }
    }
}

@Composable
private fun DetailPage(
    modifier: Modifier,
    media: CatalogMedia,
    episodes: List<CatalogEpisode>,
    streams: List<StreamOption>,
    selectedEpisode: CatalogEpisode?,
    busy: Boolean,
    status: String,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean,
    onEpisode: (CatalogEpisode) -> Unit,
    onFindStreams: () -> Unit,
    onPlay: (StreamOption) -> Unit
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("← Zurück zum Katalog") }
        AsyncImage(model = media.poster, contentDescription = media.title, modifier = Modifier.size(width = 150.dp, height = 220.dp))
        Text(media.title, style = MaterialTheme.typography.headlineSmall)
        Text(listOfNotNull(media.year, media.rating?.let { "★ $it" }, media.genres.joinToString(", ").takeIf { it.isNotBlank() }).joinToString(" · "))
        Text(media.description.orEmpty())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onFavorite) { Text(if (isFavorite) "♥ Gespeichert" else "♡ Merken") }
            Button(onClick = onFindStreams, enabled = !busy) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(" Quellen suchen")
            }
        }
        if (media.type == "series" && episodes.isNotEmpty()) {
            Text("Folgen", style = MaterialTheme.typography.titleLarge)
            episodes.forEach { episode ->
                OutlinedButton(onClick = { onEpisode(episode) }, modifier = Modifier.fillMaxWidth()) {
                    Text("S${episode.season ?: "?"} · E${episode.episode ?: "?"} — ${episode.title}")
                }
            }
        }
        if (busy) CircularProgressIndicator()
        Text(status, style = MaterialTheme.typography.bodySmall)
        if (streams.isNotEmpty()) {
            Text("Verfügbare Quellen", style = MaterialTheme.typography.titleLarge)
            streams.forEach { stream ->
                Card(Modifier.fillMaxWidth().clickable { onPlay(stream) }.focusable()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stream.title)
                            Text(listOfNotNull(stream.provider, stream.quality, stream.language).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "Abspielen")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    modifier: Modifier,
    addonUrl: String,
    onAddonChange: (String) -> Unit,
    onSave: () -> Unit,
    status: String
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Quellen & Einstellungen", style = MaterialTheme.typography.headlineSmall)
        Text("Der Filmkatalog wird automatisch aus Cinemeta geladen. Um Videostreams zu finden, verbinde einmal einen kompatiblen Stremio-Addon-Manifest-Link.")
        OutlinedTextField(
            value = addonUrl,
            onValueChange = onAddonChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Stremio Add-on Manifest-URL") },
            placeholder = { Text("https://…/manifest.json") },
            singleLine = true
        )
        Button(onClick = onSave) { Text("Addon speichern") }
        Text("Hinweis: Die allgemeine AIOStreams-Installationsadresse meldet eine erforderliche Konfiguration. Sie kann nicht als fertiger Streamanbieter verwendet werden, solange keine konfigurierte persönliche Manifest-URL vorliegt.",
            style = MaterialTheme.typography.bodySmall)
        Text(status, style = MaterialTheme.typography.bodySmall)
    }
}
