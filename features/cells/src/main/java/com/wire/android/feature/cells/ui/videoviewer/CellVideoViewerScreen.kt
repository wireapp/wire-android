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
package com.wire.android.feature.cells.ui.videoviewer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import com.wire.android.feature.cells.R
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireCellsDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private const val CONTROLS_AUTO_HIDE_MS = 3_000L
private const val POSITION_POLL_MS = 200L

@WireCellsDestination(
    style = PopUpNavigationAnimation::class,
    navArgs = CellVideoViewerNavArgs::class,
)
@Composable
fun CellVideoViewerScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
    viewModel: CellVideoViewerViewModel = hiltViewModel(),
) {
    CellVideoViewerScreenContent(
        player = viewModel.player,
        localPath = viewModel.localPath,
        fileName = viewModel.fileName,
        onNavigateBack = navigator::navigateBack,
        modifier = modifier,
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun CellVideoViewerScreenContent(
    player: ExoPlayer,
    localPath: String?,
    fileName: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Flag to track if the screen is immediately exiting
    var isExiting by remember { mutableStateOf(false) }

    // Playback state — initialised from the player so it is correct after the activity is recreated
    // on rotation (the player itself, held by the ViewModel, keeps its real state).
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var isStarted by remember { mutableStateOf(player.currentPosition > 0L || player.isPlaying) }
    var isCompleted by remember { mutableStateOf(player.playbackState == Player.STATE_ENDED) }
    var isBuffering by remember { mutableStateOf(player.playbackState == Player.STATE_BUFFERING) }
    var isMuted by remember { mutableStateOf(player.volume == 0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableIntStateOf(player.currentPosition.toInt()) }
    var durationMs by remember { mutableIntStateOf(player.duration.coerceAtLeast(0).toInt()) }
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

    fun showControls(autoHide: Boolean = isPlaying) {
        controlsVisible = true
        if (autoHide) scheduleAutoHide()
    }

    fun toggleControls() {
        if (controlsVisible) {
            if (isPlaying) {
                autoHideJob?.cancel()
                controlsVisible = false
            }
        } else {
            showControls()
        }
    }

    fun play() {
        player.play()
        isStarted = true
        isCompleted = false
        scheduleAutoHide()
    }

    fun pause() {
        player.pause()
        autoHideJob?.cancel()
        showControls(autoHide = false)
    }

    fun replay() {
        player.seekTo(0)
        play()
    }

    fun togglePlayPause() {
        if (isCompleted) {
            replay()
        } else if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun toggleMute() {
        isMuted = !isMuted
        player.volume = if (isMuted) 0f else 1f
    }

    // Reflect player state into Compose state
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                when (playbackState) {
                    Player.STATE_READY -> durationMs = player.duration.coerceAtLeast(0).toInt()
                    Player.STATE_ENDED -> {
                        isCompleted = true
                        controlsVisible = true
                        autoHideJob?.cancel()
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            // The player is owned by the ViewModel; only detach the listener here, do not release it
            player.removeListener(listener)
        }
    }

    // Poll playback position while playing
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            if (!isSeeking) {
                currentPositionMs = player.currentPosition.toInt()
                durationMs = player.duration.coerceAtLeast(0).toInt()
            }
            delay(POSITION_POLL_MS)
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

    // Apply the requested orientation
    LaunchedEffect(lockedOrientation) {
        context.findActivity()?.requestedOrientation = lockedOrientation
    }

    // Go immersive while in landscape full screen
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

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            autoHideJob?.cancel()
        }
    }

    // Set exiting state, tear down the layout, and pause the player so it doesn't keep playing during
    // the exit animation. The ViewModel releases the player when it is cleared.
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
            if (!isLandscape && !isExiting) { // Don't show topBar when exiting
                WireCenterAlignedTopAppBar(
                    title = fileName ?: stringResource(R.string.conversation_files_title),
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
            // Tear down the AndroidView during the exit animation so playback stops instantly
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

            // — Buffering / loading indicator
            if (isBuffering && !isExiting) {
                WireCircularProgressIndicator(
                    progressColor = Color.White,
                    size = dimensions().spacing48x,
                    modifier = Modifier.size(dimensions().spacing48x),
                )
            }

            // — Thumbnail overlay
            AnimatedVisibility(
                visible = !isStarted && !isExiting,
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

            // — Center play / pause / replay button (hidden while exiting or buffering)
            if (!isExiting && !isBuffering) {
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
                        .size(72.dp)
                        .scale(buttonScale)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = when {
                            isCompleted -> VideoButtonState.REPLAY
                            isPlaying -> VideoButtonState.PAUSE
                            else -> VideoButtonState.PLAY
                        },
                        transitionSpec = {
                            (scaleIn(animationSpec = spring(Spring.DampingRatioLowBouncy)) + fadeIn()) togetherWith
                                    (scaleOut() + fadeOut())
                        },
                        label = "videoButtonIcon",
                    ) { state ->
                        val res = when (state) {
                            VideoButtonState.PLAY -> R.drawable.ic_cell_play
                            VideoButtonState.PAUSE -> R.drawable.ic_cell_pause
                            VideoButtonState.REPLAY -> R.drawable.ic_cell_replay
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

            // — Bottom controls bar (Only show if not exiting)
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
                    val progress = if (durationMs > 0 && !isSeeking) {
                        currentPositionMs.toFloat() / durationMs
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
                            val target = (seekProgress * durationMs).toLong()
                            player.seekTo(target)
                            currentPositionMs = target.toInt()
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
                            text = currentPositionMs.toTimeString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = durationMs.toTimeString(),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                            Icon(
                                painter = painterResource(
                                    if (isMuted) R.drawable.ic_cell_volume_off else R.drawable.ic_cell_volume_on
                                ),
                                contentDescription = stringResource(
                                    if (isMuted) R.string.cells_video_unmute else R.string.cells_video_mute
                                ),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { toggleMute() },
                            )
                            Icon(
                                painter = painterResource(
                                    if (isLandscape) R.drawable.ic_cell_fullscreen_exit else R.drawable.ic_cell_fullscreen
                                ),
                                contentDescription = stringResource(
                                    if (isLandscape) {
                                        R.string.cells_video_exit_fullscreen
                                    } else {
                                        R.string.cells_video_enter_fullscreen
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
        CellVideoViewerScreenContent(
            player = player,
            localPath = null,
            fileName = "video.mp4",
            onNavigateBack = {},
        )
    }
}

