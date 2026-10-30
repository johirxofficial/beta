package com.johirxofficial.x.ui

import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.johirxofficial.x.data.Channel
import com.johirxofficial.x.data.UrlUtils
import com.johirxofficial.x.ui.theme.Accent
import com.johirxofficial.x.ui.theme.SurfaceDark
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private const val KEY_DPAD_CENTER = 23
private const val KEY_ENTER = 66
private const val KEY_SPACE = 62
private const val KEY_DPAD_LEFT = 21
private const val KEY_DPAD_RIGHT = 22
private const val KEY_DPAD_UP = 19
private const val KEY_DPAD_DOWN = 20
private const val KEY_MENU = 82
private const val KEY_C = 31
private const val KEY_L = 40
private const val KEY_MEDIA_PLAY_PAUSE = 85
private const val KEY_CHANNEL_UP = 166
private const val KEY_CHANNEL_DOWN = 167

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    channels: List<Channel>,
    autoPlay: Boolean,
    isFavorite: Boolean,
    allowRotation: Boolean,
    onSelectChannel: (Channel) -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val canUsePip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val errorMessage = remember(channel.url) { mutableStateOf<String?>(null) }
    val playbackState = remember(channel.url) { mutableIntStateOf(Player.STATE_IDLE) }
    var resizeMode by remember(channel.url) { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var attempt by remember(channel.url) { mutableIntStateOf(0) }
    var controlsVisible by remember(channel.url) { mutableStateOf(true) }
    var interaction by remember(channel.url) { mutableIntStateOf(0) }
    var showChannels by remember(channel.url) { mutableStateOf(false) }
    var isPlaying by remember(channel.url) { mutableStateOf(false) }
    var isMuted by remember(channel.url) { mutableStateOf(false) }
    var positionMs by remember(channel.url) { mutableLongStateOf(0L) }
    var durationMs by remember(channel.url) { mutableLongStateOf(0L) }

    val rootFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }
    val channelsFocus = remember { FocusRequester() }

    val player = remember(channel.url, channel.headers, attempt, autoPlay) {
        createPlayer(context, channel, autoPlay, errorMessage, playbackState).apply {
            setMediaItem(
                MediaItem.Builder()
                    .setUri(channel.url)
                    .setMimeType(mimeTypeFor(channel.url))
                    .build(),
            )
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState.intValue = state
                if (state == Player.STATE_READY) {
                    val duration = runCatching { player.duration }.getOrDefault(0L)
                    durationMs = if (duration > 0L && duration != C.TIME_UNSET) duration else 0L
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            runCatching { positionMs = player.currentPosition.coerceAtLeast(0L) }
            runCatching {
                val duration = player.duration
                if (duration > 0L && duration != C.TIME_UNSET) durationMs = duration
            }
            delay(500)
        }
    }

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    DisposableEffect(activity, allowRotation) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val previousOrientation = activity?.requestedOrientation
        if (allowRotation) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (allowRotation) {
                activity?.requestedOrientation =
                    previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    val revealControls: () -> Unit = {
        controlsVisible = true
        interaction += 1
    }

    val togglePlay: () -> Unit = {
        if (player.isPlaying) player.pause() else player.play()
        revealControls()
    }

    val seekBy: (Long) -> Unit = { delta ->
        val duration = runCatching { player.duration }.getOrDefault(0L)
        val target = (runCatching { player.currentPosition }.getOrDefault(0L) + delta)
            .coerceAtLeast(0L)
        val limited = if (duration > 0L && duration != C.TIME_UNSET) {
            target.coerceAtMost(duration)
        } else {
            target
        }
        runCatching { player.seekTo(limited) }
        revealControls()
    }

    val changeChannel: (Int) -> Unit = { step ->
        if (channels.isNotEmpty()) {
            val index = channels.indexOfFirst { it.url == channel.url }
            val base = if (index >= 0) index else 0
            val target = channels[(base + step + channels.size) % channels.size]
            if (target.url != channel.url) onSelectChannel(target)
        }
        showChannels = false
    }

    val toggleMute: () -> Unit = {
        isMuted = !isMuted
        player.volume = if (isMuted) 0f else 1f
        revealControls()
    }

    val cycleAspect: () -> Unit = {
        resizeMode = nextAspect(resizeMode)
        revealControls()
    }

    val toggleRotation: () -> Unit = {
        activity?.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        revealControls()
    }

    val enterPip: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            runCatching { activity?.enterPictureInPictureMode(params) }
        }
    }

    BackHandler {
        if (showChannels) {
            showChannels = false
        } else {
            onBack()
        }
    }

    LaunchedEffect(controlsVisible, interaction, isPlaying, showChannels, errorMessage.value) {
        if (controlsVisible && isPlaying && !showChannels && errorMessage.value == null) {
            delay(4_000)
            controlsVisible = false
        }
    }

    LaunchedEffect(controlsVisible, showChannels) {
        if (showChannels) return@LaunchedEffect
        runCatching {
            if (controlsVisible) playFocus.requestFocus() else rootFocus.requestFocus()
        }
    }

    LaunchedEffect(errorMessage.value) {
        if (errorMessage.value != null) {
            controlsVisible = false
            showChannels = false
            delay(60)
            runCatching { retryFocus.requestFocus() }
        }
    }

    LaunchedEffect(showChannels) {
        if (showChannels) {
            delay(60)
            runCatching { channelsFocus.requestFocus() }
        }
    }

    val currentIndex = channels.indexOfFirst { it.url == channel.url }
    val channelName = displayName(channel.name)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                // Let the error dialog's buttons own D-pad focus while it is visible.
                if (errorMessage.value != null) return@onPreviewKeyEvent false
                val code = event.nativeKeyEvent.keyCode
                if (controlsVisible) interaction += 1
                when {
                    code == KEY_CHANNEL_UP -> {
                        revealControls()
                        changeChannel(1)
                        true
                    }

                    code == KEY_CHANNEL_DOWN -> {
                        revealControls()
                        changeChannel(-1)
                        true
                    }

                    code == KEY_MENU || code == KEY_C || code == KEY_L -> {
                        revealControls()
                        showChannels = !showChannels
                        true
                    }

                    code == KEY_MEDIA_PLAY_PAUSE -> {
                        togglePlay()
                        true
                    }

                    code == KEY_SPACE && !controlsVisible -> {
                        togglePlay()
                        true
                    }

                    !controlsVisible && !showChannels &&
                        (code == KEY_DPAD_CENTER || code == KEY_ENTER ||
                            code == KEY_DPAD_UP || code == KEY_DPAD_DOWN) -> {
                        revealControls()
                        true
                    }

                    !controlsVisible && !showChannels && code == KEY_DPAD_LEFT -> {
                        seekBy(-10_000)
                        true
                    }

                    !controlsVisible && !showChannels && code == KEY_DPAD_RIGHT -> {
                        seekBy(10_000)
                        true
                    }

                    else -> false
                }
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setKeepScreenOn(true)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Transparent tap target so touching the video always reveals the controls.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { revealControls() }
                },
        )

        AnimatedVisibility(
            visible = controlsVisible && !showChannels,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                            ),
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerButton(text = "Back", onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = channelName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = channel.group ?: "Channel",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.62f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    PlayerButton(
                        text = if (isFavorite) "Saved" else "Save",
                        onClick = {
                            onToggleFavorite()
                            revealControls()
                        },
                        accent = isFavorite,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            ),
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (durationMs > 0L) {
                        Slider(
                            value = positionMs.coerceIn(0L, durationMs).toFloat(),
                            onValueChange = { value ->
                                positionMs = value.toLong()
                                interaction += 1
                            },
                            onValueChangeFinished = {
                                runCatching { player.seekTo(positionMs) }
                                revealControls()
                            },
                            valueRange = 0f..durationMs.toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatTime(positionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        Spacer(Modifier.weight(1f))
                        if (durationMs > 0L) {
                            Text(
                                text = formatTime(durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerButton(
                            text = if (isPlaying) "Pause" else "Play",
                            onClick = togglePlay,
                            primary = true,
                            modifier = Modifier.focusRequester(playFocus),
                        )
                        PlayerButton(text = "-10s", onClick = { seekBy(-10_000) })
                        PlayerButton(text = "+10s", onClick = { seekBy(10_000) })
                        PlayerButton(
                            text = if (isMuted) "Sound" else "Mute",
                            onClick = toggleMute,
                        )
                        PlayerButton(
                            text = "Channels",
                            onClick = {
                                revealControls()
                                showChannels = true
                            },
                        )
                        PlayerButton(text = aspectLabel(resizeMode), onClick = cycleAspect)
                        if (allowRotation) {
                            PlayerButton(
                                text = if (isLandscape) "Portrait" else "Landscape",
                                onClick = toggleRotation,
                            )
                        }
                        if (canUsePip && allowRotation) {
                            PlayerButton(text = "PiP", onClick = enterPip)
                        }
                    }
                }
            }
        }

        if (playbackState.intValue == Player.STATE_BUFFERING && errorMessage.value == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Accent,
            )
        }

        AnimatedVisibility(
            visible = showChannels,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize().zIndex(2f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.86f))
                    .pointerInput(Unit) {
                        detectTapGestures { showChannels = false }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .fillMaxHeight(0.84f)
                        .widthIn(max = 540.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { /* taps inside the panel stay inside */ }
                        }
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Channels",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatCount(channels.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.width(12.dp))
                        PlayerButton(
                            text = "Close",
                            onClick = { showChannels = false },
                            modifier = Modifier.focusRequester(channelsFocus),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    val listState = rememberLazyListState(
                        initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0),
                    )
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = channels,
                            key = { index, item -> "${item.url}_$index" },
                        ) { _, item ->
                            ChannelListItem(
                                channel = item,
                                isCurrent = item.url == channel.url,
                                onClick = {
                                    showChannels = false
                                    if (item.url != channel.url) onSelectChannel(item)
                                    revealControls()
                                },
                            )
                        }
                    }
                }
            }
        }

        if (errorMessage.value != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 420.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Stream unavailable",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = errorMessage.value.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                errorMessage.value = null
                                playbackState.intValue = Player.STATE_IDLE
                                controlsVisible = true
                                attempt += 1
                            },
                            modifier = Modifier.focusRequester(retryFocus),
                        ) { Text("Retry") }
                        Button(
                            onClick = {
                                controlsVisible = true
                                showChannels = true
                            },
                        ) { Text("Channel list") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    accent: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background = when {
        primary -> Accent
        focused -> Color.White.copy(alpha = 0.28f)
        accent -> Accent.copy(alpha = 0.24f)
        else -> Color.White.copy(alpha = 0.12f)
    }
    val textColor = when {
        primary -> Color(0xFF04121A)
        accent -> Accent
        else -> Color.White
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(
                width = 2.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50),
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ChannelListItem(
    channel: Channel,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                when {
                    isCurrent -> Accent.copy(alpha = 0.16f)
                    focused -> Color.White.copy(alpha = 0.12f)
                    else -> Color.White.copy(alpha = 0.05f)
                },
            )
            .border(
                width = 1.5.dp,
                color = when {
                    focused -> Color.White
                    isCurrent -> Accent
                    else -> Color.Transparent
                },
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = displayName(channel.name),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = channel.group ?: "Channel",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            Text(
                text = "Playing",
                style = MaterialTheme.typography.labelSmall,
                color = Accent,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun createPlayer(
    context: Context,
    channel: Channel,
    autoPlay: Boolean,
    errorMessage: MutableState<String?>,
    playbackState: MutableIntState,
): ExoPlayer {
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val httpDataSource = OkHttpDataSource.Factory(httpClient)
        .setUserAgent(UrlUtils.UA)
        .setDefaultRequestProperties(channel.headers)

    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSource)
    val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(5_000, 30_000, 1_000, 2_000)
                .build(),
        )
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            playWhenReady = autoPlay
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    playbackState.intValue = state
                }

                override fun onPlayerError(error: PlaybackException) {
                    val detail = error.cause?.message
                    errorMessage.value = buildString {
                        append(error.errorCodeName)
                        if (!detail.isNullOrBlank()) append(" · ").append(detail)
                    }
                }
            })
        }
}

private fun nextAspect(mode: Int): Int = when (mode) {
    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

private fun aspectLabel(mode: Int): String = when (mode) {
    AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
    else -> "Fill"
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun displayName(name: String): String =
    name.replace(Regex("""\s*\(\d{3,4}p\)""", RegexOption.IGNORE_CASE), "").trim().ifBlank { name }

/** Gives ExoPlayer a hint about the container, so ts/m3u8 streams start faster. */
private fun mimeTypeFor(url: String): String? {
    val path = url.substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.endsWith(".m3u8") || path.endsWith(".m3u") -> MimeTypes.APPLICATION_M3U8
        path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
        path.endsWith(".ts") -> MimeTypes.VIDEO_MP2T
        path.endsWith(".mp4") || path.endsWith(".m4v") -> MimeTypes.VIDEO_MP4
        path.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
        path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
        path.endsWith(".mp3") -> MimeTypes.AUDIO_MPEG
        path.endsWith(".aac") -> MimeTypes.AUDIO_AAC
        path.endsWith(".flac") -> MimeTypes.AUDIO_FLAC
        else -> null
    }
}
