package com.johirxofficial.x.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object Routes {
    const val HOME = "home"
    const val CHANNELS = "channels"
    const val PLAYER = "player"
}

@Composable
fun AppRoot(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                state = viewModel.home,
                onRefresh = viewModel::refreshHome,
                onRetry = viewModel::refreshHome,
                onOpenCategory = { category ->
                    viewModel.openCategory(category)
                    navController.navigate(Routes.CHANNELS)
                },
                onSaveApiUrl = viewModel::saveApiUrl,
                onResetApiUrl = viewModel::resetApiUrl,
            )
        }

        composable(Routes.CHANNELS) {
            ChannelsScreen(
                state = viewModel.channels,
                onBack = { navController.popBackStack() },
                onQueryChange = viewModel::setQuery,
                onRetry = viewModel::reloadChannels,
                onPlay = { channel ->
                    viewModel.selectChannel(channel)
                    navController.navigate(Routes.PLAYER)
                },
            )
        }

        composable(Routes.PLAYER) {
            val channel = viewModel.currentChannel
            if (channel == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                PlayerScreen(channel = channel, onBack = { navController.popBackStack() })
            }
        }
    }
}
