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
import com.wire.android.media.AudioMediaPlayingState
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun AudioMessage(
    audioMediaPlayingState: AudioMediaPlayingState,
    totalTimeInMs: Int,
    currentPositionInMs: Int,
    onPlayButtonClick: () -> Unit,
    onSliderPositionChange: (Float) -> Unit,
    onAudioMessageLongClick: () -> Unit
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
        if (audioMediaPlayingState == AudioMediaPlayingState.Failed) {
            FailedAudioMessage()
        } else {
            SuccessFullAudioMessage(
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
fun SuccessFullAudioMessage(
    audioMediaPlayingState: AudioMediaPlayingState,
    totalTimeInMs: Int,
    currentPositionInMs: Int, onPlayButtonClick: () -> Unit,
    onSliderPositionChange: (Float) -> Unit,
    onAudioMessageLongClick: () -> Unit = { }
) {
    val audioDuration by remember(currentPositionInMs) {
        mutableStateOf(
            AudioDuration(totalTimeInMs, currentPositionInMs)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().audioMessageHeight)
            .clickable(Clickable(true, onLongClick = onAudioMessageLongClick)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WireSecondaryIconButton(
            minWidth = 32.dp,
            iconResource = getPlayOrPauseIcon(audioMediaPlayingState),
            shape = CircleShape,
            contentDescription = R.string.content_description_image_message,
            onButtonClicked = onPlayButtonClick
        )

        Slider(
            value = audioDuration.currentPositionInMs.toFloat(),
            onValueChange = onSliderPositionChange,
            valueRange = 0f..audioDuration.totalDurationInMs.toFloat(),
            colors = SliderDefaults.colors(
                inactiveTrackColor = colorsScheme().secondaryButtonDisabledOutline
            ),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = audioDuration.formattedTimeLeft,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.wireColorScheme.secondaryText),
            maxLines = 1
        )
    }
}

@Composable
private fun FailedAudioMessage() {
    var audioNotAvailableDialog by remember { mutableStateOf(false) }

    if (audioNotAvailableDialog) {
        WireDialog(
            title = "Audio not available",
            text = "Something went wrong while downloading this audio file. Please ask the sender to upload it again",
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
            text = "Audio file not available",
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
private data class AudioDuration(val totalDurationInMs: Int, val currentPositionInMs: Int) {
    private val totalTimeInSec = totalDurationInMs / 1000

    private val currentPositionInSec = currentPositionInMs / 1000
    val formattedTimeLeft
        get() = run {
            val isTotalTimeInSecKnown = totalTimeInSec > 0

            val timeLeft = if (!isTotalTimeInSecKnown) {
                currentPositionInSec
            } else {
                (totalTimeInSec - currentPositionInSec)
            }

            val minutes = timeLeft / 60
            val seconds = timeLeft % 60
            val formattedSeconds = String.format("%02d", seconds)

            "$minutes:$formattedSeconds"
        }
}

//@Composable
//private fun PreviewAudioMessage() {
//    AudioMessage(
//        audioMessageDuration = AudioMessageDuration(10000, 0),
//        audioMediaPlayingState = AudioMediaPlayingState.Playing,
//        onPlayAudioMessage = {}
//    ) {}
//}

