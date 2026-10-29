package com.johirxofficial.x.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Loads categories from the JSON API and channels from either the same JSON or a
 * separate M3U playlist. Everything is cached in memory per API URL.
 */
class Repository(private val settings: Settings) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private var categoryCache: Pair<String, List<Category>>? = null
    private val channelCache = mutableMapOf<String, MutableList<Channel>>()

    suspend fun loadCategories(force: Boolean = false): List<Category> = withContext(Dispatchers.IO) {
        val apiUrl = settings.apiUrl
        val cached = categoryCache
        if (!force && cached != null && cached.first == apiUrl) return@withContext cached.second

        val body = fetch(apiUrl)
        val categories = if (looksLikePlaylist(body)) {
            listOf(
                Category(
                    id = "playlist",
                    name = "Playlist",
                    inlineChannels = M3uParser.parse(body),
                ),
            )
        } else {
            PlaylistJsonParser.parseCategories(body)
        }

        if (categories.isEmpty()) {
            throw IOException("No category found. JSON format check korun.")
        }

        categoryCache = apiUrl to categories
        channelCache.clear()
        categories
    }

    suspend fun loadChannels(category: Category, force: Boolean = false): List<Channel> =
        withContext(Dispatchers.IO) {
            if (!force) {
                if (category.inlineChannels.isNotEmpty()) return@withContext category.inlineChannels
                channelCache[category.id]?.takeIf { it.isNotEmpty() }?.let { return@withContext it }
            }

            val playlistUrl = category.playlistUrl
            val channels: List<Channel> = when {
                playlistUrl != null -> {
                    val body = fetch(playlistUrl)
                    if (looksLikePlaylist(body)) {
                        M3uParser.parse(body)
                    } else {
                        PlaylistJsonParser.parseChannels(body)
                    }
                }

                category.inlineChannels.isNotEmpty() -> category.inlineChannels
                else -> emptyList()
            }

            if (channels.isEmpty()) {
                throw IOException("Ei category te kono channel paoa jayni.")
            }

            channelCache[category.id] = channels.toMutableList()
            channels
        }

    /** Clears caches, used after the API URL changes. */
    fun invalidate() {
        categoryCache = null
        channelCache.clear()
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
            .header("Cache-Control", "no-cache")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} — ${url.substringBefore('?')}")
            }
            return response.body?.string()?.takeIf { it.isNotBlank() }
                ?: throw IOException("Empty response — ${url.substringBefore('?')}")
        }
    }
}
