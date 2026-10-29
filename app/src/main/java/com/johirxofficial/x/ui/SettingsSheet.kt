package com.johirxofficial.x.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johirxofficial.x.BuildConfig

data class SettingsActions(
    val saveApiUrl: (String) -> Unit,
    val resetApiUrl: () -> Unit,
    val setAutoPlay: (Boolean) -> Unit,
    val setShowLogos: (Boolean) -> Unit,
    val reload: () -> Unit,
    val clearHistory: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    apiUrl: String,
    autoPlay: Boolean,
    showLogos: Boolean,
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

            SettingSwitch(
                title = "Auto play",
                subtitle = "Channel khule songe songe play hobe",
                checked = autoPlay,
                onCheckedChange = actions.setAutoPlay,
            )
            SettingSwitch(
                title = "Channel logos",
                subtitle = "Bondho korle list aro fast chole, data bache",
                checked = showLogos,
                onCheckedChange = actions.setShowLogos,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                text = "JSON API",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Category list ei URL theke ashe. Apnar nijer server er link bosate paren.",
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
                text = "Kono cache nei — protibar API theke fresh data ashe.",
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
            Text(
                text = "X live · v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
            )
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
                Text("github.com/${BuildConfig.DEVELOPER}/x")
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
