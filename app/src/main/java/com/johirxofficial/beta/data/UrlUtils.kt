package com.johirxofficial.beta.data

internal object UrlUtils {

    /**
     * The Dalvik/Android system user agent used for every catalog, playlist, update and
     * stream request. Some providers only return playable links to an Android client.
     */
    const val UA =
        "Dalvik/2.1.0 (Linux; U; Android 12; SM-A217F Build/SP1A.210812.016) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Mobile Safari/537.36"

    private val STREAM_SCHEMES = listOf("http://", "https://", "rtsp://", "rtmp://", "udp://", "rtp://")

    fun isStreamUrl(value: String): Boolean {
        val text = value.trim().lowercase()
        if (text.isEmpty()) return false
        return STREAM_SCHEMES.any { text.startsWith(it) }
    }

    /** Human friendly fallback name for a stream that has no title in the playlist. */
    fun prettyName(url: String): String {
        val cleaned = url.substringBefore('?').substringBefore('#').trimEnd('/')
        val last = cleaned.substringAfterLast('/')
        if (last.isNotBlank() && !last.contains('.') && !last.contains(':')) return last
        val host = runCatching { java.net.URI(url).host }.getOrNull()
        return host?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: url
    }

    fun hostOf(url: String): String =
        runCatching { java.net.URI(url).host }.getOrNull()?.removePrefix("www.") ?: url

    /** Resolves relative playlist links against the URL the playlist was loaded from. */
    fun resolve(baseUrl: String?, value: String): String? {
        val text = value.trim()
        if (text.isEmpty()) return null
        if (isStreamUrl(text)) return text
        val base = baseUrl?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        return runCatching { java.net.URI(base).resolve(text).toString() }.getOrNull()
    }
}
