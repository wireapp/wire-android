package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun SpeakerButton(
    isSpeakerOn: Boolean,
    onSpeakerButtonClicked: () -> Unit
) {
    IconButton(
        modifier = Modifier
            .width(MaterialTheme.wireDimensions.defaultCallingControlsSize)
            .height(MaterialTheme.wireDimensions.defaultCallingControlsSize),
        onClick = onSpeakerButtonClicked
    ) {
        Image(
            painter = painterResource(
                id = if (isSpeakerOn)
                    R.drawable.ic_speaker_on
                else R.drawable.ic_speaker_off
            ),
            contentDescription = stringResource(
                id = if (isSpeakerOn) R.string.content_description_calling_turn_speaker_off
                else R.string.content_description_calling_turn_speaker_on
            )
        )
    }
}

@Preview
@Composable
fun ComposableSpeakerButtonPreview() {
    SpeakerButton(isSpeakerOn = true, onSpeakerButtonClicked = { })
}
