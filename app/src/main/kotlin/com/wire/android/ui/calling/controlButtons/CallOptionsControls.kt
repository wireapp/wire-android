package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun CallOptionsControls() {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.wireDimensions.spacing32x)
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MicrophoneButton(isMuted = true) { }
            Text(
                text = stringResource(id = R.string.calling_label_microphone).uppercase(),
                style = MaterialTheme.wireTypography.label01,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing8x)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CameraButton(isCameraOn = false, onCameraPermissionDenied = { }) { }
            Text(
                text = stringResource(id = R.string.calling_label_camera).uppercase(),
                style = MaterialTheme.wireTypography.label01,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing8x)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CameraFlipButton(isCameraFlipped = false) { }
            Text(
                text = stringResource(id = R.string.calling_label_flip).uppercase(),
                style = MaterialTheme.wireTypography.label01,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing8x)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpeakerButton(isSpeakerOn = true) { }
            Text(
                text = stringResource(id = R.string.calling_label_speaker).uppercase(),
                style = MaterialTheme.wireTypography.label01,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing8x)
            )
        }
    }
}

@Preview
@Composable
fun ComposablePreview() {
    CallOptionsControls()
}
