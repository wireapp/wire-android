package com.wire.android.ui.calling.controlbuttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.dimensions

@Composable
fun CameraFlipButton(
    isCameraFlipped: Boolean = false,
    onCameraFlipButtonClicked: () -> Unit
) {
    var isCameraFlipped by remember { mutableStateOf(isCameraFlipped) }

    IconButton(
        modifier = Modifier.size(dimensions().defaultCallingControlsSize),
        onClick = {}
    ) {
        Icon(
            modifier = Modifier
                .wrapContentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, radius = dimensions().defaultCallingControlsSize / 2),
                    role = Role.Button,
                    onClick = {
                        onCameraFlipButtonClicked()
                    }
                ),
            painter = painterResource(
                id = if (isCameraFlipped)
                    R.drawable.ic_flip_camera_on
                else R.drawable.ic_flip_camera_off
            ),
            contentDescription = stringResource(
                id = if (isCameraFlipped) R.string.content_description_calling_flip_camera_on
                else R.string.content_description_calling_flip_camera_off
            ),
            tint = Color.Unspecified
        )
    }
}

@Preview
@Composable
fun ComposableCameraFlipButtonPreview() {
    CameraFlipButton(onCameraFlipButtonClicked = { })
}
