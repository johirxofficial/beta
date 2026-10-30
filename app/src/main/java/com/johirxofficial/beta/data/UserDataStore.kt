package com.johirxofficial.beta.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Favorites + recently watched channels, persisted as compact JSON in SharedPreferences. */
class UserDataStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("beta_user", Context.MODE_PRIVATE)

    fun favorites(): List<Channel> = decode(prefs.getString(KEY_FAVORITES, null))

    fun recent(): List<Channel> = decode(prefs.getString(KEY_RECENT, null))

    fun toggleFavorite(channel: Channel): List<Channel> {
        val current = favorites()
        val next = if (current.any { it.url == channel.url }) {
            current.filterNot { it.url == channel.url }
        } else {
            (listOf(channel) + current).take(MAX_FAVORITES)
        }
        prefs.edit().putString(KEY_FAVORITES, encode(next)).apply()
        return next
    }

    fun addRecent(channel: Channel): List<Channel> {
        val next = (listOf(channel) + recent().filterNot { it.url == channel.url }).take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT, encode(next)).apply()
        return next
    }

    fun clearHistory(): List<Channel> {
        prefs.edit().remove(KEY_RECENT).apply()
        return emptyList()
    }

    private fun encode(channels: List<Channel>): String {
        val array = JSONArray()
        channels.forEach { channel ->
            val item = JSONObject()
                .put("name", channel.name)
                .put("url", channel.url)
            channel.logo?.let { item.put("logo", it) }
            channel.group?.let { item.put("group", it) }
            if (channel.headers.isNotEmpty()) {
                val headers = JSONObject()
                channel.headers.forEach { (key, value) -> headers.put(key, value) }
                item.put("headers", headers)
            }
            array.put(item)
        }
        return array.toString()
    }

    private fun decode(raw: String?): List<Channel> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val url = item.optString("url").takeIf { it.isNotBlank() } ?: continue
                    val headers = mutableMapOf<String, String>()
                    item.optJSONObject("headers")?.let { headerObject ->
                        headerObject.keys().forEach { key ->
                            headers[key] = headerObject.optString(key)
                        }
                    }
                    add(
                        Channel(
                            name = item.optString("name").ifBlank { UrlUtils.prettyName(url) },
                            url = url,
                            logo = item.optString("logo").takeIf { it.isNotBlank() },
                            group = item.optString("group").takeIf { it.isNotBlank() },
                            headers = headers,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_RECENT = "recent"
        const val MAX_FAVORITES = 100
        const val MAX_RECENT = 12
    }
}
