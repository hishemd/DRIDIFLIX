package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.data.model.StreamType
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.ExternalPlayerMenuSheet
import com.example.ui.theme.*
import com.example.util.Strings
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun BuiltInPlayerScreen(
    uiState: UiState,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang = uiState.settings.language

    // Find host activity for orientation toggles
    val activity = remember(context) {
        var current: Context = context
        while (current is ContextWrapper) {
            if (current is Activity) return@remember current
            current = current.baseContext
        }
        null
    }

    var isLandscape by remember { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(uiState.playingStartPosMs) }
    var durationMs by remember { mutableStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Channel list overlay for Live TV
    var isChannelDrawerOpen by remember { mutableStateOf(false) }
    var isExternalMenuOpen by remember { mutableStateOf(false) }

    // ExoPlayer creation & lifecycle
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Prepare media
    LaunchedEffect(uiState.playingStreamUrl) {
        if (uiState.playingStreamUrl.isNotBlank()) {
            playbackError = null
            isBuffering = true
            val mediaItem = ExoMediaItem.fromUri(uiState.playingStreamUrl)
            exoPlayer.setMediaItem(mediaItem)
            if (uiState.playingStartPosMs > 0 && uiState.playingStreamType != StreamType.LIVE) {
                exoPlayer.seekTo(uiState.playingStartPosMs)
            }
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = "${Strings.get("error_playback", lang)}: ${error.errorCodeName}"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Polling progress timer
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(1000)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible && isPlaying) {
            delay(4000)
            isControlsVisible = false
        }
    }

    BackHandler {
        if (isChannelDrawerOpen) {
            isChannelDrawerOpen = false
        } else {
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isControlsVisible = !isControlsVisible }
                )
            }
    ) {
        // Player View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = NetflixRed,
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.Center)
            )
        }

        // Playback Error Overlay
        if (playbackError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NetflixBlack.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NetflixRed,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = playbackError ?: "",
                        color = NetflixWhite,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                playbackError = null
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
                        ) {
                            Text("إعادة المحاولة", color = NetflixWhite)
                        }
                        OutlinedButton(
                            onClick = { isExternalMenuOpen = true }
                        ) {
                            Text(Strings.get("open_external", lang), color = NetflixWhite)
                        }
                    }
                }
            }
        }

        // Custom Netflix Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
                                onBack()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = NetflixWhite
                            )
                        }

                        Text(
                            text = uiState.playingTitle,
                            color = NetflixWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Aspect Ratio Toggle
                        IconButton(
                            onClick = {
                                resizeMode = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = Strings.get("aspect_ratio", lang),
                                tint = NetflixWhite
                            )
                        }

                        // Orientation toggle
                        IconButton(
                            onClick = {
                                isLandscape = !isLandscape
                                activity?.requestedOrientation = if (isLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isLandscape) Icons.Default.ScreenRotation else Icons.Default.ScreenLockRotation,
                                contentDescription = "Orientation",
                                tint = NetflixWhite
                            )
                        }

                        // External Player launcher
                        IconButton(
                            onClick = { isExternalMenuOpen = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = Strings.get("open_external", lang),
                                tint = NetflixRed
                            )
                        }
                    }
                }

                // Center Controls (±10s and Play/Pause)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    // Replay 10s (if not Live)
                    if (uiState.playingStreamType != StreamType.LIVE) {
                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                            },
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = NetflixWhite,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Play/Pause Big Button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(NetflixRed)
                            .clickable {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NetflixWhite,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Forward 10s (if not Live)
                    if (uiState.playingStreamType != StreamType.LIVE) {
                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                            },
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = NetflixWhite,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Bottom Control Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Live TV Badge or Progress Slider
                    if (uiState.playingStreamType == StreamType.LIVE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NetflixRed)
                                )
                                Text(
                                    text = "LIVE TV",
                                    color = NetflixRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // Channel Drawer Toggle
                            Button(
                                onClick = { isChannelDrawerOpen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixCardBg),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = null,
                                        tint = NetflixWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = Strings.get("channel_list", lang),
                                        color = NetflixWhite,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Slider + Timestamps
                        var sliderPosition by remember { mutableFloatStateOf(0f) }
                        var isDragging by remember { mutableStateOf(false) }

                        val currentSec = currentPositionMs / 1000
                        val totalSec = durationMs / 1000

                        val currentTimeStr = formatTime(currentSec)
                        val totalTimeStr = formatTime(totalSec)

                        Slider(
                            value = if (isDragging) sliderPosition else (if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f),
                            onValueChange = {
                                isDragging = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                val newPos = (sliderPosition * durationMs).toLong()
                                exoPlayer.seekTo(newPos)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = NetflixRed,
                                activeTrackColor = NetflixRed,
                                inactiveTrackColor = NetflixCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = currentTimeStr, color = NetflixGrey, fontSize = 11.sp)
                            Text(text = totalTimeStr, color = NetflixGrey, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Live Channels Quick Drawer
        AnimatedVisibility(
            visible = isChannelDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .background(NetflixDarkGrey.copy(alpha = 0.95f))
                    .padding(vertical = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.get("channel_list", lang),
                            color = NetflixWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { isChannelDrawerOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = NetflixWhite)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(uiState.liveChannels) { ch ->
                            val isSelected = ch.streamId == uiState.playingStreamId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.playStream(
                                            streamUrl = ch.streamUrl,
                                            title = ch.name,
                                            streamType = StreamType.LIVE,
                                            streamId = ch.streamId,
                                            posterUrl = ch.posterUrl
                                        )
                                        isChannelDrawerOpen = false
                                    }
                                    .background(if (isSelected) NetflixRed.copy(alpha = 0.2f) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(NetflixCardBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ch.posterUrl,
                                        contentDescription = ch.name,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Text(
                                    text = ch.name,
                                    color = if (isSelected) NetflixRed else NetflixWhite,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = NetflixRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isExternalMenuOpen) {
        ExternalPlayerMenuSheet(
            streamUrl = uiState.playingStreamUrl,
            title = uiState.playingTitle,
            lang = lang,
            onDismiss = { isExternalMenuOpen = false }
        )
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
