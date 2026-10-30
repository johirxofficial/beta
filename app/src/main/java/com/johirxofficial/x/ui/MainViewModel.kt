package com.johirxofficial.x.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.johirxofficial.x.BuildConfig
import com.johirxofficial.x.data.AppUpdate
import com.johirxofficial.x.data.Category
import com.johirxofficial.x.data.Channel
import com.johirxofficial.x.data.Repository
import com.johirxofficial.x.data.Settings
import com.johirxofficial.x.data.UpdateManager
import com.johirxofficial.x.data.UrlUtils
import com.johirxofficial.x.data.UserDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
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
    val notices: List<String> = emptyList(),
    val update: AppUpdate? = null,
)

data class ChannelsUiState(
    val category: Category? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val channels: List<Channel> = emptyList(),
    val query: String = "",
    val group: String? = null,
)

data class UpdateUiState(
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val file: File? = null,
    val error: String? = null,
    val message: String? = null,
)

class MainViewModel(
    private val repository: Repository,
    private val settings: Settings,
    private val userData: UserDataStore,
    private val updateManager: UpdateManager,
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

    var updateState by mutableStateOf(UpdateUiState())
        private set

    val updateAvailable: Boolean
        get() {
            val update = home.update ?: return false
            if (update.isNewerThan(BuildConfig.VERSION_CODE)) return true
            val latest = update.latestVersion?.trim()?.removePrefix("v") ?: return false
            return latest.isNotEmpty() && latest != BuildConfig.VERSION_NAME
        }

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
            message = if (added) "${channel.name} — favorite e add holo" else "Favorite theke bad golo",
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

    fun downloadUpdate() {
        val update = home.update ?: return
        val apkUrl = update.apkUrl ?: run {
            updateState = UpdateUiState(error = "Update link nei — API te apk_url add korun.")
            return
        }
        if (updateState.downloading) return

        updateState = UpdateUiState(downloading = true)
        viewModelScope.launch {
            try {
                val file = updateManager.download(apkUrl) { progress ->
                    updateState = updateState.copy(progress = progress)
                }
                updateState = UpdateUiState(file = file, progress = 1f)
                updateManager.install(file, apkUrl)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                updateState = UpdateUiState(error = throwable.readableMessage())
            }
        }
    }

    fun installUpdate() {
        val file = updateState.file ?: return
        val started = updateManager.install(file, home.update?.apkUrl)
        updateState = updateState.copy(
            message = if (started) null else "Installer khule ni — browser theke download korun.",
        )
    }

    fun openUpdateLink() {
        val url = home.update?.apkUrl ?: "${BuildConfig.PROJECT_URL}/releases/latest"
        updateManager.openInBrowser(url)
    }

    fun consumeUpdateMessage() {
        if (updateState.message != null || updateState.error != null) {
            updateState = updateState.copy(message = null, error = null)
        }
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
                val catalog = repository.catalog(settings.apiUrl)
                home = home.copy(
                    loading = false,
                    refreshing = false,
                    error = null,
                    categories = catalog.categories,
                    notices = catalog.notices,
                    update = catalog.update,
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
                val loaded = repository.channels(category)
                this@MainViewModel.channels = this@MainViewModel.channels.copy(
                    loading = false,
                    error = null,
                    channels = loaded,
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
        private val updateManager: UpdateManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository, settings, userData, updateManager) as T
    }
}

private fun Throwable.readableMessage(): String = when (this) {
    is UnknownHostException -> "Server paoa jayni — internet ba API URL check korun."
    is SocketTimeoutException -> "Server response dite deri korche — abar try korun."
    is IOException -> message ?: "Network problem"
    else -> message ?: this::class.java.simpleName
}
