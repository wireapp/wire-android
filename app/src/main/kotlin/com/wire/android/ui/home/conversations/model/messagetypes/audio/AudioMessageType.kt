package com.wire.android.ui.home.conversations.model.messagetypes.audio

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.media.AudioMediaPlayingState
import com.wire.android.media.AudioState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme


@Composable
fun AudioMessage(
    audioState: AudioState,
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
            when (audioState.audioMediaPlayingState) {
                AudioMediaPlayingState.Completed -> {
                    Image(
                        modifier = Modifier
                            .clickable { onPlayAudioMessage() }
                            .padding(bottom = dimensions().spacing4x)
                            .size(height = dimensions().spacing24x, width = dimensions().spacing24x),
                        painter = painterResource(
                            id = R.drawable.ic_pause_button
                        ),
                        alignment = Alignment.Center,
                        contentDescription = stringResource(R.string.content_description_image_message),
                        colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText)
                    )
                }

                is AudioMediaPlayingState.Paused -> {
                    Image(
                        modifier = Modifier
                            .clickable { onPlayAudioMessage() }
                            .padding(bottom = dimensions().spacing4x)
                            .size(height = dimensions().spacing24x, width = dimensions().spacing24x),
                        painter = painterResource(
                            id = R.drawable.ic_play_button
                        ),
                        alignment = Alignment.Center,
                        contentDescription = stringResource(R.string.content_description_image_message),
                        colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText)
                    )
                }

                is AudioMediaPlayingState.Playing -> {
                    Image(
                        modifier = Modifier
                            .clickable { onPlayAudioMessage() }
                            .padding(bottom = dimensions().spacing4x)
                            .size(height = dimensions().spacing24x, width = dimensions().spacing24x),
                        painter = painterResource(
                            id = R.drawable.ic_pause_button
                        ),
                        alignment = Alignment.Center,
                        contentDescription = stringResource(R.string.content_description_image_message),
                        colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText)
                    )
                }

                is AudioMediaPlayingState.Stopped -> {
                    Image(
                        modifier = Modifier
                            .clickable { onPlayAudioMessage() }
                            .padding(bottom = dimensions().spacing4x)
                            .size(height = dimensions().spacing24x, width = dimensions().spacing24x),
                        painter = painterResource(
                            id = R.drawable.ic_play_button
                        ),
                        alignment = Alignment.Center,
                        contentDescription = stringResource(R.string.content_description_image_message),
                        colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText)
                    )
                }
            }
            Slider(value = audioState.currentPosition.toFloat(), onValueChange = onChangePosition)
        }
    }
}
