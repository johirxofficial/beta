package com.johirxofficial.x.data

/** Minimal M3U / M3U8 playlist parser (Xtream, IPTV-org and VLC style playlists). */
object M3uParser {

    private val attributeRegex = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")

    fun parse(raw: String, defaultHeaders: Map<String, String> = emptyMap()): List<Channel> {
        val channels = mutableListOf<Channel>()

        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null
        val pendingHeaders = linkedMapOf<String, String>()

        fun flush(url: String) {
            if (!UrlUtils.isStreamUrl(url)) {
                pendingName = null; pendingLogo = null; pendingGroup = null; pendingHeaders.clear()
                return
            }
            channels += Channel(
                name = pendingName?.takeIf { it.isNotBlank() } ?: UrlUtils.prettyName(url),
                url = url,
                logo = pendingLogo,
                group = pendingGroup,
                headers = defaultHeaders + pendingHeaders,
            )
            pendingName = null
            pendingLogo = null
            pendingGroup = null
            pendingHeaders.clear()
        }

        raw.removePrefix("\uFEFF").lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val attributes = attributeRegex.findAll(line)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2] }

                    pendingLogo = attributes["tvg-logo"] ?: attributes["logo"]
                    pendingGroup = attributes["group-title"] ?: attributes["group"]

                    val commaIndex = line.lastIndexOf(',')
                    val title = if (commaIndex >= 0) line.substring(commaIndex + 1).trim() else ""
                    pendingName = title.ifBlank {
                        attributes["tvg-name"]?.takeIf { it.isNotBlank() } ?: pendingName
                    }
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

                line.startsWith("#") -> Unit // #EXTM3U, #EXT-X-..., comments

                else -> flush(line)
            }
        }

        return channels
    }
}
