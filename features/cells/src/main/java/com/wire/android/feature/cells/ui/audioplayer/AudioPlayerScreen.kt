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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.cellAudioPlayerViewModel
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireCellsDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme

private const val SKIP_MS = 15_000

private val BackgroundTop = Color(0xFF1A1A2E)
private val BackgroundBottom = Color(0xFF0D0D1A)
private val AccentColor = Color(0xFF6C5CE7)
private val AccentLight = Color(0xFFA29BFE)

@WireCellsDestination(
    style = PopUpNavigationAnimation::class,
    navArgs = AudioPlayerNavArgs::class,
)
@Composable
fun CellAudioPlayerScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
    viewModel: CellAudioPlayerViewModel = cellAudioPlayerViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CellAudioPlayerContent(
        state = state,
        fileName = viewModel.fileName,
        onTogglePlayPause = viewModel::togglePlayPause,
        onSeek = viewModel::seekTo,
        onStop = viewModel::pause,
        onNavigateBack = navigator::navigateBack,
        modifier = modifier,
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun CellAudioPlayerContent(
    state: AudioPlaybackState,
    fileName: String?,
    onTogglePlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onStop: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    fun stopAndBack() {
        onStop()
        onNavigateBack()
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

                PulsingAlbumArt(isPlaying = state.isPlaying)

                Spacer(modifier = Modifier.height(dimensions().spacing32x))

                EqualizerBars(isPlaying = state.isPlaying)

                Spacer(modifier = Modifier.height(dimensions().spacing24x))

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
                        onSeek((seekProgress * state.durationMs).toInt())
                        isSeeking = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = AccentLight,
                        activeTrackColor = AccentLight,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
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
                        text = state.currentPositionMs.toTimeString(),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                    )
                    Text(
                        text = state.durationMs.toTimeString(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                    )
                }

                Spacer(modifier = Modifier.height(dimensions().spacing24x))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            onSeek((state.currentPositionMs - SKIP_MS).coerceAtLeast(0))
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

                    val buttonScale by animateFloatAsState(
                        targetValue = if (state.isPrepared) 1f else 0.7f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "audioButtonScale",
                    )

                    Box(
                        modifier = Modifier
                            .size(dimensions().spacing72x)
                            .scale(buttonScale)
                            .clip(CircleShape)
                            .background(AccentColor)
                            .then(
                                if (state.isPrepared) {
                                    Modifier.padding(0.dp)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val iconRes = if (state.isPlaying) R.drawable.ic_cell_pause else R.drawable.ic_cell_play
                        IconButton(
                            onClick = { if (state.isPrepared) onTogglePlayPause() },
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

                    IconButton(
                        onClick = {
                            onSeek((state.currentPositionMs + SKIP_MS).coerceAtMost(state.durationMs))
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cell_skip_forward),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(dimensions().spacing32x),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimensions().spacing32x))
            }
        }
    }
}

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
            .size(dimensions().spacing200x)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(AccentColor, BackgroundTop),
                )
            ),
    ) {
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

    val maxBarHeightPx = dimensions().spacing32x
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(maxBarHeightPx),
    ) {
        heights.forEach { heightState ->
            val fraction by heightState
            Box(
                modifier = Modifier
                    .width(dimensions().spacing6x)
                    .height(maxBarHeightPx * fraction)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(dimensions().spacing3x))
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
            state = AudioPlaybackState(),
            fileName = "awesome_track.mp3",
            onTogglePlayPause = {},
            onSeek = {},
            onStop = {},
            onNavigateBack = {},
        )
    }
}

