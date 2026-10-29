package com.johirxofficial.x.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.johirxofficial.x.data.Category
import com.johirxofficial.x.data.Channel
import com.johirxofficial.x.data.Repository
import com.johirxofficial.x.data.Settings
import com.johirxofficial.x.data.UrlUtils
import com.johirxofficial.x.data.UserDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val categories: List<Category> = emptyList(),
    val favorites: List<Channel> = emptyList(),
    val recent: List<Channel> = emptyList(),
    val apiUrl: String = "",
    val apiHost: String = "",
    val autoPlay: Boolean = true,
    val showLogos: Boolean = true,
    val message: String? = null,
)

data class ChannelsUiState(
    val category: Category? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val channels: List<Channel> = emptyList(),
    val query: String = "",
    val group: String? = null,
)

class MainViewModel(
    private val repository: Repository,
    private val settings: Settings,
    private val userData: UserDataStore,
) : ViewModel() {

    var home by mutableStateOf(
        HomeUiState(
            apiUrl = settings.apiUrl,
            apiHost = UrlUtils.hostOf(settings.apiUrl),
            autoPlay = settings.autoPlay,
            showLogos = settings.showLogos,
            favorites = userData.favorites(),
            recent = userData.recent(),
        ),
    )
        private set

    var channels by mutableStateOf(ChannelsUiState())
        private set

    var currentChannel by mutableStateOf<Channel?>(null)
        private set

    init {
        loadCategories()
    }

    fun refresh() = loadCategories()

    fun openCategory(category: Category) {
        channels = ChannelsUiState(category = category, loading = true)
        loadChannels(category)
    }

    fun retryChannels() {
        val category = channels.category ?: return
        channels = channels.copy(loading = true, error = null)
        loadChannels(category)
    }

    fun setQuery(query: String) {
        channels = channels.copy(query = query)
    }

    fun setGroup(group: String?) {
        channels = channels.copy(group = group)
    }

    fun play(channel: Channel) {
        currentChannel = channel
        home = home.copy(recent = userData.addRecent(channel))
    }

    fun toggleFavorite(channel: Channel) {
        val favorites = userData.toggleFavorite(channel)
        val added = favorites.any { it.url == channel.url }
        home = home.copy(
            favorites = favorites,
            message = if (added) "${channel.name} — favorite e add holo" else "Favorite theke bad jolo",
        )
    }

    fun saveApiUrl(url: String) {
        if (url.isBlank()) return
        settings.apiUrl = url
        restartCatalog()
    }

    fun resetApiUrl() {
        settings.resetApiUrl()
        restartCatalog()
    }

    fun setAutoPlay(enabled: Boolean) {
        settings.autoPlay = enabled
        home = home.copy(autoPlay = enabled)
    }

    fun setShowLogos(enabled: Boolean) {
        settings.showLogos = enabled
        home = home.copy(showLogos = enabled)
    }

    fun clearHistory() {
        home = home.copy(recent = userData.clearHistory(), message = "History clear hoye gese")
    }

    fun consumeMessage() {
        if (home.message != null) home = home.copy(message = null)
    }

    private fun restartCatalog() {
        channels = ChannelsUiState()
        home = home.copy(
            categories = emptyList(),
            loading = true,
            refreshing = false,
            error = null,
            apiUrl = settings.apiUrl,
            apiHost = UrlUtils.hostOf(settings.apiUrl),
        )
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            home = if (home.categories.isEmpty()) {
                home.copy(loading = true, refreshing = false, error = null)
            } else {
                home.copy(loading = false, refreshing = true, error = null)
            }

            try {
                val categories = repository.categories(settings.apiUrl)
                home = home.copy(
                    loading = false,
                    refreshing = false,
                    error = null,
                    categories = categories,
                    apiUrl = settings.apiUrl,
                    apiHost = UrlUtils.hostOf(settings.apiUrl),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                val message = throwable.readableMessage()
                home = home.copy(
                    loading = false,
                    refreshing = false,
                    error = message,
                    message = if (home.categories.isEmpty()) null else message,
                    apiUrl = settings.apiUrl,
                    apiHost = UrlUtils.hostOf(settings.apiUrl),
                )
            }
        }
    }

    private fun loadChannels(category: Category) {
        viewModelScope.launch {
            try {
                val channels = repository.channels(category)
                this@MainViewModel.channels = this@MainViewModel.channels.copy(
                    loading = false,
                    error = null,
                    channels = channels,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                this@MainViewModel.channels = this@MainViewModel.channels.copy(
                    loading = false,
                    error = throwable.readableMessage(),
                )
            }
        }
    }

    class Factory(
        private val repository: Repository,
        private val settings: Settings,
        private val userData: UserDataStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository, settings, userData) as T
    }
}

private fun Throwable.readableMessage(): String = when (this) {
    is UnknownHostException -> "Server paoa jayni — internet ba API URL check korun."
    is SocketTimeoutException -> "Server response dite deri korche — abar try korun."
    is IOException -> message ?: "Network problem"
    else -> message ?: this::class.java.simpleName
}
