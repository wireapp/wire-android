package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun CameraFlipButton(
    isCameraFlipped: Boolean = false,
    onCameraFlipButtonClicked: () -> Unit
) {
    var isCameraFlipped by remember { mutableStateOf(isCameraFlipped) }

    IconButton(
        modifier = Modifier
            .width(MaterialTheme.wireDimensions.defaultCallingControlsSize)
            .height(MaterialTheme.wireDimensions.defaultCallingControlsSize),
        onClick = onCameraFlipButtonClicked
    ) {
        Image(
            painter = painterResource(
                id = if (isCameraFlipped)
                    R.drawable.ic_flip_camera_on
                else R.drawable.ic_flip_camera_off
            ),
            contentDescription = stringResource(
                id = if (isCameraFlipped) R.string.content_description_calling_flip_camera_on
                else R.string.content_description_calling_flip_camera_off
            ),
        )
    }
}

@Preview
@Composable
fun ComposableCameraFlipButtonPreview() {
    CameraFlipButton(onCameraFlipButtonClicked = { })
}
