package com.johirxofficial.x.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.johirxofficial.x.data.Channel

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
                PlayerScreen(
                    channel = channel,
                    autoPlay = viewModel.home.autoPlay,
                    isFavorite = viewModel.home.favorites.any { it.url == channel.url },
                    onToggleFavorite = { viewModel.toggleFavorite(channel) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
