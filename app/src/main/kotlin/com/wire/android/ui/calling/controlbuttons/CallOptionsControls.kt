/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.calling.controlbuttons

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun CallOptionsControls(
    isMuted: Boolean,
    isCameraOn: Boolean,
    isSpeakerOn: Boolean,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    toggleVideo: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.wireDimensions.spacing32x)
    ) {
        val (microphoneIcon, microphoneText, cameraIcon, cameraText, speakerIcon, speakerText) = createRefs()
        MicrophoneButton(
            modifier = Modifier
                .size(dimensions().defaultCallingControlsSize)
                .constrainAs(microphoneIcon) {
                    start.linkTo(parent.start)
                    end.linkTo(cameraIcon.start)
                },
            isMuted = isMuted,
            onMicrophoneButtonClicked = toggleMute
        )
        CallControlLabel(stringResource(id = R.string.calling_button_label_microphone), microphoneText, microphoneIcon)
        CameraButton(
            modifier = Modifier
                .size(dimensions().defaultCallingControlsSize)
                .constrainAs(cameraIcon) {
                    start.linkTo(microphoneIcon.end)
                    end.linkTo(speakerIcon.start)
                },
            isCameraOn = isCameraOn,
            onCameraPermissionDenied = { },
            onCameraButtonClicked = toggleVideo
        )
        CallControlLabel(stringResource(id = R.string.calling_button_label_camera), cameraText, cameraIcon)
        SpeakerButton(
            modifier = Modifier
                .size(dimensions().defaultCallingControlsSize)
                .constrainAs(speakerIcon) {
                    start.linkTo(cameraIcon.end)
                    end.linkTo(parent.end)
                },
            isSpeakerOn = isSpeakerOn,
            onSpeakerButtonClicked = toggleSpeaker
        )
        CallControlLabel(stringResource(id = R.string.calling_button_label_speaker), speakerText, speakerIcon)
    }
}

@Composable
private fun ConstraintLayoutScope.CallControlLabel(
    stringResource: String,
    constraints: ConstrainedLayoutReference,
    linkedButton: ConstrainedLayoutReference
) {
    Text(
        text = stringResource.uppercase(),
        color = colorsScheme().onSurface,
        style = MaterialTheme.wireTypography.label01,
        modifier = Modifier
            .padding(top = MaterialTheme.wireDimensions.spacing8x)
            .constrainAs(constraints) {
                start.linkTo(linkedButton.start)
                end.linkTo(linkedButton.end)
                top.linkTo(linkedButton.bottom)
            },
    )
}

@Preview
@Composable
fun PreviewCallOptionsControls() {
    CallOptionsControls(
        isMuted = true,
        isCameraOn = false,
        isSpeakerOn = false,
        toggleSpeaker = { },
        toggleMute = { },
        toggleVideo = { }
    )
}
