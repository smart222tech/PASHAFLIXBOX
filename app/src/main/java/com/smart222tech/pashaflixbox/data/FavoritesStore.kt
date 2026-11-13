package com.smart222tech.pashaflixbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pashaFlixDataStore by preferencesDataStore(name = "pashaflixbox_preferences")

/** Stores stable catalogue IDs only; playback URLs and credentials are never persisted here. */
class FavoritesStore(private val context: Context) {
    private val favoritesKey = stringSetPreferencesKey("favorite_media_ids")

    val favorites: Flow<Set<String>> = context.pashaFlixDataStore.data.map { prefs ->
        prefs[favoritesKey] ?: emptySet()
    }

    suspend fun setFavorite(id: String, enabled: Boolean) {
        context.pashaFlixDataStore.edit { prefs ->
            val current = prefs[favoritesKey]?.toMutableSet() ?: mutableSetOf()
            if (enabled) current.add(id) else current.remove(id)
            prefs[favoritesKey] = current
        }
    }
}
