package com.smart222tech.pashaflixbox.providers

data class VideoSource(
    val id: String,
    val providerName: String,
    val title: String,
    val playbackUrl: String?,
    val audioLanguages: List<String> = emptyList(),
    val subtitleLanguages: List<String> = emptyList(),
    val qualityLabel: String? = null,
    val requiresExternalApp: Boolean = false,
    val note: String? = null
)

/**
 * Provider contract for personal servers and catalogues the user is authorized to access.
 * Implementations must not bypass DRM, authentication, paywalls or access controls.
 */
interface VideoSourceProvider {
    val id: String
    val displayName: String
    suspend fun search(query: String): List<VideoSource>
}

fun choosePreferredLanguage(languages: List<String>, preferred: String = "de"): String? =
    languages.firstOrNull { it.equals(preferred, ignoreCase = true) }
        ?: languages.firstOrNull { it.startsWith("$preferred-", ignoreCase = true) }
