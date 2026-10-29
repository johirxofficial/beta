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
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val categories: List<Category> = emptyList(),
    val apiUrl: String = "",
)

data class ChannelsUiState(
    val category: Category? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val channels: List<Channel> = emptyList(),
    val query: String = "",
) {
    val visibleChannels: List<Channel>
        get() = if (query.isBlank()) {
            channels
        } else {
            channels.filter { it.name.contains(query, ignoreCase = true) }
        }
}

class MainViewModel(
    private val repository: Repository,
    private val settings: Settings,
) : ViewModel() {

    var home by mutableStateOf(HomeUiState(apiUrl = settings.apiUrl))
        private set

    var channels by mutableStateOf(ChannelsUiState())
        private set

    var currentChannel by mutableStateOf<Channel?>(null)
        private set

    init {
        loadCategories(force = false)
    }

    fun refreshHome() = loadCategories(force = true)

    fun openCategory(category: Category) {
        channels = ChannelsUiState(category = category, loading = true)
        loadChannels(category, force = false)
    }

    fun reloadChannels() {
        val category = channels.category ?: return
        channels = channels.copy(loading = true, error = null)
        loadChannels(category, force = true)
    }

    fun setQuery(query: String) {
        channels = channels.copy(query = query)
    }

    fun selectChannel(channel: Channel) {
        currentChannel = channel
    }

    fun saveApiUrl(url: String) {
        if (url.isBlank()) return
        settings.apiUrl = url
        repository.invalidate()
        channels = ChannelsUiState()
        home = HomeUiState(apiUrl = settings.apiUrl)
        loadCategories(force = true)
    }

    fun resetApiUrl() {
        settings.resetApiUrl()
        repository.invalidate()
        channels = ChannelsUiState()
        home = HomeUiState(apiUrl = settings.apiUrl)
        loadCategories(force = true)
    }

    private fun loadCategories(force: Boolean) {
        viewModelScope.launch {
            home = if (home.categories.isEmpty()) {
                home.copy(loading = true, refreshing = false, error = null)
            } else {
                home.copy(loading = false, refreshing = true, error = null)
            }

            runCatching { repository.loadCategories(force) }
                .onSuccess { categories ->
                    home = home.copy(
                        loading = false,
                        refreshing = false,
                        error = null,
                        categories = categories,
                        apiUrl = settings.apiUrl,
                    )
                }
                .onFailure { throwable ->
                    home = home.copy(
                        loading = false,
                        refreshing = false,
                        error = throwable.readableMessage(),
                        apiUrl = settings.apiUrl,
                    )
                }
        }
    }

    private fun loadChannels(category: Category, force: Boolean) {
        viewModelScope.launch {
            runCatching { repository.loadChannels(category, force) }
                .onSuccess { list ->
                    channels = channels.copy(loading = false, error = null, channels = list)
                }
                .onFailure { throwable ->
                    channels = channels.copy(loading = false, error = throwable.readableMessage())
                }
        }
    }

    class Factory(
        private val repository: Repository,
        private val settings: Settings,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository, settings) as T
    }
}

private fun Throwable.readableMessage(): String = when (this) {
    is UnknownHostException -> "Server paoa jayni. Internet connection ba API URL check korun."
    is SocketTimeoutException -> "Server response dite deri korche. Abar try korun."
    is IOException -> message ?: "Network error"
    else -> message ?: this::class.java.simpleName
}
