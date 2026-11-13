package com.smart222tech.pashaflixbox.providers

/** In-memory provider for explicitly supplied, user-authorized test sources. */
class AuthorizedUrlProvider(
    private val entries: List<VideoSource> = emptyList()
) : VideoSourceProvider {
    override val id: String = "authorized-url"
    override val displayName: String = "Eigene / autorisierte Quellen"

    override suspend fun search(query: String): List<VideoSource> = entries.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.providerName.contains(query, ignoreCase = true)
    }
}
