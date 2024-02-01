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

package com.wire.android.ui.calling.initiating

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.style.KeepOnScreenPopUpNavigationAnimation
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.calling.CallState
import com.wire.android.ui.calling.CallingNavArgs
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.common.CallVideoPreview
import com.wire.android.ui.calling.common.CallerDetails
import com.wire.android.ui.calling.controlbuttons.CallOptionsControls
import com.wire.android.ui.calling.controlbuttons.HangUpButton
import com.wire.android.ui.common.bottomsheet.WireBottomSheetScaffold
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.destinations.OngoingCallScreenDestination
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.permission.PermissionDenialType
import com.wire.kalium.logic.data.id.ConversationId

@RootNavGraph
@Destination(
    navArgsDelegate = CallingNavArgs::class,
    style = KeepOnScreenPopUpNavigationAnimation::class
)
@Composable
fun InitiatingCallScreen(
    navigator: Navigator,
    navArgs: CallingNavArgs,
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
    initiatingCallViewModel: InitiatingCallViewModel = hiltViewModel()
) {
    LaunchedEffect(initiatingCallViewModel.state.flowState) {
        when (initiatingCallViewModel.state.flowState) {
            InitiatingCallState.FlowState.CallClosed -> navigator.navigateBack()
            InitiatingCallState.FlowState.CallEstablished ->
                navigator.navigate(NavigationCommand(OngoingCallScreenDestination(navArgs.conversationId), BackStackMode.REMOVE_CURRENT))

            InitiatingCallState.FlowState.Default -> { /* do nothing */
            }
        }
    }
    with(sharedCallingViewModel) {
        InitiatingCallContent(
            callState = callState,
            toggleMute = ::toggleMute,
            toggleSpeaker = ::toggleSpeaker,
            toggleVideo = ::toggleVideo,
            onHangUpCall = initiatingCallViewModel::hangUpCall,
            onVideoPreviewCreated = ::setVideoPreview,
            onSelfClearVideoPreview = ::clearVideoPreview,
            onPermissionPermanentlyDenied = {
                if (it is PermissionDenialType.CallingCamera) {
                    sharedCallingViewModel.showPermissionPermanentlyDeniedDialog(
                        title = R.string.app_permission_dialog_title,
                        description = R.string.camera_permission_dialog_description
                    )
                }
            }
        )
    }
    PermissionPermanentlyDeniedDialog(
        dialogState = sharedCallingViewModel.permissionPermanentlyDeniedDialogState,
        hideDialog = sharedCallingViewModel::hidePermissionPermanentlyDeniedDialog
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InitiatingCallContent(
    callState: CallState,
    toggleMute: () -> Unit,
    toggleSpeaker: () -> Unit,
    toggleVideo: () -> Unit,
    onHangUpCall: () -> Unit,
    onVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit
) {
    BackHandler {
        // DO NOTHING
    }

    val scaffoldState = rememberBottomSheetScaffoldState()

    WireBottomSheetScaffold(
        sheetDragHandle = null,
        scaffoldState = scaffoldState,
        sheetSwipeEnabled = false,
        sheetPeekHeight = dimensions().defaultInitiatingCallSheetPeekHeight,
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CallOptionsControls(
                    isMuted = callState.isMuted ?: true,
                    isCameraOn = callState.isCameraOn,
                    isSpeakerOn = callState.isSpeakerOn,
                    toggleSpeaker = toggleSpeaker,
                    toggleMute = toggleMute,
                    toggleVideo = toggleVideo,
                    onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
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
                isCameraOn = callState.isCameraOn,
                onVideoPreviewCreated = onVideoPreviewCreated,
                onSelfClearVideoPreview = onSelfClearVideoPreview
            )
            CallerDetails(
                callState.conversationId,
                conversationName = callState.conversationName,
                isCameraOn = callState.isCameraOn,
                isCbrEnabled = callState.isCbrEnabled,
                avatarAssetId = callState.avatarAssetId,
                conversationType = callState.conversationType,
                membership = callState.membership,
                callingLabel = stringResource(id = R.string.calling_label_ringing_call),
                protocolInfo = callState.protocolInfo,
                mlsVerificationStatus = callState.mlsVerificationStatus,
                proteusVerificationStatus = callState.proteusVerificationStatus,
            )
        }
    }
}

@Preview
@Composable
fun PreviewInitiatingCallScreen() {
    InitiatingCallContent(CallState(ConversationId("value", "domain")), {}, {}, {}, {}, {}, {}, {})
}
