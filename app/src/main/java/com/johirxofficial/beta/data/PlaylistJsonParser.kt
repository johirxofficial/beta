package com.johirxofficial.beta.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Tolerant JSON parser. Field names are matched case-insensitively and several
 * commonly used shapes are supported, so the app keeps working when the API
 * changes slightly. Supported shapes:
 *
 * 1. `{ "categories": [ { "name": "Indian", "channels": [...] } ] }`
 * 2. `[ { "name": "Indian", "playlist_url": "https://.../indian.m3u" } ]`
 * 3. `{ "indian": [ ...channels... ], "sports": [ ... ] }`
 * 4. `{ "data": { "categories": [...] } }` (nested wrappers are unwrapped)
 */
object PlaylistJsonParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val NESTED_LIST_KEYS =
        listOf("categories", "catalog", "playlists", "sections", "groups", "items", "data")
    private val NAME_KEYS = arrayOf("name", "title", "category", "label", "group", "group_title", "tvg_name")
    private val ID_KEYS = arrayOf("id", "slug", "key", "code", "category_id")
    private val LOGO_KEYS = arrayOf("logo", "icon", "image", "poster", "thumbnail", "tvg_logo", "cover")
    private val SOURCE_KEYS = arrayOf("source", "feed", "playlist_source", "playlist")
    private val PLAYLIST_KEYS = arrayOf(
        "playlist_url", "playlist", "m3u", "m3u_url", "m3u8", "source", "url", "link", "file",
        "endpoint", "channels_url", "stream_url",
    )
    private val CHANNEL_LIST_KEYS =
        arrayOf("channels", "items", "streams", "links", "list", "entries", "playlist_items", "data")
    private val CHANNEL_URL_KEYS =
        arrayOf("url", "link", "stream", "stream_url", "src", "file", "source", "m3u8", "path", "hls", "uri")
    private val COUNT_KEYS = arrayOf("channels_count", "channel_count", "count", "total", "channels")

    fun parseCategories(raw: String): List<Category> = parseCatalog(raw).categories

    /** Parses categories plus the optional notices / app-update blocks. */
    fun parseCatalog(raw: String): Catalog {
        val root = jsonOrNull(raw) ?: return Catalog()
        val objects: List<JsonObject> = when (root) {
            is JsonArray -> root.mapNotNull { it as? JsonObject }
            is JsonObject -> categoryObjectsFrom(root)
            else -> emptyList()
        }
        val categories = objects
            .mapIndexedNotNull { index, obj -> categoryFrom(obj, index) }
            .filter { it.name.isNotBlank() }
        val container = root as? JsonObject
        return Catalog(
            categories = categories,
            notices = container?.let(::noticesFrom).orEmpty(),
            update = container?.let(::updateFrom),
        )
    }

    /** Parses a plain list of channels (used when a category URL points to channel JSON). */
    fun parseChannels(raw: String): List<Channel> {
        val root = jsonOrNull(raw) ?: return emptyList()
        val elements: List<JsonElement> = when (root) {
            is JsonArray -> root
            is JsonObject -> {
                val nested = NESTED_LIST_KEYS.firstNotNullOfOrNull { key -> root[key] as? JsonArray }
                val sourceObject = SOURCE_KEYS.firstNotNullOfOrNull { key -> root[key] as? JsonObject }
                nested?.toList()
                    ?: (root["channels"] as? JsonArray)?.toList()
                    ?: sourceObject?.let { source ->
                        CHANNEL_LIST_KEYS.firstNotNullOfOrNull { key -> source[key] as? JsonArray }
                    }?.toList()
                    ?: listOf(root)
            }
            else -> emptyList()
        }
        return elements.mapNotNull { channelFrom(it) }
    }

    private fun jsonOrNull(raw: String): JsonElement? {
        val cleaned = raw.removePrefix("\uFEFF").trim()
        if (cleaned.isEmpty()) return null
        return runCatching { json.parseToJsonElement(cleaned) }.getOrNull()
    }

    /** Finds the category array, unwrapping `{"data": {...}}` style wrappers. */
    private fun categoryObjectsFrom(root: JsonObject, depth: Int = 0): List<JsonObject> {
        if (depth > 4) return emptyList()

        for (key in NESTED_LIST_KEYS) {
            val value = root.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value ?: continue
            if (value is JsonArray) {
                val objects = value.mapNotNull { it as? JsonObject }
                if (objects.isNotEmpty()) return objects
            }
            if (value is JsonObject) {
                val nested = categoryObjectsFrom(value, depth + 1)
                if (nested.isNotEmpty()) return nested
            }
        }

        // `{"Indian": [...], "Sports": [...]}` - object keys are the category names.
        return root.entries.mapNotNull { (key, value) ->
            when (value) {
                is JsonArray -> jsonObjectOf(NAME_KEYS.first() to JsonPrimitive(key), "channels" to value)
                is JsonObject -> jsonObjectOf(
                    NAME_KEYS.first() to JsonPrimitive(key),
                    "channels" to JsonArray(listOf(value)),
                )
                else -> null
            }
        }
    }

    private fun categoryFrom(obj: JsonObject, index: Int): Category {
        val name = firstString(obj, *NAME_KEYS) ?: "Category ${index + 1}"
        val id = firstString(obj, *ID_KEYS) ?: slugify(name).ifBlank { "category_$index" }
        val source = firstObject(obj, *SOURCE_KEYS)

        val playlist = firstString(obj, *PLAYLIST_KEYS)
            ?.takeIf { UrlUtils.isStreamUrl(it) }
            ?: source?.let { firstString(it, *PLAYLIST_KEYS) }
                ?.takeIf { UrlUtils.isStreamUrl(it) }

        val channels = (
            firstArray(obj, *CHANNEL_LIST_KEYS)
                ?: source?.let { firstArray(it, *CHANNEL_LIST_KEYS) }
            )
            ?.mapNotNull { channelFrom(it) }
            .orEmpty()

        return Category(
            id = id,
            name = name,
            logo = firstString(obj, *LOGO_KEYS) ?: source?.let { firstString(it, *LOGO_KEYS) },
            playlistUrl = playlist,
            inlineChannels = channels,
            channelCountHint = firstInt(obj, *COUNT_KEYS)?.takeIf { it > 0 }
                ?: source?.let { firstInt(it, *COUNT_KEYS) }?.takeIf { it > 0 }
                ?: channels.size.takeIf { it > 0 },
            type = firstString(obj, "type", "kind", "genre"),
        )
    }

    private fun channelFrom(element: JsonElement): Channel? = when (element) {
        is JsonPrimitive -> element.contentOrNull
            ?.takeIf { UrlUtils.isStreamUrl(it) }
            ?.let { url -> Channel(name = UrlUtils.prettyName(url), url = url) }

        is JsonObject -> {
            val url = firstString(element, *CHANNEL_URL_KEYS)
            if (url == null || !UrlUtils.isStreamUrl(url)) {
                null
            } else {
                Channel(
                    name = firstString(element, *NAME_KEYS) ?: UrlUtils.prettyName(url),
                    url = url,
                    logo = firstString(element, *LOGO_KEYS),
                    group = firstString(element, "group", "group_title", "group-title", "category"),
                    headers = headersFrom(element),
                )
            }
        }

        else -> null
    }

    private fun headersFrom(obj: JsonObject): Map<String, String> {
        val headers = linkedMapOf<String, String>()

        val raw = obj.entries.firstOrNull {
            it.key.equals("headers", true) || it.key.equals("http_headers", true) || it.key.equals("header", true)
        }?.value
        if (raw is JsonObject) {
            raw.forEach { (key, value) ->
                if (value is JsonPrimitive) {
                    value.contentOrNull?.takeIf { it.isNotBlank() }?.let { headers[key] = it }
                }
            }
        }

        firstString(obj, "referer", "referrer", "http_referer", "http-referrer")?.let { headers["Referer"] = it }
        firstString(obj, "user_agent", "user-agent", "http_user_agent")?.let { headers["User-Agent"] = it }
        firstString(obj, "cookie", "http_cookie")?.let { headers["Cookie"] = it }
        firstString(obj, "origin")?.let { headers["Origin"] = it }

        return headers
    }

    private fun firstString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val value = obj.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value ?: continue
            if (value is JsonPrimitive) {
                val text = value.contentOrNull?.trim()
                if (!text.isNullOrEmpty() && !text.equals("null", ignoreCase = true)) return text
            }
        }
        return null
    }

    private fun firstArray(obj: JsonObject, vararg keys: String): JsonArray? {
        for (key in keys) {
            val value = obj.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
            if (value is JsonArray) return value
        }
        return null
    }

    private fun firstObject(obj: JsonObject, vararg keys: String): JsonObject? {
        for (key in keys) {
            val value = obj.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
            if (value is JsonObject) return value
        }
        return null
    }

    private fun noticesFrom(root: JsonObject): List<String> {
        val direct = noticeArray(root)
        if (direct != null) {
            return direct.mapNotNull(::noticeText)
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
        }
        val data = firstObject(root, "data") ?: return emptyList()
        return noticeArray(data).orEmpty()
            .mapNotNull(::noticeText)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun noticeArray(obj: JsonObject): JsonArray? =
        firstArray(obj, "notices", "notice", "announcements", "messages", "alerts", "news")

    private fun noticeText(element: JsonElement): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull
        is JsonObject -> firstString(element, "text", "message", "notice", "title", "body")
        else -> null
    }

    private fun updateFrom(root: JsonObject): AppUpdate? {
        val obj = firstObject(root, "update", "app_update", "appUpdate", "app_info", "appInfo") ?: return null
        val update = AppUpdate(
            latestVersion = firstString(obj, "latest_version", "version_name", "version", "latest", "tag"),
            versionCode = firstInt(obj, "version_code", "versionCode", "code", "build", "build_number"),
            apkUrl = firstString(obj, "apk_url", "download_url", "apk", "url", "link"),
            message = firstString(obj, "message", "notes", "changelog", "whats_new", "description"),
            sizeMb = firstString(obj, "size_mb", "size")?.toDoubleOrNull(),
        )
        return update.takeIf { it.apkUrl != null || it.latestVersion != null }
    }

    private fun firstInt(obj: JsonObject, vararg keys: String): Int? {
        for (key in keys) {
            val value = obj.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value ?: continue
            if (value is JsonPrimitive) {
                value.contentOrNull?.trim()?.toIntOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun jsonObjectOf(vararg pairs: Pair<String, JsonElement>): JsonObject =
        JsonObject(pairs.associate { it.first to it.second })

    private fun slugify(value: String): String = value
        .lowercase()
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')
}
