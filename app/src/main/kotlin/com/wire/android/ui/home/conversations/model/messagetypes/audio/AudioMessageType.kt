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
import androidx.compose.material3.Text
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
    onPlayAudioMessage: () -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(dimensions().audioMessageHeight)
        ) {
            when (val test = audioState.audioMediaPlayingState) {
                AudioMediaPlayingState.Completed -> {
                    Text("Is Completed")

                }

                is AudioMediaPlayingState.Paused -> {
                    Text("Is Paused ${audioState.currentPosition}")
                }

                is AudioMediaPlayingState.Playing -> {
                    Text("Is playing ${audioState.currentPosition}")
                }

                is AudioMediaPlayingState.Stopped -> {
                    Text("Is Stoppped ${audioState.currentPosition}")
                }
            }
            Image(
                modifier = Modifier
                    .clickable { onPlayAudioMessage() }
                    .padding(bottom = dimensions().spacing4x)
                    .size(height = dimensions().spacing24x, width = dimensions().spacing24x),
                painter = painterResource(
                    id = R.drawable.ic_speaker_on
                ),
                alignment = Alignment.Center,
                contentDescription = stringResource(R.string.content_description_image_message),
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText)
            )
        }
    }
}
