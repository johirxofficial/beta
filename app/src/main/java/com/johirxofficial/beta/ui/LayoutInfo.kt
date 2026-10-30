package com.johirxofficial.beta.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

data class LayoutInfo(
    val columns: Int,
    val isTv: Boolean,
    val widthDp: Int,
    val padding: Int,
)

/** One source of truth for phone / tablet / foldable / TV / desktop layout decisions. */
@Composable
@ReadOnlyComposable
fun rememberLayoutInfo(): LayoutInfo {
    val configuration = LocalConfiguration.current
    val isTv = (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val width = configuration.screenWidthDp
    val columns = when {
        width >= 1280 -> 6
        width >= 960 -> 5
        width >= 720 -> 4
        width >= 560 -> 3
        else -> 2
    }
    val padding = when {
        width >= 960 -> 32
        width >= 720 -> 24
        else -> 16
    }
    return LayoutInfo(columns = columns, isTv = isTv, widthDp = width, padding = padding)
}
