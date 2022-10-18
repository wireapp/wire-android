package com.wire.android.ui.calling.controlbuttons

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
fun MicrophoneButton(
    isMuted: Boolean,
    onMicrophoneButtonClicked: () -> Unit
) {
    IconButton(
        modifier = Modifier
            .width(MaterialTheme.wireDimensions.defaultCallingControlsSize)
            .height(MaterialTheme.wireDimensions.defaultCallingControlsSize),
        onClick = onMicrophoneButtonClicked
    ) {
        Image(
            painter = painterResource(
                id = if (isMuted) {
                    R.drawable.ic_muted
                } else {
                    R.drawable.ic_unmuted
                }
            ),
            contentDescription = stringResource(
                id = if (isMuted) R.string.content_description_calling_unmute_call
                else R.string.content_description_calling_mute_call
            )
        )
    }
}

@Preview
@Composable
fun ComposableMicrophoneButtonPreview() {
    MicrophoneButton(isMuted = true, onMicrophoneButtonClicked = { })
}
