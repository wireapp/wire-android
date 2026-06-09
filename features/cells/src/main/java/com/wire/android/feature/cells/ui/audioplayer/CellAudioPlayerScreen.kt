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
package com.wire.android.feature.cells.ui.audioplayer

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val POSITION_POLL_MS = 200L
private const val SKIP_MS = 15_000

private val BackgroundTop = Color(0xFF1A1A2E)
private val BackgroundBottom = Color(0xFF0D0D1A)
private val AccentColor = Color(0xFF6C5CE7)
private val AccentLight = Color(0xFFA29BFE)

@WireCellsDestination(
    style = PopUpNavigationAnimation::class,
    navArgs = CellAudioPlayerNavArgs::class,
)
@Composable
fun CellAudioPlayerScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
    viewModel: CellAudioPlayerViewModel = hiltViewModel(),
) {
    CellAudioPlayerContent(
        localPath = viewModel.localPath,
        contentUrl = viewModel.contentUrl,
        fileName = viewModel.fileName,
        onNavigateBack = navigator::navigateBack,
        modifier = modifier,
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun CellAudioPlayerContent(
    localPath: String?,
    contentUrl: String?,
    fileName: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var isPrepared by remember { mutableStateOf(false) }

    val mediaPlayer = remember {
        MediaPlayer().apply {
            setOnPreparedListener { mp ->
                durationMs = mp.duration
                isPrepared = true
            }
            setOnCompletionListener {
                isPlaying = false
                isCompleted = true
            }
        }
    }

    // Initialise the media source
    LaunchedEffect(localPath, contentUrl) {
        try {
            mediaPlayer.reset()
            isPrepared = false
            when {
                localPath != null -> mediaPlayer.setDataSource(localPath)
                contentUrl != null -> mediaPlayer.setDataSource(context, Uri.parse(contentUrl))
                else -> return@LaunchedEffect
            }
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            // handle silently — file may not exist yet
        }
    }

    // Poll playback position while playing
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            if (!isSeeking) {
                currentPositionMs = mediaPlayer.currentPosition
            }
            delay(POSITION_POLL_MS)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            try { mediaPlayer.stop() } catch (_: Exception) { }
            mediaPlayer.release()
        }
    }

    fun stopAndBack() {
        try { mediaPlayer.stop() } catch (_: Exception) { }
        onNavigateBack()
    }

    fun play() {
        if (isPrepared) {
            mediaPlayer.start()
            isPlaying = true
            isCompleted = false
        }
    }

    fun pause() {
        mediaPlayer.pause()
        isPlaying = false
    }

    fun seekTo(ms: Int) {
        mediaPlayer.seekTo(ms)
        currentPositionMs = ms
    }

    fun togglePlayPause() {
        if (isCompleted) {
            seekTo(0)
            play()
        } else if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    BackHandler { stopAndBack() }

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                title = fileName ?: stringResource(R.string.conversation_files_title),
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = ::stopAndBack,
            )
        },
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BackgroundTop, BackgroundBottom)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions().spacing24x),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Spacer(modifier = Modifier.height(dimensions().spacing24x))

                // — Animated album art circle
                PulsingAlbumArt(isPlaying = isPlaying)

                Spacer(modifier = Modifier.height(dimensions().spacing32x))

                // — Equalizer bars
                EqualizerBars(isPlaying = isPlaying)

                Spacer(modifier = Modifier.height(dimensions().spacing24x))

                // — File name
                Text(
                    text = fileName ?: stringResource(R.string.conversation_files_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(dimensions().spacing32x))

                // — Seek slider
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
                        val targetMs = (seekProgress * durationMs).toInt()
                        seekTo(targetMs)
                        currentPositionMs = targetMs
                        isSeeking = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = AccentLight,
                        activeTrackColor = AccentLight,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // — Time row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions().spacing4x),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = currentPositionMs.toTimeString(),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                    )
                    Text(
                        text = durationMs.toTimeString(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                    )
                }

                Spacer(modifier = Modifier.height(dimensions().spacing24x))

                // — Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Skip back 15s
                    IconButton(
                        onClick = {
                            val target = (currentPositionMs - SKIP_MS).coerceAtLeast(0)
                            seekTo(target)
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cell_skip_back),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(dimensions().spacing24x))

                    // Play / Pause button (large, spring-animated)
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isPrepared) 1f else 0.7f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "audioButtonScale",
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(buttonScale)
                            .clip(CircleShape)
                            .background(AccentColor)
                            .then(
                                if (isPrepared) {
                                    Modifier.padding(0.dp)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val iconRes = if (isPlaying) R.drawable.ic_cell_pause else R.drawable.ic_cell_play
                        IconButton(
                            onClick = { if (isPrepared) togglePlayPause() },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(dimensions().spacing24x))

                    // Skip forward 15s
                    IconButton(
                        onClick = {
                            val target = (currentPositionMs + SKIP_MS).coerceAtMost(durationMs)
                            seekTo(target)
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cell_skip_forward),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimensions().spacing32x))
            }
        }
    }
}

// — Animated pulsing album art placeholder
@Composable
private fun PulsingAlbumArt(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "albumArtPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "albumArtScale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(AccentColor, BackgroundTop),
                )
            ),
    ) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentLight.copy(alpha = 0.15f),
                            Color.Transparent,
                        )
                    )
                )
        )
        Icon(
            painter = painterResource(R.drawable.ic_file_type_audio),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(88.dp),
        )
    }
}

// — Animated equalizer bars
@Composable
private fun EqualizerBars(isPlaying: Boolean) {
    val barCount = 7
    val infiniteTransition = rememberInfiniteTransition(label = "equalizerBars")

    val heights = (0 until barCount).map { index ->
        val durationMs = 400 + index * 80
        val initialValue = 0.15f + (index % 3) * 0.1f
        val targetValue = if (isPlaying) 0.5f + (index % 4) * 0.15f else initialValue

        infiniteTransition.animateFloat(
            initialValue = initialValue,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar$index",
        )
    }

    val maxBarHeightPx = 32.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(maxBarHeightPx),
    ) {
        heights.forEach { heightState ->
            val fraction by heightState
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(maxBarHeightPx * fraction)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(AccentLight, AccentColor)
                        )
                    )
            )
        }
    }
}

private fun Int.toTimeString(): String {
    val totalSec = this / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

@MultipleThemePreviews
@Composable
fun PreviewCellAudioPlayerScreen() {
    WireTheme {
        CellAudioPlayerContent(
            localPath = null,
            contentUrl = null,
            fileName = "awesome_track.mp3",
            onNavigateBack = {},
        )
    }
}

