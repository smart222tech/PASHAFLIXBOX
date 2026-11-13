package com.smart222tech.pashaflixbox.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class PlaylistItem(
    val id: String,
    val title: String,
    val group: String,
    val logoUrl: String?,
    val streamUrl: String
) {
    val isSeries: Boolean
        get() = group.contains("serie", true) ||
            title.contains("staffel", true) || title.contains("episode", true)
}

object M3uPlaylistRepository {
    suspend fun load(playlistUrl: String): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val uri = URL(playlistUrl)
        require(uri.protocol.equals("https", true) || uri.protocol.equals("http", true)) {
            "Nur HTTP- und HTTPS-Playlist-Adressen werden unterstützt."
        }
        val connection = (uri.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 25_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "PASHAFLIXBOX/0.1 Android")
            instanceFollowRedirects = true
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("Playlist-Server antwortet mit HTTP $code")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parse(body)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(text: String): List<PlaylistItem> {
        val lines = text.lineSequence().map { it.trim().removePrefix("\uFEFF") }
            .filter { it.isNotEmpty() }.toList()
        require(lines.firstOrNull()?.startsWith("#EXTM3U", true) == true) {
            "Die Adresse liefert keine gültige M3U-Playlist."
        }
        val result = mutableListOf<PlaylistItem>()
        var pendingTitle: String? = null
        var pendingGroup = ""
        var pendingLogo: String? = null
        for (line in lines.drop(1)) {
            if (line.startsWith("#EXTINF", true)) {
                val comma = line.lastIndexOf(',')
                pendingTitle = if (comma >= 0 && comma < line.lastIndex) {
                    line.substring(comma + 1).trim()
                } else attribute(line, "tvg-name") ?: "Unbenannter Eintrag"
                pendingGroup = attribute(line, "group-title").orEmpty()
                pendingLogo = attribute(line, "tvg-logo")
            } else if (!line.startsWith("#") && pendingTitle != null) {
                val stream = runCatching { URL(line) }.getOrNull()
                if (stream != null && (stream.protocol == "https" || stream.protocol == "http")) {
                    val title = pendingTitle!!.ifBlank { "Unbenannter Eintrag" }
                    result += PlaylistItem(
                        id = MessageDigest.getInstance("SHA-256").digest(line.toByteArray())\n                            .joinToString("") { byte -> "%02x".format(byte) },
                        title = title,
                        group = pendingGroup,
                        logoUrl = pendingLogo?.takeIf { it.startsWith("https://") || it.startsWith("http://") },
                        streamUrl = line
                    )
                }
                pendingTitle = null
                pendingGroup = ""
                pendingLogo = null
            }
        }
        require(result.isNotEmpty()) { "In der Playlist wurden keine gültigen HTTP(S)-Einträge gefunden." }
        return result
    }

    private fun attribute(line: String, name: String): String? {
        val regex = Regex(Regex.escape(name) + "=\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.getOrNull(1)
    }
}
