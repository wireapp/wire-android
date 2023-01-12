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
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.media.AudioMediaPlayerState
import com.wire.android.media.AudioState
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography


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
            when (val test = audioState.audioMediaPlayerState) {
                AudioMediaPlayerState.Completed -> {
                    Text("Is Completed")

                }

                is AudioMediaPlayerState.Paused -> {
                    Text("Is Paused ${test.currentPosition}")
                }

                is AudioMediaPlayerState.Playing -> {
                    Text("Is playing ${test.currentPosition}")
                }

                is AudioMediaPlayerState.Stopped -> {
                    Text("Is Stoppped ${test.currentPosition}")
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
