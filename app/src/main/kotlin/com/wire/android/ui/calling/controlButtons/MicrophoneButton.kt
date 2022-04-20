package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun MicrophoneButton(initialState: Boolean = true) {
    var isMicrophoneOn by remember { mutableStateOf(initialState) }
    IconButton(
        modifier = Modifier.width(MaterialTheme.wireDimensions.defaultCallingControlsSize),
        onClick = { isMicrophoneOn = !isMicrophoneOn }
    ) {
        Image(
            painter = painterResource(
                id = if (isMicrophoneOn) {
                    R.drawable.ic_unmuted
                } else {
                    R.drawable.ic_muted
                }
            ),
            contentDescription = stringResource(id = R.string.calling_hang_up_call)
        )
    }
}
