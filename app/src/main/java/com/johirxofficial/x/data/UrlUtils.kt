package com.johirxofficial.x.data

internal object UrlUtils {

    /** Many IPTV servers refuse unknown user agents, this one works on most of them. */
    const val UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Mobile Safari/537.36"

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
}
