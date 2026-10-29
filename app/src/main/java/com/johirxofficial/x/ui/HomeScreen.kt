package com.johirxofficial.x.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed as rowItemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.johirxofficial.x.BuildConfig
import com.johirxofficial.x.data.Category
import com.johirxofficial.x.data.Channel
import com.johirxofficial.x.ui.components.ErrorState
import com.johirxofficial.x.ui.components.Pill
import com.johirxofficial.x.ui.components.SectionHeader
import com.johirxofficial.x.ui.components.SourceDot
import com.johirxofficial.x.ui.theme.Accent

private val CategoryGradients = listOf(
    listOf(Color(0xFF0E7490), Color(0xFF083344)),
    listOf(Color(0xFF7C3AED), Color(0xFF312E81)),
    listOf(Color(0xFFB45309), Color(0xFF451A03)),
    listOf(Color(0xFF15803D), Color(0xFF052E16)),
    listOf(Color(0xFFBE123C), Color(0xFF4C0519)),
    listOf(Color(0xFF2563EB), Color(0xFF172554)),
    listOf(Color(0xFF0891B2), Color(0xFF164E63)),
    listOf(Color(0xFF9333EA), Color(0xFF4A044E)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    layout: LayoutInfo,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenCategory: (Category) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onMessageShown: () -> Unit,
    settingsActions: SettingsActions,
) {
    var showSettings by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHost.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "X",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = "live · open source",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.loading && state.categories.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null && state.categories.isEmpty() -> {
                    ErrorState(
                        message = state.error.orEmpty(),
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    HomeContent(
                        state = state,
                        layout = layout,
                        onOpenCategory = onOpenCategory,
                        onPlayChannel = onPlayChannel,
                        onToggleFavorite = onToggleFavorite,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }

            if (state.refreshing) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            apiUrl = state.apiUrl,
            autoPlay = state.autoPlay,
            showLogos = state.showLogos,
            onDismiss = { showSettings = false },
            actions = settingsActions,
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    layout: LayoutInfo,
    onOpenCategory: (Category) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val totalChannels = remember(state.categories) {
        state.categories.sumOf { it.channelCountHint ?: it.inlineChannels.size }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(layout.columns),
        contentPadding = PaddingValues(
            start = layout.padding.dp,
            end = layout.padding.dp,
            top = 4.dp,
            bottom = 28.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroCard(
                categoryCount = state.categories.size,
                channelCount = totalChannels,
                apiHost = state.apiHost,
            )
        }

        if (state.favorites.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionHeader("Favorites", trailing = "${state.favorites.size} saved")
                    ChannelStrip(
                        channels = state.favorites,
                        favorites = state.favorites,
                        onPlay = onPlayChannel,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
        }

        if (state.recent.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionHeader("Continue watching")
                    ChannelStrip(
                        channels = state.recent,
                        favorites = state.favorites,
                        onPlay = onPlayChannel,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader("Categories", trailing = "${state.categories.size}")
        }

        itemsIndexed(
            items = state.categories,
            key = { index, category -> "${category.id}_$index" },
        ) { index, category ->
            CategoryCard(
                category = category,
                index = index,
                onClick = { onOpenCategory(category) },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Footer(apiHost = state.apiHost, onOpenSettings = onOpenSettings)
        }
    }
}

@Composable
fun BrandMark(size: Dp = 34.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(
                Brush.linearGradient(
                    listOf(Accent, Color(0xFF3B82F6)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "X",
            color = Color(0xFF04121A),
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.52f).sp,
        )
    }
}

@Composable
private fun HeroCard(categoryCount: Int, channelCount: Int, apiHost: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0B3C47), Color(0xFF0A1620), Color(0xFF070B10)),
                ),
            )
            .border(1.dp, Accent.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Text(
            text = "X",
            fontSize = 96.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.05f),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "X",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(8.dp))
                    Pill("LIVE")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(formatCount(categoryCount))
                        append(" categories")
                        if (channelCount > 0) {
                            append(" · ")
                            append(formatCount(channelCount))
                            append(" channels")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (apiHost.isBlank()) "Fresh from API · no cache" else "Fresh · $apiHost",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChannelStrip(
    channels: List<Channel>,
    favorites: List<Channel>,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        rowItemsIndexed(items = channels, key = { index, channel -> "strip_${index}_${channel.url.hashCode()}" }) { _, channel ->
            MiniChannelCard(
                channel = channel,
                isFavorite = favorites.any { it.url == channel.url },
                onClick = { onPlay(channel) },
                onToggleFavorite = { onToggleFavorite(channel) },
            )
        }
    }
}

@Composable
private fun MiniChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(172.dp).height(78.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LetterBadge(text = channel.name, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.quality ?: channel.group ?: "Live",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite) Accent else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun LetterBadge(text: String, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Accent,
        )
    }
}

@Composable
private fun CategoryCard(category: Category, index: Int, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.03f else 1f, label = "categoryScale")
    val shape = RoundedCornerShape(20.dp)
    val gradient = CategoryGradients[index % CategoryGradients.size]

    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.32f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Accent else Color.White.copy(alpha = 0.06f),
                shape = shape,
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(14.dp),
        ) {
            val logoUrl = category.logo
            if (!logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(logoUrl)
                        .size(120, 120)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(6.dp)),
                )
            } else {
                Text(
                    text = category.name.take(1).uppercase(),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val count = category.channelCountHint
                    if (count != null && count > 0) {
                        Text(
                            text = "${formatCount(count)} ch",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }
                    category.type?.let { type ->
                        Pill(
                            text = type,
                            color = Color.White.copy(alpha = 0.9f),
                            background = Color.White.copy(alpha = 0.14f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Footer(apiHost: String, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceDot()
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "v${BuildConfig.VERSION_NAME} · ${BuildConfig.DEVELOPER}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (apiHost.isBlank()) "No cache — always fresh" else "$apiHost · no cache, always fresh",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onOpenSettings) { Text("Settings") }
    }
}

fun formatCount(value: Int): String = "%,d".format(value)
