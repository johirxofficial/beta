package com.johirxofficial.x.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistJsonParserTest {

    @Test
    fun `shape A - categories with playlist urls`() {
        val json = """
            {
              "app_name": "X",
              "categories": [
                { "id": "indian", "name": "Indian", "playlist_url": "https://iptv-org.github.io/iptv/countries/in.m3u" },
                { "id": "bangla", "name": "Bangla", "playlist_url": "https://iptv-org.github.io/iptv/countries/bd.m3u" }
              ]
            }
        """.trimIndent()

        val categories = PlaylistJsonParser.parseCategories(json)

        assertEquals(2, categories.size)
        assertEquals("indian", categories[0].id)
        assertEquals("Bangla", categories[1].name)
        assertEquals("https://iptv-org.github.io/iptv/countries/bd.m3u", categories[1].playlistUrl)
        assertTrue(categories[0].inlineChannels.isEmpty())
    }

    @Test
    fun `shape B - inline channels with headers`() {
        val json = """
            {
              "categories": [
                {
                  "name": "Bangla",
                  "channels": [
                    { "name": "One", "url": "https://example.com/one.m3u8", "logo": "https://example.com/1.png" },
                    {
                      "title": "Two",
                      "link": "https://example.com/two.ts",
                      "headers": { "Referer": "https://example.com/" }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val category = PlaylistJsonParser.parseCategories(json).single()

        assertEquals("Bangla", category.name)
        assertEquals("bangla", category.id)
        assertEquals(2, category.inlineChannels.size)
        assertEquals("One", category.inlineChannels[0].name)
        assertEquals("https://example.com/1.png", category.inlineChannels[0].logo)
        assertEquals("Two", category.inlineChannels[1].name)
        assertEquals("https://example.com/two.ts", category.inlineChannels[1].url)
        assertEquals("https://example.com/", category.inlineChannels[1].headers["Referer"])
    }

    @Test
    fun `shape C - map of category name to channels`() {
        val json = """
            {
              "Indian": [ "https://example.com/a.m3u8" ],
              "Sports": [ { "name": "Final", "url": "https://example.com/b.ts" } ],
              "updated": "2026-10-29"
            }
        """.trimIndent()

        val categories = PlaylistJsonParser.parseCategories(json)

        assertEquals(2, categories.size)
        assertEquals("Indian", categories[0].name)
        assertEquals(1, categories[0].inlineChannels.size)
        assertEquals("Final", categories[1].inlineChannels.single().name)
    }

    @Test
    fun `top level array and data wrapper`() {
        val arrayJson = """[ { "name": "Movies", "playlist_url": "https://example.com/movies.m3u" } ]"""
        val wrappedJson = """
            { "data": { "categories": [ { "name": "News", "playlist_url": "https://example.com/news.m3u" } ] } }
        """.trimIndent()

        assertEquals("Movies", PlaylistJsonParser.parseCategories(arrayJson).single().name)
        assertEquals("News", PlaylistJsonParser.parseCategories(wrappedJson).single().name)
    }

    /** The real API used by this app: 13 categories, some with placeholder urls. */
    @Test
    fun `parses the live x api document`() {
        val json = """
            {
              "app_name": "X",
              "updated": "2026-10-29",
              "categories": [
                { "id": "indian", "name": "Indian", "logo": "https://github.io",
                  "playlist_url": "https://iptv-org.github.io/iptv/countries/in.m3u" },
                { "id": "bangla", "name": "Bangla", "logo": "https://github.io",
                  "playlist_url": "https://iptv-org.github.io/iptv/countries/bd.m3u" },
                { "id": "entertainment", "name": "Entertainment", "logo": "https://github.io",
                  "playlist_url": "https://github.io" },
                { "id": "sports", "name": "Sports", "logo": "https://github.io",
                  "playlist_url": "https://github.io" },
                { "id": "kids_animation", "name": "Kids & Animation", "logo": "https://github.io",
                  "playlist_url": "https://github.io" }
              ]
            }
        """.trimIndent()

        val categories = PlaylistJsonParser.parseCategories(json)

        assertEquals(5, categories.size)
        assertEquals("kids_animation", categories[4].id)
        assertEquals("Kids & Animation", categories[4].name)
        assertEquals("https://iptv-org.github.io/iptv/countries/in.m3u", categories[0].playlistUrl)
    }

    @Test
    fun `broken json returns empty list instead of crashing`() {
        assertTrue(PlaylistJsonParser.parseCategories("<html>not json</html>").isEmpty())
    }

    @Test
    fun `channel list parsing accepts plain object list`() {
        val channels = PlaylistJsonParser.parseChannels(
            """[ { "name": "A", "url": "https://example.com/a.m3u8" }, { "url": "https://example.com/b.mp4" } ]""",
        )

        assertEquals(2, channels.size)
        assertEquals("A", channels[0].name)
        assertEquals("example.com", channels[1].name)
        assertNull(channels[1].logo)
    }

    @Test
    fun `count hint, type, logo and inline channels come from the api`() {
        val json = """
            {
              "categories": [
                { "id": "in", "name": "Indian", "type": "country", "channels_count": 849,
                  "logo": "https://flagcdn.com/w80/in.png",
                  "playlist_url": "https://iptv-org.github.io/iptv/countries/in.m3u" },
                { "id": "featured", "name": "Featured", "type": "mixed", "channels_count": 2,
                  "channels": [
                    { "name": "One", "url": "https://example.com/1.m3u8" },
                    { "name": "Two", "url": "https://example.com/2.ts" }
                  ] }
              ]
            }
        """.trimIndent()

        val categories = PlaylistJsonParser.parseCategories(json)

        assertEquals(849, categories[0].channelCountHint)
        assertEquals("country", categories[0].type)
        assertEquals("https://flagcdn.com/w80/in.png", categories[0].logo)
        assertEquals("mixed", categories[1].type)
        assertEquals(2, categories[1].channelCountHint)
        assertEquals(2, categories[1].inlineChannels.size)
    }
}
