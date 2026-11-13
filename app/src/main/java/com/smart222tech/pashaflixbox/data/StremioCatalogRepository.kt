package com.smart222tech.pashaflixbox.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CatalogMedia(
    val id: String,
    val type: String,
    val title: String,
    val poster: String? = null,
    val background: String? = null,
    val description: String? = null,
    val year: String? = null,
    val rating: String? = null,
    val genres: List<String> = emptyList()
)

data class CatalogEpisode(
    val id: String,
    val title: String,
    val season: Int? = null,
    val episode: Int? = null,
    val released: String? = null
)

data class StreamOption(
    val provider: String,
    val title: String,
    val url: String,
    val quality: String? = null,
    val language: String? = null
)

class StremioCatalogRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("pashaflix_catalog", Context.MODE_PRIVATE)

    fun addonUrl(): String = prefs.getString(KEY_ADDON, "").orEmpty()

    fun saveAddonUrl(value: String) {
        prefs.edit().putString(KEY_ADDON, value.trim()).apply()
    }

    suspend fun loadCatalog(type: String, search: String = "", skip: Int = 0): List<CatalogMedia> =
        withContext(Dispatchers.IO) {
            val manifest = getJson("$CINEMETA/manifest.json")
            val catalogs = manifest.optJSONArray("catalogs") ?: JSONArray()
            val selected = (0 until catalogs.length()).map { catalogs.optJSONObject(it) }
                .filterNotNull().firstOrNull { it.optString("type") == type }
                ?: throw IllegalStateException("Der Katalog für $type ist beim Metadatenanbieter nicht verfügbar.")
            val catalogId = selected.optString("id")
            val endpoint = buildString {
                append("$CINEMETA/catalog/$type/${encodePath(catalogId)}")
                if (search.isNotBlank()) append("/search=${encodePath(search.trim())}")
                if (skip > 0) append("/skip=$skip")
                append(".json")
            }
            val response = getJson(endpoint)
            val metas = response.optJSONArray("metas") ?: JSONArray()
            (0 until metas.length()).mapNotNull { index -> metas.optJSONObject(index)?.toMedia(type) }
        }

    suspend fun loadMeta(media: CatalogMedia): Pair<CatalogMedia, List<CatalogEpisode>> =
        withContext(Dispatchers.IO) {
            val type = media.type
            val response = getJson("$CINEMETA/meta/$type/${encodePath(media.id)}.json")
            val meta = response.optJSONObject("meta") ?: JSONObject()
            val enriched = meta.toMedia(type).copy(
                title = meta.optString("name").ifBlank { media.title },
                description = meta.optNullable("description") ?: media.description,
                poster = meta.optNullable("poster") ?: media.poster,
                background = meta.optNullable("background") ?: media.background,
                year = meta.optNullable("year") ?: media.year,
                rating = meta.optNullable("imdbRating") ?: media.rating,
                genres = meta.optStringList("genres").ifEmpty { media.genres }
            )
            val videos = meta.optJSONArray("videos") ?: JSONArray()
            val episodes = (0 until videos.length()).mapNotNull { index ->
                val item = videos.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id")
                if (id.isBlank()) return@mapNotNull null
                CatalogEpisode(
                    id = id,
                    title = item.optString("title").ifBlank { "Episode ${index + 1}" },
                    season = item.optIntOrNull("season"),
                    episode = item.optIntOrNull("episode"),
                    released = item.optNullable("released")
                )
            }
            enriched to episodes
        }

    suspend fun findStreams(media: CatalogMedia, episode: CatalogEpisode? = null): List<StreamOption> =
        withContext(Dispatchers.IO) {
            val configured = addonUrl()
            if (configured.isBlank()) {
                throw IllegalStateException("Для поиска видеопотоков нужно один раз подключить настроенный Stremio-аддон. Каталог фильмов уже работает.")
            }
            val manifestUrl = normalizeManifestUrl(configured)
            val manifest = getJson(manifestUrl)
            if (manifest.optBoolean("configurable", false) || manifest.optBoolean("configurationRequired", false)) {
                throw IllegalStateException("Этот аддон требует настройки. Укажи его персональную настроенную Manifest-ссылку, а не общую ссылку установки.")
            }
            val base = manifestUrl.substringBeforeLast("/")
            val mediaId = episode?.id ?: media.id
            val type = if (episode != null) "series" else media.type
            val response = getJson("$base/stream/$type/${encodePath(mediaId)}.json")
            val streams = response.optJSONArray("streams") ?: JSONArray()
            (0 until streams.length()).mapNotNull { index ->
                val item = streams.optJSONObject(index) ?: return@mapNotNull null
                val url = item.optNullable("url") ?: return@mapNotNull null
                if (!url.startsWith("https://", true) && !url.startsWith("http://", true)) return@mapNotNull null
                StreamOption(
                    provider = item.optNullable("behaviorHints")?.let { "Stremio" } ?: "Stremio",
                    title = item.optNullable("title") ?: item.optNullable("name") ?: "Stream ${index + 1}",
                    url = url,
                    quality = item.optNullable("quality"),
                    language = item.optNullable("description")
                )
            }
        }

    private fun getJson(address: String): JSONObject {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "PASHAFLIXBOX/0.3 Android")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("Источник ответил HTTP $code: ${body.take(180)}")
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeManifestUrl(value: String): String {
        val clean = value.trim().trimEnd('/')
        return if (clean.endsWith("/manifest.json", true)) clean else "$clean/manifest.json"
    }

    private fun encodePath(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun JSONObject.toMedia(fallbackType: String) = CatalogMedia(
        id = optString("id"),
        type = optString("type").ifBlank { fallbackType },
        title = optString("name").ifBlank { optString("title") },
        poster = optNullable("poster"),
        background = optNullable("background"),
        description = optNullable("description"),
        year = optNullable("year"),
        rating = optNullable("imdbRating"),
        genres = optStringList("genres")
    )

    private fun JSONObject.optNullable(name: String): String? =
        optString(name).takeIf { it.isNotBlank() && it != "null" }

    private fun JSONObject.optStringList(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    companion object {
        private const val KEY_ADDON = "stremio_addon_manifest"
        private const val CINEMETA = "https://v3-cinemeta.strem.io"
    }
}
