package com.johirxofficial.x.data

/** A single playable stream. */
data class Channel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    /** Extra HTTP headers required by the stream server (Referer, User-Agent, Cookie...). */
    val headers: Map<String, String> = emptyMap(),
)

/** A category shown on the home screen. */
data class Category(
    val id: String,
    val name: String,
    val logo: String? = null,
    /** When set, channels are downloaded and parsed from this M3U/JSON playlist. */
    val playlistUrl: String? = null,
    /** When set, channels are already inside the categories JSON. */
    val inlineChannels: List<Channel> = emptyList(),
)
