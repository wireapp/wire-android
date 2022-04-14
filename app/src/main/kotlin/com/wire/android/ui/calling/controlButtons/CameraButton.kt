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
fun CameraButton(initialState: Boolean = false) {
    var isCameraOn by remember { mutableStateOf(initialState) }

    IconButton(
        modifier = Modifier.width(MaterialTheme.wireDimensions.defaultCallingControlsSize),
        onClick = { isCameraOn = !isCameraOn }
    ) {
        Image(
            painter = painterResource(
                id = if (isCameraOn) {
                    R.drawable.ic_camera_on
                } else {
                    R.drawable.ic_camera_off
                }
            ),
            contentDescription = stringResource(id = R.string.calling_turn_camera_on_off),
        )
    }
}
