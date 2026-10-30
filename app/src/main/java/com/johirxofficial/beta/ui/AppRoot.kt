package com.johirxofficial.beta.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.johirxofficial.beta.data.Channel

private object Routes {
    const val HOME = "home"
    const val CHANNELS = "channels"
    const val PLAYER = "player"
}

@Composable
fun AppRoot(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val layout = rememberLayoutInfo()
    val context = LocalContext.current

    val settingsActions = SettingsActions(
        saveApiUrl = viewModel::saveApiUrl,
        resetApiUrl = viewModel::resetApiUrl,
        setAutoPlay = viewModel::setAutoPlay,
        setShowLogos = viewModel::setShowLogos,
        setTheme = viewModel::setTheme,
        setGridColumns = viewModel::setGridColumns,
        setHomeLayout = viewModel::setHomeLayout,
        setChannelLayout = viewModel::setChannelLayout,
        setChannelGridColumns = viewModel::setChannelGridColumns,
        reload = {
            navController.popBackStack(Routes.HOME, inclusive = false)
            viewModel.refresh()
        },
        clearHistory = viewModel::clearHistory,
        downloadUpdate = viewModel::downloadUpdate,
        installUpdate = viewModel::installUpdate,
        openUpdateLink = viewModel::openUpdateLink,
    )

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                state = viewModel.home,
                layout = layout,
                updateAvailable = viewModel.updateAvailable,
                updateState = viewModel.updateState,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::refresh,
                onClose = { context.findActivity()?.finish() },
                onOpenCategory = { category ->
                    viewModel.openCategory(category)
                    navController.navigate(Routes.CHANNELS)
                },
                onPlayChannel = { channel ->
                    viewModel.play(channel)
                    navController.navigate(Routes.PLAYER)
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onMessageShown = viewModel::consumeMessage,
                settingsActions = settingsActions,
            )
        }

        composable(Routes.CHANNELS) {
            ChannelsScreen(
                state = viewModel.channels,
                layout = layout,
                showLogos = viewModel.home.showLogos,
                favorites = viewModel.home.favorites,
                channelLayout = viewModel.home.channelLayout,
                channelGridColumns = viewModel.home.channelGridColumns,
                onBack = { navController.popBackStack() },
                onQueryChange = viewModel::setQuery,
                onGroupChange = viewModel::setGroup,
                onRetry = viewModel::retryChannels,
                onPlay = { channel: Channel ->
                    viewModel.play(channel)
                    navController.navigate(Routes.PLAYER)
                },
                onToggleFavorite = viewModel::toggleFavorite,
            )
        }

        composable(Routes.PLAYER) {
            val channel = viewModel.currentChannel
            if (channel == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val currentList = viewModel.channels.channels
                val channelList = remember(currentList, channel, viewModel.home.favorites, viewModel.home.recent) {
                    if (currentList.any { it.url == channel.url }) {
                        currentList
                    } else {
                        (listOf(channel) + viewModel.home.favorites + viewModel.home.recent)
                            .distinctBy { it.url }
                    }
                }
                PlayerScreen(
                    channel = channel,
                    channels = channelList,
                    autoPlay = viewModel.home.autoPlay,
                    isFavorite = viewModel.home.favorites.any { it.url == channel.url },
                    allowRotation = !layout.isTv,
                    onSelectChannel = viewModel::play,
                    onToggleFavorite = { viewModel.toggleFavorite(channel) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
