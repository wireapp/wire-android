package com.wire.android.ui.home.conversations.model.messagetypes.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.media.AudioMediaPlayingState
import com.wire.android.media.AudioState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.dimensions

@Composable
fun AudioMessage(
    durationInMs: Long,
    audioMediaPlayingState: AudioMediaPlayingState,
    currentPositionInMs: Int,
    onPlayAudioMessage: () -> Unit,
    onChangePosition: (Float) -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(dimensions().audioMessageHeight)
        ) {
            when (audioMediaPlayingState) {
                AudioMediaPlayingState.Completed -> {
                    WireSecondaryIconButton(
                        minWidth = 32.dp,
                        iconResource = R.drawable.ic_play,
                        shape = CircleShape,
                        contentDescription = R.string.content_description_image_message,
                        onButtonClicked = onPlayAudioMessage
                    )
                }

                is AudioMediaPlayingState.Paused -> {
                    WireSecondaryIconButton(
                        minWidth = 32.dp,
                        iconResource = R.drawable.ic_play,
                        shape = CircleShape,
                        contentDescription = R.string.content_description_image_message,
                        onButtonClicked = onPlayAudioMessage
                    )
                }

                is AudioMediaPlayingState.Playing -> {
                    WireSecondaryIconButton(
                        minWidth = 32.dp,
                        iconResource = R.drawable.ic_pause,
                        shape = CircleShape,
                        contentDescription = R.string.content_description_image_message,
                        onButtonClicked = onPlayAudioMessage
                    )
                }

                is AudioMediaPlayingState.Stopped -> {
                    WireSecondaryIconButton(
                        minWidth = 32.dp,
                        iconResource = R.drawable.ic_play,
                        shape = CircleShape,
                        contentDescription = R.string.content_description_image_message,
                        onButtonClicked = onPlayAudioMessage
                    )
                }
            }

            Slider(value = currentPositionInMs.toFloat(), onValueChange = onChangePosition, valueRange = 0f..durationInMs.toFloat())
        }
    }
}

