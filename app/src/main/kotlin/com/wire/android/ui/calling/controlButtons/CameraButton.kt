package com.wire.android.ui.calling.controlButtons

import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.extension.checkPermission

@Composable
fun CameraButton(
    isCameraOn: Boolean = false,
    onCameraPermissionDenied: () -> Unit,
    onCameraButtonClicked: () -> Unit
) {
    var isCameraOn by remember { mutableStateOf(isCameraOn) }
    val context = LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onCameraButtonClicked()
        } else {
            onCameraPermissionDenied()
        }
    }

    IconButton(
        modifier = Modifier.width(MaterialTheme.wireDimensions.defaultCallingControlsSize),
        onClick = {
            verifyCameraPermission(
                context = context,
                cameraPermissionLauncher = cameraPermissionLauncher
            )
        }
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

private fun verifyCameraPermission(
    context: Context,
    cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    if (context.checkPermission(android.Manifest.permission.CAMERA).not()) {
        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }
}

@Preview
@Composable
fun ComposableCameraButtonPreview() {
    CameraButton(onCameraPermissionDenied = { }, onCameraButtonClicked = { })
}
