package com.johirxofficial.x.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.johirxofficial.x.data.Channel
import com.johirxofficial.x.ui.components.EmptyState
import com.johirxofficial.x.ui.components.ErrorState
import com.johirxofficial.x.ui.components.Pill
import com.johirxofficial.x.ui.components.TruncatedMeta
import com.johirxofficial.x.ui.theme.Accent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChannelsScreen(
    state: ChannelsUiState,
    layout: LayoutInfo,
    showLogos: Boolean,
    favorites: List<Channel>,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onGroupChange: (String?) -> Unit,
    onRetry: () -> Unit,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
) {
    val groups = remember(state.channels) {
        state.channels
            .mapNotNull { it.group?.trim()?.takeIf { group -> group.isNotEmpty() } }
            .distinct()
            .sorted()
    }

    val visible = remember(state.channels, state.query, state.group) {
        val query = state.query.trim()
        state.channels.filter { channel ->
            (state.group == null || channel.group == state.group) &&
                (
                    query.isEmpty() ||
                        channel.name.contains(query, ignoreCase = true) ||
                        channel.group?.contains(query, ignoreCase = true) == true
                    )
        }
    }

    val grouped = remember(visible, state.group) {
        if (state.group != null) {
            linkedMapOf(state.group to visible)
        } else {
            visible.groupBy { it.group?.takeIf { group -> group.isNotBlank() } ?: "Other" }
        }
    }

    val favoriteUrls = remember(favorites) { favorites.map { it.url }.toHashSet() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.category?.name ?: "Channels",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                if (state.channels.isNotEmpty()) {
                                    append(formatCount(state.channels.size))
                                    append(" channels")
                                    if (groups.size > 1) {
                                        append(" · ")
                                        append(groups.size)
                                        append(" groups")
                                    }
                                } else {
                                    append("Fresh from playlist")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search channel or group") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = layout.padding.dp, vertical = 6.dp),
            )

            if (groups.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = layout.padding.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "chip_all") {
                        FilterChip(
                            selected = state.group == null,
                            onClick = { onGroupChange(null) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent.copy(alpha = 0.2f),
                                selectedLabelColor = Accent,
                            ),
                        )
                    }
                    items(groups, key = { "chip_$it" }) { group ->
                        FilterChip(
                            selected = state.group == group,
                            onClick = { onGroupChange(if (state.group == group) null else group) },
                            label = { Text(group, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent.copy(alpha = 0.2f),
                                selectedLabelColor = Accent,
                            ),
                        )
                    }
                }
            }

            when {
                state.loading && state.channels.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null && state.channels.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorState(message = state.error.orEmpty(), onRetry = onRetry)
                    }
                }

                visible.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = if (state.query.isBlank()) "Kono channel nei" else "Kichu paoa jayni",
                            subtitle = if (state.query.isBlank()) {
                                "Playlist ta khali ba server bondho"
                            } else {
                                "Onno naam diye khujun"
                            },
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = layout.padding.dp,
                            end = layout.padding.dp,
                            top = 4.dp,
                            bottom = 20.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        grouped.forEach { (group, channels) ->
                            stickyHeader(key = "header_$group") {
                                GroupHeader(name = group, count = channels.size)
                            }
                            itemsIndexed(
                                items = channels,
                                key = { index, channel -> "ch_${group}_${index}_${channel.url.hashCode()}" },
                            ) { _, channel ->
                                ChannelRow(
                                    channel = channel,
                                    showLogo = showLogos,
                                    isFavorite = favoriteUrls.contains(channel.url),
                                    onClick = { onPlay(channel) },
                                    onToggleFavorite = { onToggleFavorite(channel) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(name: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Accent,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    showLogo: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.01f else 1f, label = "channelScale")
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                if (focused) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surface,
            )
            .border(
                width = if (focused) 1.5.dp else 0.dp,
                color = if (focused) Accent else Color.Transparent,
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelLogo(channel = channel, showLogo = showLogo)

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                channel.quality?.let { quality -> Pill(text = quality) }
                TruncatedMeta(
                    text = channel.group?.takeIf { it.isNotBlank() }
                        ?: channel.url.substringBefore('?'),
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                tint = if (isFavorite) Accent else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ChannelLogo(channel: Channel, showLogo: Boolean) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = channel.name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Accent,
        )
        val logo = channel.logo
        if (showLogo && !logo.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logo)
                    .size(92, 92)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
