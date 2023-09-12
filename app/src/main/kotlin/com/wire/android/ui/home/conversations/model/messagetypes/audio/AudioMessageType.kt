package com.wire.android.ui.home.conversations.model.messagetypes.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioMediaPlayingState
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun AudioMessage(
    audioMediaPlayingState: AudioMediaPlayingState,
    totalTimeInMs: AudioState.TotalTimeInMs,
    currentPositionInMs: Int,
    onPlayButtonClick: () -> Unit,
    onSliderPositionChange: (Float) -> Unit,
    onAudioMessageLongClick: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .padding(top = dimensions().spacing4x)
            .background(
                color = MaterialTheme.wireColorScheme.onPrimary,
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .padding(dimensions().spacing8x)
    ) {
        if (audioMediaPlayingState is AudioMediaPlayingState.Failed) {
            FailedAudioMessage()
        } else {
            SuccessfulAudioMessage(
                audioMediaPlayingState = audioMediaPlayingState,
                totalTimeInMs = totalTimeInMs,
                currentPositionInMs = currentPositionInMs,
                onPlayButtonClick = onPlayButtonClick,
                onSliderPositionChange = onSliderPositionChange,
                onAudioMessageLongClick = onAudioMessageLongClick
            )
        }
    }
}

@Composable
fun RecordedAudioMessage(
    audioMediaPlayingState: AudioMediaPlayingState,
    totalTimeInMs: AudioState.TotalTimeInMs,
    currentPositionInMs: Int,
    onPlayButtonClick: () -> Unit,
    onSliderPositionChange: (Float) -> Unit,
    onAudioMessageLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.apply {
            padding(top = dimensions().spacing4x)
            padding(dimensions().spacing8x)
        }
    ) {
        SuccessfulAudioMessage(
            modifier = Modifier.padding(horizontal = dimensions().spacing24x),
            audioMediaPlayingState = audioMediaPlayingState,
            totalTimeInMs = totalTimeInMs,
            currentPositionInMs = currentPositionInMs,
            onPlayButtonClick = onPlayButtonClick,
            onSliderPositionChange = onSliderPositionChange,
            onAudioMessageLongClick = onAudioMessageLongClick
        )
    }
}

@Composable
private fun SuccessfulAudioMessage(
    modifier: Modifier = Modifier,
    audioMediaPlayingState: AudioMediaPlayingState,
    totalTimeInMs: AudioState.TotalTimeInMs,
    currentPositionInMs: Int,
    onPlayButtonClick: () -> Unit,
    onSliderPositionChange: (Float) -> Unit,
    onAudioMessageLongClick: (() -> Unit)? = null
) {
    val audioDuration by remember(currentPositionInMs) {
        mutableStateOf(
            AudioDuration(totalTimeInMs, currentPositionInMs)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions().audioMessageHeight)
            .clickable(Clickable(enabled = onAudioMessageLongClick != null, onLongClick = onAudioMessageLongClick)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WireSecondaryIconButton(
            minSize = dimensions().buttonSmallMinSize,
            minClickableSize = dimensions().buttonMinClickableSize,
            iconSize = dimensions().spacing12x,
            iconResource = getPlayOrPauseIcon(audioMediaPlayingState),
            shape = CircleShape,
            contentDescription = R.string.content_description_image_message,
            state = if (audioMediaPlayingState is AudioMediaPlayingState.Fetching) WireButtonState.Disabled else WireButtonState.Default,
            onButtonClicked = onPlayButtonClick
        )

        Slider(
            value = audioDuration.currentPositionInMs.toFloat(),
            onValueChange = onSliderPositionChange,
            valueRange = 0f..if (totalTimeInMs is AudioState.TotalTimeInMs.Known) totalTimeInMs.value.toFloat() else 0f,
            colors = SliderDefaults.colors(
                inactiveTrackColor = colorsScheme().secondaryButtonDisabledOutline
            ),
            modifier = Modifier.weight(1f)
        )

        if (audioMediaPlayingState is AudioMediaPlayingState.Fetching) {
            WireCircularProgressIndicator(
                progressColor = MaterialTheme.wireColorScheme.secondaryButtonEnabled
            )
        } else {
            Text(
                text = audioDuration.formattedTimeLeft(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.wireColorScheme.secondaryText,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FailedAudioMessage() {
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

private fun getPlayOrPauseIcon(audioMediaPlayingState: AudioMediaPlayingState): Int =
    when (audioMediaPlayingState) {
        AudioMediaPlayingState.Playing -> R.drawable.ic_pause
        AudioMediaPlayingState.Completed -> R.drawable.ic_play
        else -> R.drawable.ic_play
    }

// helper wrapper class to format the time that is left
private data class AudioDuration(val totalDurationInMs: AudioState.TotalTimeInMs, val currentPositionInMs: Int) {
    companion object {
        const val totalMsInSec = 1000
        const val totalSecInMin = 60
        const val UNKNOWN_DURATION_LABEL = "-:--"
    }

    fun formattedTimeLeft(): String {
        if (totalDurationInMs is AudioState.TotalTimeInMs.Known) {
            val totalTimeInSec = totalDurationInMs.value / totalMsInSec
            val currentPositionInSec = currentPositionInMs / totalMsInSec

            val isTotalTimeInSecKnown = totalTimeInSec > 0

            val timeLeft = if (!isTotalTimeInSecKnown) {
                currentPositionInSec
            } else {
                totalTimeInSec - currentPositionInSec
            }

            // sanity check, timeLeft, should not be smaller, however if the back-end makes mistake we
            // will display a negative values, which we do not want
            val minutes = if (timeLeft < 0) 0 else timeLeft / totalSecInMin
            val seconds = if (timeLeft < 0) 0 else timeLeft % totalSecInMin
            val formattedSeconds = String.format("%02d", seconds)

            return "$minutes:$formattedSeconds"
        }

        return UNKNOWN_DURATION_LABEL
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewSuccessfulAudioMessage() {
    WireTheme {
        SuccessfulAudioMessage(
            audioMediaPlayingState = AudioMediaPlayingState.Completed,
            totalTimeInMs = AudioState.TotalTimeInMs.Known(10000),
            currentPositionInMs = 5000,
            onPlayButtonClick = {},
            onSliderPositionChange = {}
        )
    }
}
