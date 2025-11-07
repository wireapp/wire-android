/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home.conversations.model.messagetypes.audio

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.media.audiomessage.AudioMediaPlayingState
import com.wire.android.media.audiomessage.AudioMessageArgs
import com.wire.android.media.audiomessage.AudioMessageViewModel
import com.wire.android.media.audiomessage.AudioMessageViewModelImpl
import com.wire.android.media.audiomessage.AudioSpeed
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.equalizedWavesMask
import com.wire.android.media.audiomessage.sampledWavesMask
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.highlighted
import com.wire.android.ui.home.conversations.messages.item.isBubble
import com.wire.android.ui.home.conversations.messages.item.surface
import com.wire.android.ui.home.conversations.messages.item.textColor
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UploadInProgressAssetMessage
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.DateAndTimeParsers
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun AudioMessage(
    audioMessageArgs: AudioMessageArgs,
    audioMessageDurationInMs: Long,
    assetTransferStatus: AssetTransferStatus,
    extension: String,
    size: Long,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
) {
    if (assetTransferStatus == AssetTransferStatus.UPLOAD_IN_PROGRESS) {
        UploadingAudioMessage(extension, size, messageStyle, modifier)
    } else {
        UploadedAudioMessage(audioMessageArgs, audioMessageDurationInMs, extension, size, messageStyle, modifier)
    }
}

@Composable
private fun AudioMessageLayout(
    extension: String,
    size: Long,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .applyIf(!messageStyle.isBubble()) {
                padding(top = dimensions().spacing4x)
                    .background(
                        color = messageStyle.surface(),
                        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
                        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
                    )
                    .padding(dimensions().spacing8x)
            },
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
    ) {
        FileHeaderView(
            extension = extension,
            size = size,
            messageStyle = messageStyle
        )
        Box(
            modifier = Modifier.defaultMinSize(minHeight = MaterialTheme.wireDimensions.spacing72x)
        ) {
            content()
        }
    }
}

@Composable
private fun UploadingAudioMessage(
    extension: String,
    size: Long,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier
) = AudioMessageLayout(extension, size, messageStyle, modifier) {
    UploadInProgressAssetMessage(messageStyle)
}

@Composable
private fun UploadedAudioMessage(
    audioMessageArgs: AudioMessageArgs,
    audioMessageDurationInMs: Long,
    extension: String,
    size: Long,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
) {
    val viewModel: AudioMessageViewModel =
        hiltViewModelScoped<AudioMessageViewModelImpl, AudioMessageViewModel, AudioMessageArgs>(audioMessageArgs)
    val sanitizedAudioState by remember(viewModel.state.audioState, audioMessageDurationInMs) {
        derivedStateOf {
            viewModel.state.audioState.copy(totalTimeInMs = viewModel.state.audioState.sanitizeTotalTime(audioMessageDurationInMs.toInt()))
        }
    }
    UploadedAudioMessage(
        audioState = sanitizedAudioState,
        audioSpeed = viewModel.state.audioSpeed,
        extension = extension,
        size = size,
        onPlayButtonClick = viewModel::playAudio,
        onSliderPositionChange = viewModel::changeAudioPosition,
        onAudioSpeedChange = {
            viewModel.changeAudioSpeed(viewModel.state.audioSpeed.toggle())
        },
        messageStyle = messageStyle,
        modifier = modifier,
    )
}

@Composable
private fun UploadedAudioMessage(
    audioState: AudioState,
    audioSpeed: AudioSpeed,
    extension: String,
    size: Long,
    onPlayButtonClick: () -> Unit,
    onSliderPositionChange: (Float) -> Unit,
    onAudioSpeedChange: (() -> Unit)?,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
) = AudioMessageLayout(extension, size, messageStyle, modifier) {
    if (audioState.audioMediaPlayingState is AudioMediaPlayingState.Failed) {
        FailedAudioMessageContent()
    } else {
        SuccessfulAudioMessageContent(
            audioState = audioState,
            audioSpeed = audioSpeed,
            messageStyle = messageStyle,
            onPlayButtonClick = onPlayButtonClick,
            onSliderPositionChange = onSliderPositionChange,
            onAudioSpeedChange = onAudioSpeedChange,
        )
    }
}

@Composable
fun RecordedAudioMessage(
    audioState: AudioState,
    onPlayButtonClick: () -> Unit,
    onSliderPositionChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.apply {
            padding(top = dimensions().spacing4x)
            padding(dimensions().spacing8x)
        }
    ) {
        SuccessfulAudioMessageContent(
            modifier = Modifier.padding(horizontal = dimensions().spacing24x),
            audioState = audioState,
            audioSpeed = AudioSpeed.NORMAL,
            onPlayButtonClick = onPlayButtonClick,
            onSliderPositionChange = onSliderPositionChange,
            messageStyle = MessageStyle.NORMAL,
            onAudioSpeedChange = null
        )
    }
}

@Composable
fun SuccessfulAudioMessageContent(
    audioState: AudioState,
    audioSpeed: AudioSpeed,
    messageStyle: MessageStyle,
    onPlayButtonClick: () -> Unit,
    onSliderPositionChange: (Float) -> Unit,
    onAudioSpeedChange: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val audioDuration by remember(audioState.currentPositionInMs) {
        mutableStateOf(
            AudioDuration(audioState.totalTimeInMs, audioState.currentPositionInMs)
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        val (iconResource, contentDescriptionRes) = getPlayOrPauseIcon(audioState.audioMediaPlayingState)
        WireSecondaryIconButton(
            minSize = DpSize(dimensions().spacing32x, dimensions().spacing32x),
            minClickableSize = dimensions().buttonMinClickableSize,
            iconSize = dimensions().spacing12x,
            iconResource = iconResource,
            shape = CircleShape,
            contentDescription = contentDescriptionRes,
            state = when (audioState.audioMediaPlayingState) {
                is AudioMediaPlayingState.Fetching -> WireButtonState.Disabled
                else -> WireButtonState.Default
            },
            onButtonClicked = onPlayButtonClick
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {

            AudioMessageSlider(
                audioDuration = audioDuration,
                totalTimeInMs = audioState.totalTimeInMs,
                waveMask = audioState.wavesMask,
                messageStyle = messageStyle,
                onSliderPositionChange = onSliderPositionChange
            )

            val currentTimeColor = when (messageStyle) {
                MessageStyle.BUBBLE_SELF -> MaterialTheme.wireColorScheme.onPrimary
                MessageStyle.BUBBLE_OTHER -> MaterialTheme.wireColorScheme.primary
                MessageStyle.NORMAL -> MaterialTheme.wireColorScheme.primary
            }

            Row {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(vertical = MaterialTheme.wireDimensions.spacing2x),
                    text = audioDuration.formattedCurrentTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = currentTimeColor,
                    maxLines = 1
                )

                if (audioState.audioMediaPlayingState is AudioMediaPlayingState.Playing && onAudioSpeedChange != null) {
                    if (messageStyle == MessageStyle.BUBBLE_SELF) {
                        WireSecondaryButton(
                            onClick = onAudioSpeedChange,
                            text = stringResource(audioSpeed.titleRes),
                            textStyle = MaterialTheme.wireTypography.label03,
                            contentPadding = PaddingValues(
                                horizontal = MaterialTheme.wireDimensions.spacing4x,
                                vertical = MaterialTheme.wireDimensions.spacing2x
                            ),
                            shape = RoundedCornerShape(MaterialTheme.wireDimensions.corner4x),
                            minSize = DpSize(
                                dimensions().spacing32x,
                                dimensions().spacing16x
                            ),
                            minClickableSize = DpSize(
                                dimensions().spacing40x,
                                dimensions().spacing16x
                            ),
                            fillMaxWidth = false
                        )
                    } else {
                        WirePrimaryButton(
                            onClick = onAudioSpeedChange,
                            text = stringResource(audioSpeed.titleRes),
                            textStyle = MaterialTheme.wireTypography.label03,
                            contentPadding = PaddingValues(
                                horizontal = MaterialTheme.wireDimensions.spacing4x,
                                vertical = MaterialTheme.wireDimensions.spacing2x
                            ),
                            shape = RoundedCornerShape(MaterialTheme.wireDimensions.corner4x),
                            minSize = DpSize(
                                dimensions().spacing32x,
                                dimensions().spacing16x
                            ),
                            minClickableSize = DpSize(
                                dimensions().spacing40x,
                                dimensions().spacing16x
                            ),
                            fillMaxWidth = false
                        )
                    }
                }

                Spacer(Modifier.weight(1F))

                if (audioState.audioMediaPlayingState is AudioMediaPlayingState.Fetching) {
                    WireCircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        progressColor = MaterialTheme.wireColorScheme.secondaryButtonEnabled
                    )
                } else {
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(vertical = MaterialTheme.wireDimensions.spacing2x),
                        text = audioDuration.formattedTotalTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = messageStyle.textColor(),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Material3 version 1.3.0 introduced several modifications to the Slider component.
 * These changes may require adjustments to maintain the desired appearance and behavior in your app:
 * - Thumb size changed to 4dp x 44dp - changed back to old 20dp x 20dp.
 * - Thumb requires interactionSource - it must be different than Slider to not update thumb appearance during drag.
 * - Track now draws stop indicator by default - turned off.
 * - Track adds thumbTrackGapSize - set back to 0dp.
 * - Track has different height than previously - changed back to old 4.dp.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AudioMessageSlider(
    audioDuration: AudioDuration,
    totalTimeInMs: AudioState.TotalTimeInMs,
    messageStyle: MessageStyle,
    waveMask: List<Int>?,
    onSliderPositionChange: (Float) -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val totalMs = if (totalTimeInMs is AudioState.TotalTimeInMs.Known) totalTimeInMs.value.toFloat() else 0f
        val waves by remember(waveMask, constraints.maxWidth) {
            derivedStateOf {
                val wavesAmount = with(density) {
                    (constraints.maxWidth.toDp() / 3.dp).toInt() // each wave should take 2dp + spacer 1dp
                }
                when {
                    wavesAmount <= 0 -> emptyList()
                    waveMask.isNullOrEmpty() -> getDefaultWaveMask(wavesAmount)
                    else -> waveMask.equalizedWavesMask().sampledWavesMask(wavesAmount)
                }
            }
        }

        Slider(
            value = audioDuration.currentPositionInMs.toFloat(),
            onValueChange = onSliderPositionChange,
            valueRange = 0f..totalMs,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    colors = SliderDefaults.colors(thumbColor = messageStyle.highlighted()),
                    thumbSize = DpSize(dimensions().spacing4x, dimensions().spacing32x),
                )
            },
            track = { sliderState: SliderState ->
                AudioMessageSliderTrack(sliderState, waves, messageStyle)
            },
            colors = SliderDefaults.colors(
                inactiveTrackColor = colorsScheme().secondaryButtonDisabledOutline
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioMessageSliderTrack(state: SliderState, waves: List<Int>, messageStyle: MessageStyle) {
    val progressPercentage = (state.value - state.valueRange.start) / (state.valueRange.endInclusive - state.valueRange.start)
    val activatedColor = messageStyle.highlighted()
    val disabledColor = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().onPrimary.copy(alpha = 0.7F)
        MessageStyle.BUBBLE_OTHER -> colorsScheme().onTertiaryButtonDisabled
        MessageStyle.NORMAL -> colorsScheme().onTertiaryButtonDisabled
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(
                    color = activatedColor,
                    blendMode = BlendMode.SrcAtop,
                    size = size.copy(width = size.width * progressPercentage, height = size.height)
                )
            }
    ) {
        waves.forEachIndexed { index, wave ->
            Spacer(
                Modifier
                    .background(color = disabledColor, shape = RoundedCornerShape(dimensions().corner2x))
                    .weight(2f)
                    .animateContentSize()
                    .height(wave.dp)
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun FailedAudioMessageContent() {
    var audioNotAvailableDialog by remember { mutableStateOf(false) }

    if (audioNotAvailableDialog) {
        WireDialog(
            title = stringResource(R.string.audio_not_available),
            text = stringResource(R.string.audio_not_available_explanation),
            onDismiss = { audioNotAvailableDialog = false },
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.label_ok),
                type = WireDialogButtonType.Primary,
                onClick = { audioNotAvailableDialog = false }
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().audioMessageHeight)
            .clickable(Clickable(true, onClick = { audioNotAvailableDialog = true })),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_audio),
            contentDescription = null,
            modifier = Modifier
                .width(dimensions().spacing24x)
                .width(dimensions().spacing24x)
                .size(dimensions().spacing24x),
            tint = colorsScheme().secondaryText
        )
        HorizontalSpace.x8()
        Text(
            text = stringResource(R.string.audio_not_available),
            style = MaterialTheme.typography.labelSmall.copy(color = colorsScheme().error),
            maxLines = 1
        )
    }
}

private fun getPlayOrPauseIcon(audioMediaPlayingState: AudioMediaPlayingState): Pair<Int, Int> =
    when (audioMediaPlayingState) {
        AudioMediaPlayingState.Playing -> R.drawable.ic_pause to R.string.content_description_pause_audio
        AudioMediaPlayingState.Completed -> R.drawable.ic_play to R.string.content_description_play_audio
        else -> R.drawable.ic_play to R.string.content_description_play_audio
    }

@Suppress("MagicNumber")
private fun getDefaultWaveMask(amount: Int): List<Int> = List(amount) { 1 }

// helper wrapper class to format the time
private data class AudioDuration(val totalDurationInMs: AudioState.TotalTimeInMs, val currentPositionInMs: Int) {
    companion object {
        const val UNKNOWN_DURATION_LABEL = "-:--"
    }

    fun formattedCurrentTime(): String = DateAndTimeParsers.audioMessageTime(currentPositionInMs.toLong())

    fun formattedTotalTime(): String = if (totalDurationInMs is AudioState.TotalTimeInMs.Known) {
        DateAndTimeParsers.audioMessageTime(totalDurationInMs.value.toLong())
    } else {
        UNKNOWN_DURATION_LABEL
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewUploadingAudioMessage() = WireTheme {
    AudioMessage(
        audioMessageArgs = AudioMessageArgs(ConversationId("convId", "domain"), "messageId"),
        audioMessageDurationInMs = 10000,
        extension = "MP3",
        size = 1024,
        assetTransferStatus = AssetTransferStatus.UPLOAD_IN_PROGRESS,
        messageStyle = MessageStyle.NORMAL
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewUploadedAudioMessage() = WireTheme {
    AudioMessage(
        audioMessageArgs = AudioMessageArgs(ConversationId("convId", "domain"), "messageId"),
        audioMessageDurationInMs = 10000,
        extension = "MP3",
        size = 1024,
        assetTransferStatus = AssetTransferStatus.UPLOADED,
        messageStyle = MessageStyle.NORMAL
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewUploadedAudioMessageFetching() = WireTheme {
    UploadedAudioMessage(
        audioState = PREVIEW_AUDIO_STATE.copy(audioMediaPlayingState = AudioMediaPlayingState.Fetching),
        audioSpeed = AudioSpeed.NORMAL,
        messageStyle = MessageStyle.NORMAL,
        extension = "MP3",
        size = 1024,
        onPlayButtonClick = {},
        onSliderPositionChange = {},
        onAudioSpeedChange = {}
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewUploadedAudioMessageFetched() = WireTheme {
    UploadedAudioMessage(
        audioState = PREVIEW_AUDIO_STATE.copy(audioMediaPlayingState = AudioMediaPlayingState.SuccessfulFetching),
        audioSpeed = AudioSpeed.NORMAL,
        extension = "MP3",
        messageStyle = MessageStyle.NORMAL,
        size = 1024,
        onPlayButtonClick = {},
        onSliderPositionChange = {},
        onAudioSpeedChange = {}
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewUploadedAudioMessagePlaying() = WireTheme {
    UploadedAudioMessage(
        audioState = PREVIEW_AUDIO_STATE.copy(audioMediaPlayingState = AudioMediaPlayingState.Playing, currentPositionInMs = 5000),
        audioSpeed = AudioSpeed.NORMAL,
        messageStyle = MessageStyle.NORMAL,
        extension = "MP3",
        size = 1024,
        onPlayButtonClick = {},
        onSliderPositionChange = {},
        onAudioSpeedChange = {}
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewUploadedAudioMessageFailed() = WireTheme {
    UploadedAudioMessage(
        audioState = PREVIEW_AUDIO_STATE.copy(audioMediaPlayingState = AudioMediaPlayingState.Failed),
        audioSpeed = AudioSpeed.NORMAL,
        messageStyle = MessageStyle.NORMAL,
        extension = "MP3",
        size = 1024,
        onPlayButtonClick = {},
        onSliderPositionChange = {},
        onAudioSpeedChange = {}
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewUploadedAudioMessageNarrow() = WireTheme {
    Box(modifier = Modifier.width(150.dp)) {
        UploadedAudioMessage(
            audioState = PREVIEW_AUDIO_STATE.copy(audioMediaPlayingState = AudioMediaPlayingState.Fetching),
            audioSpeed = AudioSpeed.NORMAL,
            messageStyle = MessageStyle.NORMAL,
            extension = "MP3",
            size = 1024,
            onPlayButtonClick = {},
            onSliderPositionChange = {},
            onAudioSpeedChange = {}
        )
    }
}

@Suppress("MagicNumber")
private val PREVIEW_AUDIO_STATE = AudioState(
    audioMediaPlayingState = AudioMediaPlayingState.SuccessfulFetching,
    currentPositionInMs = 0,
    totalTimeInMs = AudioState.TotalTimeInMs.Known(10000),
    wavesMask = listOf(
        115, 166, 142, 163, 19, 70, 224, 5, 193, 73, 244, 64, 140, 255, 149, 58, 194, 244, 112, 128, 239, 51, 102, 83, 107, 148, 3, 147,
        151, 27, 124, 216, 208, 176, 248, 199, 47, 77, 154, 44, 73, 101, 33, 169, 17, 129, 97, 66, 17, 110, 247, 124, 237, 245, 43, 184,
        198, 196, 175, 195, 60, 66, 81, 109, 185, 206, 38, 130, 248, 206, 43, 156, 184, 9, 65, 40, 42, 18, 134, 41, 140, 234, 105, 130,
        42, 197, 103, 183, 82, 195, 24, 65, 45, 12, 136, 112, 204, 157, 123, 193, 193, 120, 51, 69, 136, 133, 37, 43, 233, 172, 63, 209,
        113, 175, 20, 211, 95, 131, 78, 198, 94, 239, 112, 67, 157, 106, 191, 75, 59, 115, 216, 21, 0, 57, 225, 2, 95, 88, 205, 104, 114,
        156, 24, 210, 69, 232, 141, 65, 102, 219, 36, 166, 252, 40, 129, 16, 240, 60, 33, 29, 219, 32, 5, 243, 39, 8, 89, 196, 250, 48,
        87, 181, 11, 165, 109, 151, 5, 46, 43, 36, 55, 108, 253, 153, 60, 45, 11, 225, 122, 244, 64, 241, 78, 44, 65, 137, 166, 10, 73, 75
    ),
)
