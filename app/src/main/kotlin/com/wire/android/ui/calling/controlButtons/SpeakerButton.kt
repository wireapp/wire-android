package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.Image
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
    isSpeakerOn: Boolean = false,
    onSpeakerButtonClicked: () -> Unit
) {
    IconButton(
        modifier = Modifier.width(MaterialTheme.wireDimensions.defaultCallingControlsSize),
        onClick = onSpeakerButtonClicked
    ) {
        Image(
            painter = painterResource(
                id = if (isSpeakerOn)
                    R.drawable.ic_speaker_off
                else R.drawable.ic_speaker_on
            ),
            contentDescription = stringResource(id = R.string.calling_turn_speaker_on_off)
        )
    }
}

@Preview
@Composable
fun ComposableSpeakerButtonPreview() {
    SpeakerButton(onSpeakerButtonClicked = { })
}
