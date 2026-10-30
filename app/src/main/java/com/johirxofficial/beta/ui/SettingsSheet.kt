package com.johirxofficial.beta.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johirxofficial.beta.BuildConfig
import com.johirxofficial.beta.R
import com.johirxofficial.beta.data.AppUpdate
import com.johirxofficial.beta.data.Settings
import com.johirxofficial.beta.ui.theme.Accent

data class SettingsActions(
    val saveApiUrl: (String) -> Unit,
    val resetApiUrl: () -> Unit,
    val setAutoPlay: (Boolean) -> Unit,
    val setShowLogos: (Boolean) -> Unit,
    val setTheme: (String) -> Unit,
    val setGridColumns: (Int) -> Unit,
    val setHomeLayout: (String) -> Unit,
    val setChannelLayout: (String) -> Unit,
    val setChannelGridColumns: (Int) -> Unit,
    val reload: () -> Unit,
    val clearHistory: () -> Unit,
    val downloadUpdate: () -> Unit,
    val installUpdate: () -> Unit,
    val openUpdateLink: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    apiUrl: String,
    autoPlay: Boolean,
    showLogos: Boolean,
    theme: String,
    gridColumns: Int,
    homeLayout: String,
    channelLayout: String,
    channelGridColumns: Int,
    update: AppUpdate?,
    updateAvailable: Boolean,
    updateState: UpdateUiState,
    onDismiss: () -> Unit,
    actions: SettingsActions,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var url by remember(apiUrl) { mutableStateOf(apiUrl) }
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "App layout",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    Settings.THEME_SYSTEM to "System",
                    Settings.THEME_DARK to "Dark",
                    Settings.THEME_LIGHT to "Light",
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = theme == value,
                        onClick = { actions.setTheme(value) },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                text = "Home layout",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    Settings.LAYOUT_GRID to "Grid",
                    Settings.LAYOUT_LIST to "List",
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = homeLayout == value,
                        onClick = { actions.setHomeLayout(value) },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                text = "Home grid columns",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val options = listOf(0 to "Auto", 2 to "2", 3 to "3", 4 to "4", 5 to "5", 6 to "6")
                options.forEach { (value, label) ->
                    FilterChip(
                        selected = gridColumns == value,
                        onClick = { actions.setGridColumns(value) },
                        label = { Text(label) },
                        enabled = homeLayout == Settings.LAYOUT_GRID,
                    )
                }
            }
            Text(
                text = "Use List for one wide category per row, or Grid with 2–6 cards per row. Auto follows the screen size.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Channel layout",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    Settings.LAYOUT_LIST to "List",
                    Settings.LAYOUT_GRID to "Grid",
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = channelLayout == value,
                        onClick = { actions.setChannelLayout(value) },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                text = "Channel grid columns",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(2, 3, 4).forEach { value ->
                    FilterChip(
                        selected = channelGridColumns == value,
                        onClick = { actions.setChannelGridColumns(value) },
                        label = { Text(value.toString()) },
                        enabled = channelLayout == Settings.LAYOUT_GRID,
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            SettingSwitch(
                title = "Auto play",
                subtitle = "Start playing as soon as a channel opens",
                checked = autoPlay,
                onCheckedChange = actions.setAutoPlay,
            )
            SettingSwitch(
                title = "Channel logos",
                subtitle = "Turn off to load channel lists faster and use less data",
                checked = showLogos,
                onCheckedChange = actions.setShowLogos,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                text = "App update",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Current v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val latest = update?.latestVersion
                if (!latest.isNullOrBlank()) {
                    Text(
                        text = "Latest $latest",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (updateAvailable) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            val updateNote = update?.message
            if (!updateNote.isNullOrBlank()) {
                Text(
                    text = updateNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                updateState.downloading -> {
                    LinearProgressIndicator(
                        progress = { updateState.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Downloading ${(updateState.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                updateState.file != null -> {
                    Button(
                        onClick = actions.installUpdate,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Install downloaded APK") }
                    Text(
                        text = "Download complete — tap Install to update the app.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                updateAvailable -> {
                    Button(
                        onClick = actions.downloadUpdate,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !update?.apkUrl.isNullOrBlank(),
                    ) { Text("Update now") }
                    val size = update?.sizeMb
                    if (size != null) {
                        Text(
                            text = "APK size: $size MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                update == null -> {
                    Text(
                        text = "No update information in the API yet. New versions will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Text(
                        text = "You are on the latest version.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Accent,
                    )
                }
            }

            updateState.error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            updateState.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TextButton(onClick = actions.openUpdateLink) {
                Text("Open download page")
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                text = "JSON API",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Categories and channels load from this URL. You can use your own server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("API URL") },
                placeholder = { Text("https://example.com/categories.json") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        actions.saveApiUrl(url)
                        onDismiss()
                    },
                    enabled = url.isNotBlank(),
                ) { Text("Save") }
                TextButton(onClick = { url = BuildConfig.DEFAULT_API_URL }) { Text("My server") }
                TextButton(onClick = { url = BuildConfig.FALLBACK_API_URL }) { Text("Public") }
            }
            TextButton(onClick = {
                actions.resetApiUrl()
                onDismiss()
            }) { Text("Reset to default") }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                text = "Storage",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "No cache — every reload fetches fresh data from the API.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = actions.reload) { Text("Reload now") }
                TextButton(onClick = actions.clearHistory) { Text("Clear history") }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = "beta logo",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp)),
                )
                Column {
                    Text(
                        text = "beta · v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "watch live",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "Developer: ${BuildConfig.DEVELOPER}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Open source · MIT License · Kotlin + Jetpack Compose + Media3",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { uriHandler.openUri(BuildConfig.PROJECT_URL) }) {
                Text("github.com/${BuildConfig.DEVELOPER}/beta")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
