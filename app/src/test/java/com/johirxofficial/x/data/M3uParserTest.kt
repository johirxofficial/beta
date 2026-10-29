package com.johirxofficial.x.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    /** Example taken from a real iptv-org playlist. */
    private val iptvOrg = """
        #EXTM3U
        #EXTINF:-1 tvg-id="5Plus.il@SD" tvg-logo="https://i.imgur.com/L3RA9Lv.png" group-title="Sports",5Plus (1080p)
        http://89.33.29.115:8080/sportplus/index.m3u8
        #EXTINF:-1 tvg-id="30AGolfKingdom.us@SD" tvg-logo="https://i.imgur.com/Lv53nh4.png" group-title="Sports",30A Golf Kingdom (720p)
        https://30a-tv.com/feeds/vidaa/golf.m3u8
        #EXTINF:-1,Channel Without Logo
        https://example.com/stream.ts
    """.trimIndent()

    @Test
    fun `parses iptv-org style playlist`() {
        val channels = M3uParser.parse(iptvOrg)

        assertEquals(3, channels.size)
        assertEquals("5Plus (1080p)", channels[0].name)
        assertEquals("http://89.33.29.115:8080/sportplus/index.m3u8", channels[0].url)
        assertEquals("https://i.imgur.com/L3RA9Lv.png", channels[0].logo)
        assertEquals("Sports", channels[0].group)
        assertEquals("Channel Without Logo", channels[2].name)
        assertEquals(null, channels[2].logo)
    }

    @Test
    fun `reads vlc options as request headers`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 group-title="News",Headered
            #EXTVLCOPT:http-referrer=https://site.example/
            #EXTVLCOPT:http-user-agent=CustomUA/1.0
            https://example.com/headered.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        assertEquals("https://site.example/", channels[0].headers["Referer"])
        assertEquals("CustomUA/1.0", channels[0].headers["User-Agent"])
    }

    @Test
    fun `plain url list gets a readable name`() {
        val channels = M3uParser.parse("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")

        assertEquals(1, channels.size)
        assertEquals("test-streams.mux.dev", channels[0].name)
    }

    @Test
    fun `ignores comments and non stream lines`() {
        val playlist = """
            #EXTM3U
            # some comment
            not a url
            #EXTINF:-1,Real One
            https://example.com/a.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        assertTrue(channels[0].url.endsWith("a.m3u8"))
    }
}
