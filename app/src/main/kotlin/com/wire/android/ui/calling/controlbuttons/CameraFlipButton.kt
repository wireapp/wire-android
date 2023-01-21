package com.wire.android.ui.calling.controlbuttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@Composable
fun CameraFlipButton(
    isCameraFlipped: Boolean = false,
    onCameraFlipButtonClicked: () -> Unit
) {
    WireCallControlButton(
        isCameraFlipped
    ) { iconColor ->
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
            painter = painterResource(id = R.drawable.ic_camera_flip),
            tint = iconColor,
            contentDescription = stringResource(
                id = if (isCameraFlipped) R.string.content_description_calling_flip_camera_on
                else R.string.content_description_calling_flip_camera_off
            )
        )
    }
}

@Preview
@Composable
fun PreviewCameraFlipButton() {
    CameraFlipButton(onCameraFlipButtonClicked = { })
}
