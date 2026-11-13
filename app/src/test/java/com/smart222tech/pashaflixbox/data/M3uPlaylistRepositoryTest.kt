package com.smart222tech.pashaflixbox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uPlaylistRepositoryTest {
    @Test
    fun parsesTitlesGroupsAndArtwork() {
        val input = """#EXTM3U
#EXTINF:-1 tvg-name="Film Eins" tvg-logo="https://cdn.example/poster.jpg" group-title="Filme",Film Eins
https://media.example/film.mp4
#EXTINF:-1 group-title="Serien",Serie Zwei
https://media.example/serie.m3u8
"""
        val items = M3uPlaylistRepository.parse(input)
        assertEquals(2, items.size)
        assertEquals("Film Eins", items[0].title)
        assertEquals("Filme", items[0].group)
        assertEquals("https://cdn.example/poster.jpg", items[0].logoUrl)
        assertTrue(items[1].isSeries)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonM3uContent() {
        M3uPlaylistRepository.parse("not a playlist")
    }
}
