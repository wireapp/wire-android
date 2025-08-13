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

package com.wire.android.ui.calling.outgoing

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.common.CallVideoPreview
import com.wire.android.ui.calling.common.CallerDetails
import com.wire.android.ui.calling.common.ObserveRotation
import com.wire.android.ui.calling.common.SharedCallingViewModel
import com.wire.android.ui.calling.controlbuttons.CallOptionsControls
import com.wire.android.ui.calling.controlbuttons.HangUpButton
import com.wire.android.ui.calling.model.CallState
import com.wire.android.ui.common.bottomsheet.WireBottomSheetScaffold
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.kalium.logic.data.id.ConversationId

@Suppress("ParameterWrapping")
@Composable
fun OutgoingCallScreen(
    conversationId: ConversationId,
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel<SharedCallingViewModel, SharedCallingViewModel.Factory>(
        key = "shared_$conversationId",
        creationCallback = { factory -> factory.create(conversationId = conversationId) }
    ),
    outgoingCallViewModel: OutgoingCallViewModel = hiltViewModel<OutgoingCallViewModel, OutgoingCallViewModel.Factory>(
        key = "outgoing_$conversationId",
        creationCallback = { factory -> factory.create(conversationId = conversationId) }
    ),
    onCallAccepted: () -> Unit
) {
    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val activity = LocalActivity.current

    LaunchedEffect(outgoingCallViewModel.state.flowState) {
        when (outgoingCallViewModel.state.flowState) {
            OutgoingCallState.FlowState.CallClosed -> {
                activity.finishAndRemoveTask()
            }

            OutgoingCallState.FlowState.CallEstablished -> {
                onCallAccepted()
            }

            OutgoingCallState.FlowState.Default -> {
                /* do nothing */
            }
        }
    }
    with(sharedCallingViewModel) {
        OutgoingCallContent(
            callState = callState,
            toggleMute = ::toggleMute,
            toggleSpeaker = ::toggleSpeaker,
            toggleVideo = ::toggleVideo,
            onHangUpCall = outgoingCallViewModel::hangUpCall,
            onVideoPreviewCreated = ::setVideoPreview,
            onSelfClearVideoPreview = ::clearVideoPreview,
            onCameraPermissionPermanentlyDenied = {
                permissionPermanentlyDeniedDialogState.show(
                    PermissionPermanentlyDeniedDialogState.Visible(
                        title = R.string.app_permission_dialog_title,
                        description = R.string.camera_permission_dialog_description
                    )
                )
            },
            onMinimiseScreen = {
                activity.moveTaskToBack(true)
            }
        )
        ObserveRotation(::setUIRotation)
    }

    PermissionPermanentlyDeniedDialog(
        dialogState = permissionPermanentlyDeniedDialogState,
        hideDialog = permissionPermanentlyDeniedDialogState::dismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutgoingCallContent(
    callState: CallState,
    toggleMute: () -> Unit,
    toggleSpeaker: () -> Unit,
    toggleVideo: () -> Unit,
    onHangUpCall: () -> Unit,
    onVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit,
    onCameraPermissionPermanentlyDenied: () -> Unit,
    onMinimiseScreen: () -> Unit
) {
    BackHandler {
        // DO NOTHING
    }

    val scaffoldState = rememberBottomSheetScaffoldState()

    WireBottomSheetScaffold(
        sheetDragHandle = null,
        scaffoldState = scaffoldState,
        sheetSwipeEnabled = false,
        sheetPeekHeight = dimensions().defaultOutgoingCallSheetPeekHeight,
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
                    onCameraPermissionPermanentlyDenied = onCameraPermissionPermanentlyDenied
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                )
                HangUpButton(
                    size = dimensions().bigCallingControlsSize,
                    iconSize = dimensions().bigCallingHangUpButtonIconSize,
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
                conversationTypeForCall = callState.conversationTypeForCall,
                membership = callState.membership,
                groupCallerName = null,
                protocolInfo = callState.protocolInfo,
                mlsVerificationStatus = callState.mlsVerificationStatus,
                proteusVerificationStatus = callState.proteusVerificationStatus,
                onMinimiseScreen = onMinimiseScreen,
                accentId = callState.accentId
            )
        }
    }
}

@Preview
@Composable
fun PreviewOutgoingCallScreen() {
    OutgoingCallContent(CallState(ConversationId("value", "domain")), {}, {}, {}, {}, {}, {}, {}, {})
}
