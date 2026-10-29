package com.johirxofficial.x.data

import android.content.Context
import com.johirxofficial.x.BuildConfig

/** Small SharedPreferences wrapper so the API URL can be changed without rebuilding. */
class Settings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("x_settings", Context.MODE_PRIVATE)

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

    fun resetApiUrl() {
        prefs.edit().remove(KEY_API_URL).apply()
    }

    private companion object {
        const val KEY_API_URL = "api_url"
    }
}
