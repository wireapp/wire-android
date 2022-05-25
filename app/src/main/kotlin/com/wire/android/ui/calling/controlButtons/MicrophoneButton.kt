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
fun MicrophoneButton(
    isMuted: Boolean = true,
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
            contentDescription = stringResource(id = R.string.calling_hang_up_call)
        )
    }
}

@Preview
@Composable
fun ComposableMicrophoneButtonPreview() {
    MicrophoneButton(onMicrophoneButtonClicked = { })
}
