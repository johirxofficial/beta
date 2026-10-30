package com.johirxofficial.x.data

import androidx.compose.runtime.Immutable

/** A single playable stream. */
@Immutable
data class Channel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    /** Extra HTTP headers required by the stream server (Referer, User-Agent, Cookie...). */
    val headers: Map<String, String> = emptyMap(),
) {
    val isLive: Boolean
        get() = url.lowercase().let { it.contains(".m3u8") || it.contains("/live") || it.contains(".ts") }

    /** Parses "(1080p)" style quality tags that IPTV playlists put in channel names. */
    val quality: String?
        get() = QUALITY_REGEX.find(name)?.groupValues?.getOrNull(1)

    companion object {
        private val QUALITY_REGEX = Regex("""\((\d{3,4}p)\)""", RegexOption.IGNORE_CASE)
    }
}

/** A category shown on the home screen. */
@Immutable
data class Category(
    val id: String,
    val name: String,
    val logo: String? = null,
    /** When set, channels are downloaded and parsed from this M3U/JSON playlist. */
    val playlistUrl: String? = null,
    /** When set, channels are already inside the categories JSON. */
    val inlineChannels: List<Channel> = emptyList(),
    /** Optional number from the API so the home screen can show a count before loading. */
    val channelCountHint: Int? = null,
    /** "country", "genre"... free form label from the API. */
    val type: String? = null,
)

/** Everything the API returns for the home screen. */
@Immutable
data class Catalog(
    val categories: List<Category> = emptyList(),
    val notices: List<String> = emptyList(),
    val update: AppUpdate? = null,
)

/** "App update" block from the JSON catalog. */
@Immutable
data class AppUpdate(
    val latestVersion: String? = null,
    val versionCode: Int? = null,
    val apkUrl: String? = null,
    val message: String? = null,
    val sizeMb: Double? = null,
) {
    fun isNewerThan(currentCode: Int): Boolean = versionCode?.let { it > currentCode } ?: false
}

