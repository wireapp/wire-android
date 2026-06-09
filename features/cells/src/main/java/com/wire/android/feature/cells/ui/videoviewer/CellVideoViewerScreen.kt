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

import android.net.Uri
import android.widget.VideoView
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import com.wire.android.feature.cells.R
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireCellsDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
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
        localPath = viewModel.localPath,
        contentUrl = viewModel.contentUrl,
        fileName = viewModel.fileName,
        onNavigateBack = navigator::navigateBack,
        modifier = modifier,
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun CellVideoViewerScreenContent(
    localPath: String?,
    contentUrl: String?,
    fileName: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Playback state
    var isPlaying by remember { mutableStateOf(false) }
    var isStarted by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
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
        videoViewRef?.start()
        isPlaying = true
        isStarted = true
        isCompleted = false
        scheduleAutoHide()
    }

    fun pause() {
        videoViewRef?.pause()
        isPlaying = false
        autoHideJob?.cancel()
        showControls(autoHide = false)
    }

    fun replay() {
        videoViewRef?.seekTo(0)
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

    // Build video URI
    val videoUri = remember(localPath, contentUrl) {
        when {
            localPath != null -> Uri.fromFile(File(localPath))
            contentUrl != null -> Uri.parse(contentUrl)
            else -> null
        }
    }

    // Poll playback position while playing
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            val vv = videoViewRef
            if (vv != null && !isSeeking) {
                currentPositionMs = vv.currentPosition
                durationMs = vv.duration.coerceAtLeast(0)
            }
            delay(POSITION_POLL_MS)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            autoHideJob?.cancel()
            videoViewRef?.stopPlayback()
        }
    }

    // Stop playback immediately on back so the video doesn't play during the exit animation
    fun stopAndNavigateBack() {
        videoViewRef?.stopPlayback()
        isPlaying = false
        onNavigateBack()
    }

    BackHandler { stopAndNavigateBack() }

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                title = fileName ?: stringResource(R.string.conversation_files_title),
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = ::stopAndNavigateBack,
            )
        },
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { toggleControls() },
            contentAlignment = Alignment.Center,
        ) {
            if (videoUri != null) {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(videoUri)
                            setOnPreparedListener { mp ->
                                durationMs = mp.duration
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                isCompleted = true
                                controlsVisible = true
                                autoHideJob?.cancel()
                            }
                        }.also { videoViewRef = it }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = {
                        videoViewRef?.stopPlayback()
                        videoViewRef?.suspend()
                    },
                )
            }

            // — Thumbnail overlay (fade out on first play)
            AnimatedVisibility(
                visible = !isStarted,
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

            // — Center play / pause / replay button
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

            // — Bottom controls bar
            AnimatedVisibility(
                visible = controlsVisible,
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
                            videoViewRef?.seekTo((seekProgress * durationMs).toInt())
                            currentPositionMs = (seekProgress * durationMs).toInt()
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
                    ) {
                        Text(
                            text = currentPositionMs.toTimeString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = durationMs.toTimeString(),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

private enum class VideoButtonState { PLAY, PAUSE, REPLAY }

private fun Int.toTimeString(): String {
    val totalSec = this / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

@MultipleThemePreviews
@Composable
fun PreviewCellVideoViewerScreen() {
    WireTheme {
        CellVideoViewerScreenContent(
            localPath = null,
            contentUrl = null,
            fileName = "video.mp4",
            onNavigateBack = {},
        )
    }
}

