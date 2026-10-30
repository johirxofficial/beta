package com.johirxofficial.x.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Loads categories from the JSON API and channels from M3U/JSON playlists.
 *
 * There is intentionally **no cache**: every load hits the network so the app always
 * shows exactly what the API returns. The only state kept in memory is the list the
 * current screen is already displaying.
 */
class Repository(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun catalog(apiUrl: String): Catalog = withContext(Dispatchers.IO) {
        val catalog = parseDocument(fetch(apiUrl))
        if (catalog.categories.isEmpty()) {
            throw IOException("No category found at ${UrlUtils.hostOf(apiUrl)}")
        }
        catalog
    }

    suspend fun channels(category: Category): List<Channel> = withContext(Dispatchers.IO) {
        if (category.inlineChannels.isNotEmpty()) return@withContext category.inlineChannels

        val playlistUrl = category.playlistUrl
            ?: throw IOException("Ei category te playlist nei")

        val body = fetch(playlistUrl)
        val channels = if (looksLikePlaylist(body)) {
            M3uParser.parse(body)
        } else {
            PlaylistJsonParser.parseChannels(body)
        }

        if (channels.isEmpty()) {
            throw IOException("Ei category te channel paoa jayni")
        }
        channels
    }

    private fun parseDocument(body: String): Catalog = if (looksLikePlaylist(body)) {
        Catalog(
            categories = listOf(
                Category(
                    id = "playlist",
                    name = "Playlist",
                    inlineChannels = M3uParser.parse(body),
                ),
            ),
        )
    } else {
        PlaylistJsonParser.parseCatalog(body)
    }

    private fun looksLikePlaylist(body: String): Boolean {
        val head = body.removePrefix("\uFEFF").trimStart()
        return head.startsWith("#EXTM3U", true) || head.startsWith("#EXTINF", true)
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
