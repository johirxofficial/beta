package com.johirxofficial.beta.data

import android.content.Context
import com.johirxofficial.beta.BuildConfig

/** Small SharedPreferences wrapper so the API URL and playback options can change at runtime. */
class Settings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("beta_settings", Context.MODE_PRIVATE)

    var apiUrl: String
        get() = prefs.getString(KEY_API_URL, null)?.trim()?.takeIf { it.isNotEmpty() }
            ?: BuildConfig.DEFAULT_API_URL
        set(value) {
            val cleaned = value.trim()
            if (cleaned.isEmpty()) {
                prefs.edit().remove(KEY_API_URL).apply()
            } else {
                prefs.edit().putString(KEY_API_URL, cleaned).apply()
            }
        }

    val isUsingCustomApi: Boolean
        get() = !prefs.getString(KEY_API_URL, null).isNullOrBlank()

    var autoPlay: Boolean
        get() = prefs.getBoolean(KEY_AUTOPLAY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()
        }

    var showLogos: Boolean
        get() = prefs.getBoolean(KEY_LOGOS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_LOGOS, value).apply()
        }

    /** "system", "dark" or "light". */
    var theme: String
        get() = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) {
            val cleaned = if (value in THEMES) value else THEME_SYSTEM
            prefs.edit().putString(KEY_THEME, cleaned).apply()
        }

    /** 0 = automatic columns, otherwise a fixed 2-6 column grid. */
    var gridColumns: Int
        get() = prefs.getInt(KEY_GRID_COLUMNS, 0).coerceIn(0, 6)
        set(value) {
            prefs.edit().putInt(KEY_GRID_COLUMNS, value.coerceIn(0, 6)).apply()
        }

    /** "grid" or "list" for the home category area. */
    var homeLayout: String
        get() = prefs.getString(KEY_HOME_LAYOUT, LAYOUT_GRID) ?: LAYOUT_GRID
        set(value) {
            val cleaned = if (value in LAYOUTS) value else LAYOUT_GRID
            prefs.edit().putString(KEY_HOME_LAYOUT, cleaned).apply()
        }

    /** "list" or "grid" for channel lists. */
    var channelLayout: String
        get() = prefs.getString(KEY_CHANNEL_LAYOUT, LAYOUT_LIST) ?: LAYOUT_LIST
        set(value) {
            val cleaned = if (value in LAYOUTS) value else LAYOUT_LIST
            prefs.edit().putString(KEY_CHANNEL_LAYOUT, cleaned).apply()
        }

    /** Fixed 2-4 columns used when channelLayout == "grid". */
    var channelGridColumns: Int
        get() = prefs.getInt(KEY_CHANNEL_GRID_COLUMNS, 2).coerceIn(2, 4)
        set(value) {
            prefs.edit().putInt(KEY_CHANNEL_GRID_COLUMNS, value.coerceIn(2, 4)).apply()
        }

    fun resetApiUrl() {
        prefs.edit().remove(KEY_API_URL).apply()
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
        val THEMES = setOf(THEME_SYSTEM, THEME_DARK, THEME_LIGHT)

        const val LAYOUT_GRID = "grid"
        const val LAYOUT_LIST = "list"
        val LAYOUTS = setOf(LAYOUT_GRID, LAYOUT_LIST)

        private const val KEY_API_URL = "api_url"
        private const val KEY_AUTOPLAY = "autoplay"
        private const val KEY_LOGOS = "show_logos"
        private const val KEY_THEME = "theme"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_HOME_LAYOUT = "home_layout"
        private const val KEY_CHANNEL_LAYOUT = "channel_layout"
        private const val KEY_CHANNEL_GRID_COLUMNS = "channel_grid_columns"
    }
}
