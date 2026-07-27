/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.mediaplayer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CONTROLS_AUTO_HIDE_MS = 3_000L

/**
 * Reusable full-screen video player. Plays either a local file ([localPath]) or a remote
 * [contentUrl]. Callers own navigation via [onNavigateBack]; the ViewModel is resolved from the
 * shared media-player Metro graph so any module can host this screen.
 */
@Composable
fun VideoPlayer(
    localPath: String?,
    contentUrl: String?,
    fileName: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoPlayerViewModel = videoPlayerViewModel(localPath, contentUrl, fileName),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VideoPlayerContent(
        player = viewModel.player,
        state = state,
        localPath = viewModel.localPath,
        fileName = viewModel.fileName,
        onTogglePlayPause = viewModel::togglePlayPause,
        onToggleMute = viewModel::toggleMute,
        onSeek = viewModel::seekTo,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun VideoPlayerContent(
    player: ExoPlayer,
    state: VideoPlaybackState,
    localPath: String?,
    fileName: String?,
    onTogglePlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onSeek: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isExiting by remember { mutableStateOf(false) }

    // UI-only state. Playback state (playing, position, duration, mute, …) lives in the ViewModel so
    // it survives the activity recreation on rotation
    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    // Survives the activity recreation triggered by orientation changes
    var lockedOrientation by rememberSaveable { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_USER) }

    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = scope.launch {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    fun showControls(autoHide: Boolean = state.isPlaying) {
        controlsVisible = true
        if (autoHide) scheduleAutoHide()
    }

    fun toggleControls() {
        if (controlsVisible) {
            if (state.isPlaying) {
                autoHideJob?.cancel()
                controlsVisible = false
            }
        } else {
            showControls()
        }
    }

    // auto-hide while playing, keep visible while paused or completed.
    LaunchedEffect(state.isPlaying, state.isCompleted) {
        when {
            state.isCompleted -> {
                autoHideJob?.cancel()
                controlsVisible = true
            }

            state.isPlaying -> scheduleAutoHide()
            else -> {
                autoHideJob?.cancel()
                showControls(autoHide = false)
            }
        }
    }

    // Toggle the requested orientation; the layout reacts to the resulting configuration change
    fun toggleFullScreen() {
        lockedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    LaunchedEffect(lockedOrientation) {
        context.findActivity()?.requestedOrientation = lockedOrientation
    }

    DisposableEffect(isLandscape) {
        val controller = context.findActivity()?.window?.let { WindowInsetsControllerCompat(it, view) }
        if (isLandscape) {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            autoHideJob?.cancel()
        }
    }

   fun stopAndNavigateBack() {
        isExiting = true
        player.pause()
        context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        onNavigateBack()
    }

    BackHandler {
        if (isLandscape) {
            toggleFullScreen()
        } else {
            stopAndNavigateBack()
        }
    }

    WireScaffold(
        modifier = modifier,
        topBar = {
            if (!isLandscape && !isExiting) {
                WireCenterAlignedTopAppBar(
                    title = fileName ?: stringResource(R.string.media_player_title),
                    navigationIconType = NavigationIconType.Back(),
                    onNavigationPressed = ::stopAndNavigateBack,
                )
            }
        },
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(if (isLandscape) PaddingValues(0.dp) else innerPadding)
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { if (!isExiting) toggleControls() },
            contentAlignment = Alignment.Center,
        ) {
            if (!isExiting) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            setPlayer(player)
                            useController = false
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { it.player = null },
                )
            }

            if (state.isBuffering && !isExiting) {
                WireCircularProgressIndicator(
                    progressColor = Color.White,
                    size = dimensions().spacing48x,
                    modifier = Modifier.size(dimensions().spacing48x),
                )
            }

            AnimatedVisibility(
                visible = !state.isStarted && !isExiting,
                exit = fadeOut(animationSpec = tween(durationMillis = 600)),
            ) {
                if (localPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(localPath)
                            .decoderFactory { result, options, _ ->
                                VideoFrameDecoder(result.source, options)
                            }
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (!isExiting && !state.isBuffering) {
                val buttonScale by animateFloatAsState(
                    targetValue = if (controlsVisible) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "videoButtonScale",
                )

                Box(
                    modifier = Modifier
                        .size(dimensions().spacing72x)
                        .scale(buttonScale)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { onTogglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = when {
                            state.isCompleted -> VideoButtonState.REPLAY
                            state.isPlaying -> VideoButtonState.PAUSE
                            else -> VideoButtonState.PLAY
                        },
                        transitionSpec = {
                            (scaleIn(animationSpec = spring(Spring.DampingRatioLowBouncy)) + fadeIn()) togetherWith
                                    (scaleOut() + fadeOut())
                        },
                        label = "videoButtonIcon",
                    ) { buttonState ->
                        val res = when (buttonState) {
                            VideoButtonState.PLAY -> R.drawable.ic_play
                            VideoButtonState.PAUSE -> R.drawable.ic_pause
                            VideoButtonState.REPLAY -> R.drawable.ic_replay
                        }
                        Icon(
                            painter = painterResource(res),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = controlsVisible && !isExiting,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300),
                ),
                exit = fadeOut(tween(250)) + slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(250),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.82f),
                                ),
                            )
                        )
                        .padding(horizontal = dimensions().spacing8x, vertical = dimensions().spacing4x),
                ) {
                    val progress = if (state.durationMs > 0 && !isSeeking) {
                        state.currentPositionMs.toFloat() / state.durationMs
                    } else if (isSeeking) {
                        seekProgress
                    } else {
                        0f
                    }

                    Slider(
                        value = progress,
                        onValueChange = { value ->
                            isSeeking = true
                            seekProgress = value
                        },
                        onValueChangeFinished = {
                            onSeek((seekProgress * state.durationMs).toLong())
                            isSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions().spacing4x),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.currentPositionMs.toTimeString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.durationMs.toTimeString(),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                            Icon(
                                painter = painterResource(
                                    if (state.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
                                ),
                                contentDescription = stringResource(
                                    if (state.isMuted) R.string.media_player_unmute else R.string.media_player_mute
                                ),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(dimensions().spacing24x)
                                    .clip(CircleShape)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { onToggleMute() },
                            )
                            Icon(
                                painter = painterResource(
                                    if (isLandscape) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
                                ),
                                contentDescription = stringResource(
                                    if (isLandscape) {
                                        R.string.media_player_exit_fullscreen
                                    } else {
                                        R.string.media_player_enter_fullscreen
                                    }
                                ),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { toggleFullScreen() },
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class VideoButtonState { PLAY, PAUSE, REPLAY }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("MagicNumber")
private fun Int.toTimeString(): String {
    val totalSec = this / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

@MultipleThemePreviews
@Composable
fun PreviewCellVideoViewerScreen() {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    WireTheme {
        VideoPlayerContent(
            player = player,
            state = VideoPlaybackState(),
            localPath = null,
            fileName = "video.mp4",
            onTogglePlayPause = {},
            onToggleMute = {},
            onSeek = {},
            onNavigateBack = {},
        )
    }
}
