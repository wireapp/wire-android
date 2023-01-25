/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.calling.initiating

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.calling.CallState
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.common.CallVideoPreview
import com.wire.android.ui.calling.common.CallerDetails
import com.wire.android.ui.calling.controlbuttons.CallOptionsControls
import com.wire.android.ui.calling.controlbuttons.HangUpButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun InitiatingCallScreen(
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
    initiatingCallViewModel: InitiatingCallViewModel = hiltViewModel()
) {
    with(sharedCallingViewModel) {
        InitiatingCallContent(
            callState = callState,
            toggleMute = ::toggleMute,
            toggleSpeaker = ::toggleSpeaker,
            toggleVideo = ::toggleVideo,
            onHangUpCall = initiatingCallViewModel::hangUpCall,
            onVideoPreviewCreated = ::setVideoPreview,
            onSelfClearVideoPreview = ::clearVideoPreview
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun InitiatingCallContent(
    callState: CallState,
    toggleMute: () -> Unit,
    toggleSpeaker: () -> Unit,
    toggleVideo: () -> Unit,
    onHangUpCall: () -> Unit,
    onVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        sheetShape = RoundedCornerShape(topStart = dimensions().corner16x, topEnd = dimensions().corner16x),
        backgroundColor = colorsScheme().background,
        sheetBackgroundColor = colorsScheme().surface,
        scaffoldState = scaffoldState,
        sheetGesturesEnabled = false,
        sheetPeekHeight = dimensions().defaultInitiatingCallSheetPeekHeight,
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CallOptionsControls(
                    isMuted = callState.isMuted ?: true,
                    isCameraOn = callState.isCameraOn ?: false,
                    isSpeakerOn = callState.isSpeakerOn,
                    toggleSpeaker = toggleSpeaker,
                    toggleMute = toggleMute,
                    toggleVideo = toggleVideo
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                )
                HangUpButton(
                    modifier = Modifier
                        .width(MaterialTheme.wireDimensions.initiatingCallHangUpButtonSize)
                        .height(MaterialTheme.wireDimensions.initiatingCallHangUpButtonSize),
                    onHangUpButtonClicked = onHangUpCall
                )
            }
        }
    ) {
        Box {
            CallVideoPreview(
                isCameraOn = callState.isCameraOn ?: false,
                onVideoPreviewCreated = onVideoPreviewCreated,
                onSelfClearVideoPreview = onSelfClearVideoPreview
            )
            CallerDetails(
                conversationName = callState.conversationName,
                isCameraOn = callState.isCameraOn ?: false,
                avatarAssetId = callState.avatarAssetId,
                conversationType = callState.conversationType,
                membership = callState.membership,
                callingLabel = stringResource(id = R.string.calling_label_ringing_call),
                securityClassificationType = callState.securityClassificationType
            )
        }
    }
}

@Preview
@Composable
fun ComposablePreview() {
    InitiatingCallScreen()
}
