package com.wire.android.ui.home.conversations.model.messagetypes.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.media.AudioMediaPlayingState
import com.wire.android.model.Clickable
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun AudioMessage(
    audioMediaPlayingState: AudioMediaPlayingState,
    totalTimeInMs: Int,
    currentPositionInMs: Int,
    onPlayAudioMessage: () -> Unit,
    onChangePosition: (Float) -> Unit,
    onLongClick: () -> Unit = {}
) {
    val audioDuration by remember(currentPositionInMs) { mutableStateOf(AudioDuration(totalTimeInMs, currentPositionInMs)) }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions().audioMessageHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WireSecondaryIconButton(
                minWidth = 32.dp,
                iconResource = getPlayOrPauseIcon(audioMediaPlayingState),
                shape = CircleShape,
                contentDescription = R.string.content_description_image_message,
                onButtonClicked = onPlayAudioMessage
            )

            Slider(
                value = audioDuration.currentPositionInMs.toFloat(),
                onValueChange = onChangePosition,
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
}

private fun getPlayOrPauseIcon(audioMediaPlayingState: AudioMediaPlayingState): Int =
    when (audioMediaPlayingState) {
        AudioMediaPlayingState.Completed, AudioMediaPlayingState.Playing -> R.drawable.ic_pause
        else -> R.drawable.ic_play
    }

// helper wrapper class to format the time that is left
private data class AudioDuration(val totalDurationInMs: Int, val currentPositionInMs: Int) {
    private val totalTimeInSec = totalDurationInMs / 1000

    private val currentPositionInSec = currentPositionInMs / 1000
    val formattedTimeLeft
        get() = run {
            val timeLeft = (totalTimeInSec - currentPositionInSec)

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

