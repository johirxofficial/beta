package com.johirxofficial.beta.data

/**
 * Minimal M3U / M3U8 playlist parser (Xtream, IPTV-org and VLC style playlists).
 *
 * The parser does not care about the file extension or the server technology behind the
 * URL: a `.m3u`, `.m3u8`, `.php`, `.txt` or extension-less endpoint all work as long as
 * the response contains `#EXTM3U` / `#EXTINF` lines (or a plain list of stream URLs).
 */
object M3uParser {

    private val quotedAttributeRegex = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")
    private val bareAttributeRegex = Regex("([A-Za-z0-9_-]+)=([^\\s,\"]+)")

    fun parse(
        raw: String,
        defaultHeaders: Map<String, String> = emptyMap(),
        baseUrl: String? = null,
    ): List<Channel> {
        val channels = mutableListOf<Channel>()
        val seenUrls = HashSet<String>()

        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null
        val pendingHeaders = linkedMapOf<String, String>()

        fun resetPending() {
            pendingName = null
            pendingLogo = null
            pendingGroup = null
            pendingHeaders.clear()
        }

        fun flush(value: String) {
            val url = UrlUtils.resolve(baseUrl, value)
            if (url == null || !UrlUtils.isStreamUrl(url)) {
                resetPending()
                return
            }
            if (seenUrls.add(url)) {
                channels += Channel(
                    name = pendingName?.takeIf { it.isNotBlank() } ?: UrlUtils.prettyName(url),
                    url = url,
                    logo = pendingLogo,
                    group = pendingGroup,
                    headers = defaultHeaders + pendingHeaders,
                )
            }
            resetPending()
        }

        raw.removePrefix("\uFEFF").lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val quoted = quotedAttributeRegex.findAll(line)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    val bare = bareAttributeRegex.findAll(line)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    val attributes = bare + quoted

                    pendingLogo = attributes["tvg-logo"] ?: attributes["logo"] ?: pendingLogo
                    pendingGroup = attributes["group-title"] ?: attributes["group"] ?: pendingGroup

                    val commaIndex = line.lastIndexOf(',')
                    val title = if (commaIndex >= 0) line.substring(commaIndex + 1).trim() else ""
                    pendingName = title.ifBlank {
                        attributes["tvg-name"]?.takeIf { it.isNotBlank() } ?: pendingName
                    }
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    pendingGroup = line.substringAfter(':').trim().takeIf { it.isNotEmpty() }
                }

                line.startsWith("#EXTVLCOPT", ignoreCase = true) -> {
                    val body = line.substringAfter(':', "")
                    val key = body.substringBefore('=').trim().lowercase()
                    val value = body.substringAfter('=', "").trim()
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        when (key) {
                            "http-referrer", "http-referer" -> pendingHeaders["Referer"] = value
                            "http-user-agent" -> pendingHeaders["User-Agent"] = value
                            "http-cookie" -> pendingHeaders["Cookie"] = value
                            "http-origin" -> pendingHeaders["Origin"] = value
                        }
                    }
                }

                line.startsWith("#KODIPROP:", ignoreCase = true) -> {
                    val body = line.substringAfter(':', "")
                    val key = body.substringBefore('=').trim().lowercase()
                    val value = body.substringAfter('=', "").trim()
                    if (key.endsWith("stream_headers") && value.isNotEmpty()) {
                        value.split('&').forEach { pair ->
                            val name = pair.substringBefore('=').trim()
                            val headerValue = pair.substringAfter('=', "").trim()
                            if (name.isNotEmpty() && headerValue.isNotEmpty()) {
                                pendingHeaders[name] = headerValue
                            }
                        }
                    }
                }

                line.startsWith("#") -> Unit // #EXTM3U, #EXT-X-..., #KODIPROP comments

                else -> flush(line)
            }
        }

        return channels
    }
}
