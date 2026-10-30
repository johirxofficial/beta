package com.johirxofficial.beta.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Loads categories from the JSON API and channels from any playlist endpoint.
 *
 * There is intentionally **no cache**: every load hits the network so the app always
 * shows exactly what the API returns. The only state kept in memory is the list the
 * current screen is already displaying.
 *
 * Playlist endpoints are detected from their **content**, never from the URL suffix. A
 * server may expose the same data as `channels.m3u`, `channels.php`, `get.php?...` or an
 * extension-less route, and may switch between M3U and JSON without notice.
 */
class Repository(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun catalog(apiUrl: String): Catalog = withContext(Dispatchers.IO) {
        val catalog = parseDocument(fetch(apiUrl), apiUrl)
        if (catalog.categories.isEmpty()) {
            throw IOException("No category found at ${UrlUtils.hostOf(apiUrl)}")
        }
        catalog
    }

    suspend fun channels(category: Category): List<Channel> = withContext(Dispatchers.IO) {
        if (category.inlineChannels.isNotEmpty()) return@withContext category.inlineChannels

        val playlistUrl = category.playlistUrl
            ?: throw IOException("This category has no playlist URL")

        val channels = parseChannelDocument(fetch(playlistUrl), playlistUrl)
        if (channels.isEmpty()) {
            throw IOException("No playable channels found in this category")
        }
        channels
    }

    private fun parseDocument(body: String, sourceUrl: String): Catalog {
        val head = body.removePrefix("\uFEFF").trimStart()
        if (head.startsWith("{") || head.startsWith("[")) {
            val catalog = PlaylistJsonParser.parseCatalog(body)
            if (catalog.categories.isNotEmpty()) return catalog
        }

        if (looksLikePlaylist(body) || looksLikeUrlList(body)) {
            return Catalog(
                categories = listOf(
                    Category(
                        id = "playlist",
                        name = "Playlist",
                        inlineChannels = M3uParser.parse(body, baseUrl = sourceUrl),
                    ),
                ),
            )
        }

        return PlaylistJsonParser.parseCatalog(body)
    }

    private fun parseChannelDocument(body: String, sourceUrl: String): List<Channel> {
        val head = body.removePrefix("\uFEFF").trimStart()
        if (head.startsWith("{") || head.startsWith("[")) {
            val jsonChannels = PlaylistJsonParser.parseChannels(body)
            if (jsonChannels.isNotEmpty()) return jsonChannels
        }

        if (looksLikePlaylist(body) || looksLikeUrlList(body)) {
            return M3uParser.parse(body, baseUrl = sourceUrl)
        }

        // Last chance: a JSON document with a less common wrapper.
        return PlaylistJsonParser.parseChannels(body)
    }

    /**
     * True when the body contains M3U markers. We scan the first part of the response so
     * PHP scripts that print a BOM, blank lines or a small HTML wrapper still work.
     */
    private fun looksLikePlaylist(body: String): Boolean {
        val head = body.removePrefix("\uFEFF").take(64 * 1024)
        return head.contains("#EXTM3U", ignoreCase = true) ||
            head.contains("#EXTINF", ignoreCase = true)
    }

    /** Plain text lists (one stream URL per line) are valid playlist responses too. */
    private fun looksLikeUrlList(body: String): Boolean =
        body.removePrefix("\uFEFF")
            .lineSequence()
            .take(200)
            .any { line ->
                val text = line.trim()
                text.startsWith("http://", ignoreCase = true) ||
                    text.startsWith("https://", ignoreCase = true) ||
                    text.startsWith("rtsp://", ignoreCase = true) ||
                    text.startsWith("rtmp://", ignoreCase = true)
            }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UrlUtils.UA)
            .header("Accept", "*/*")
            // Explicitly bypass every intermediate cache.
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} — ${UrlUtils.hostOf(url)}")
            }
            return response.body?.string()?.takeIf { it.isNotBlank() }
                ?: throw IOException("Empty response — ${UrlUtils.hostOf(url)}")
        }
    }
}
