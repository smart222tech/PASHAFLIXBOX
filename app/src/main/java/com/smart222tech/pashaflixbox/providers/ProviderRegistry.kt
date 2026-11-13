package com.smart222tech.pashaflixbox.providers

class ProviderRegistry(providers: List<VideoSourceProvider> = emptyList()) {
    private val registered = providers.associateBy { it.id }

    fun all(): List<VideoSourceProvider> = registered.values.toList()
    fun find(id: String): VideoSourceProvider? = registered[id]

    suspend fun searchAll(query: String): List<VideoSource> {
        if (query.isBlank()) return emptyList()
        return registered.values.flatMap { provider ->
            runCatching { provider.search(query.trim()) }.getOrDefault(emptyList())
        }
    }
}

enum class PlaybackTarget { MEDIA3, EXTERNAL, UNAVAILABLE }

fun VideoSource.playbackTarget(): PlaybackTarget = when {
    requiresExternalApp -> PlaybackTarget.EXTERNAL
    playbackUrl?.startsWith("https://", ignoreCase = true) == true -> PlaybackTarget.MEDIA3
    else -> PlaybackTarget.UNAVAILABLE
}
