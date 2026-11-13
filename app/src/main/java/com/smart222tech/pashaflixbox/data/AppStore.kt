package com.smart222tech.pashaflixbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pashaStore by preferencesDataStore(name = "pashaflixbox_store")

class AppStore(context: Context) {
    private val dataStore = context.applicationContext.pashaStore
    private val favoritesKey = stringSetPreferencesKey("favorite_ids")
    private val historyKey = stringPreferencesKey("watch_history")
    private val playlistKey = stringPreferencesKey("playlist_url_encrypted")

    val favorites: Flow<Set<String>> = dataStore.data.map { it[favoritesKey] ?: emptySet() }
    val history: Flow<List<String>> = dataStore.data.map {
        it[historyKey].orEmpty().split("\n").filter(String::isNotBlank)
    }
    val playlistUrl: Flow<String> = dataStore.data.map { it[playlistKey]?.let(SecureUrlCipher::decrypt).orEmpty() }

    suspend fun setFavorite(id: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val updated = (prefs[favoritesKey] ?: emptySet()).toMutableSet()
            if (enabled) updated.add(id) else updated.remove(id)
            prefs[favoritesKey] = updated
        }
    }

    suspend fun savePlaylistUrl(url: String) {
        dataStore.edit { it[playlistKey] = SecureUrlCipher.encrypt(url.trim()) }
    }

    suspend fun recordPlayback(id: String) {
        dataStore.edit { prefs ->
            val old = prefs[historyKey].orEmpty().split("\n").filter(String::isNotBlank)
            prefs[historyKey] = (listOf(id) + old.filterNot { it == id }).take(100).joinToString("\n")
        }
    }

    suspend fun clearHistory() {
        dataStore.edit { it.remove(historyKey) }
    }
}
