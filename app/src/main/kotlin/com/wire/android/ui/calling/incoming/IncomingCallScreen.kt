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

package com.wire.android.ui.calling.incoming

import android.content.Intent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.AppLockActivity
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.CallActivity
import com.wire.android.ui.calling.CallState
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.common.CallVideoPreview
import com.wire.android.ui.calling.common.CallerDetails
import com.wire.android.ui.calling.controlbuttons.AcceptButton
import com.wire.android.ui.calling.controlbuttons.CallOptionsControls
import com.wire.android.ui.calling.controlbuttons.HangUpButton
import com.wire.android.ui.common.bottomsheet.WireBottomSheetScaffold
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.PermissionDenialType
import com.wire.android.util.permission.rememberCallingRecordAudioRequestFlow
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun IncomingCallScreen(
    conversationId: ConversationId,
    incomingCallViewModel: IncomingCallViewModel = hiltViewModel<IncomingCallViewModel, IncomingCallViewModel.Factory>(
        creationCallback = { factory -> factory.create(conversationId = conversationId) }
    ),
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel<SharedCallingViewModel, SharedCallingViewModel.Factory>(
        creationCallback = { factory -> factory.create(conversationId = conversationId) }
    ),
    onCallAccepted: () -> Unit
) {
    val activity = LocalActivity.current

    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val audioPermissionCheck = AudioPermissionCheckFlow(
        onAcceptCall = {
            incomingCallViewModel.acceptCall {
                (activity as CallActivity).openAppLockActivity()
            }
        },
        onPermanentPermissionDecline = {
            permissionPermanentlyDeniedDialogState.show(
                PermissionPermanentlyDeniedDialogState.Visible(
                    title = R.string.app_permission_dialog_title,
                    description = R.string.call_permission_dialog_description
                )
            )
        }
    )

    with(incomingCallViewModel) {
        if (incomingCallState.shouldShowJoinCallAnywayDialog) {
            JoinAnywayDialog(
                onDismiss = ::dismissJoinCallAnywayDialog,
                onConfirm = {
                    acceptCallAnyway {
                        (activity as CallActivity).openAppLockActivity()
                    }
                }
            )
        }
    }
    LaunchedEffect(incomingCallViewModel.incomingCallState.flowState) {
        when (incomingCallViewModel.incomingCallState.flowState) {
            is IncomingCallState.FlowState.CallClosed -> {
                activity.finish()
            }

            is IncomingCallState.FlowState.CallAccepted -> {
                onCallAccepted()
            }

            is IncomingCallState.FlowState.Default -> { /* do nothing */
            }
        }
    }
    with(sharedCallingViewModel) {
        IncomingCallContent(
            callState = callState,
            toggleMute = { sharedCallingViewModel.toggleMute(true) },
            toggleSpeaker = ::toggleSpeaker,
            toggleVideo = ::toggleVideo,
            declineCall = {
                incomingCallViewModel.declineCall(
                    onAppLocked = {
                        (activity as CallActivity).openAppLockActivity()
                    },
                    onCallRejected = {
                        activity.finish()
                    }
                )
            },
            acceptCall = audioPermissionCheck::launch,
            onVideoPreviewCreated = ::setVideoPreview,
            onSelfClearVideoPreview = ::clearVideoPreview,
            onPermissionPermanentlyDenied = {
                if (it is PermissionDenialType.CallingCamera) {
                    permissionPermanentlyDeniedDialogState.show(
                        PermissionPermanentlyDeniedDialogState.Visible(
                            title = R.string.app_permission_dialog_title,
                            description = R.string.camera_permission_dialog_description
                        )
                    )
                }
            }
        )
    }

    PermissionPermanentlyDeniedDialog(
        dialogState = permissionPermanentlyDeniedDialogState,
        hideDialog = permissionPermanentlyDeniedDialogState::dismiss
    )
}

fun CallActivity.openAppLockActivity() {
    Intent(this, AppLockActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    }.run {
        startActivity(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomingCallContent(
    callState: CallState,
    toggleMute: () -> Unit,
    toggleSpeaker: () -> Unit,
    toggleVideo: () -> Unit,
    declineCall: () -> Unit,
    acceptCall: () -> Unit,
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
        sheetSwipeEnabled = false,
        scaffoldState = scaffoldState,
        sheetPeekHeight = dimensions().defaultIncomingCallSheetPeekHeight,
        sheetContent = {
            CallOptionsControls(
                isMuted = callState.isMuted ?: true,
                isCameraOn = callState.isCameraOn,
                isSpeakerOn = callState.isSpeakerOn,
                toggleSpeaker = toggleSpeaker,
                toggleMute = toggleMute,
                toggleVideo = toggleVideo,
                onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensions().spacing40x,
                        top = dimensions().spacing32x,
                        end = dimensions().spacing40x
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(alignment = Alignment.CenterStart)
                ) {
                    HangUpButton(
                        modifier = Modifier.size(dimensions().initiatingCallHangUpButtonSize),
                        onHangUpButtonClicked = { declineCall() }
                    )
                    Text(
                        text = stringResource(id = R.string.calling_button_label_decline),
                        color = colorsScheme().onSurface,
                        style = MaterialTheme.wireTypography.body03,
                        modifier = Modifier.padding(
                            top = dimensions().spacing8x,
                            bottom = dimensions().spacing40x
                        )
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd)
                ) {
                    AcceptButton(
                        buttonClicked = acceptCall
                    )
                    Text(
                        text = stringResource(id = R.string.calling_button_label_accept),
                        color = colorsScheme().onSurface,
                        style = MaterialTheme.wireTypography.body03,
                        modifier = Modifier.padding(
                            top = dimensions().spacing8x,
                            bottom = dimensions().spacing40x
                        )
                    )
                }
            }
        },
    ) {
        Box {
            CallVideoPreview(
                isCameraOn = callState.isCameraOn,
                onVideoPreviewCreated = onVideoPreviewCreated,
                onSelfClearVideoPreview = onSelfClearVideoPreview
            )
            val isCallingString = if (callState.conversationType == ConversationType.Conference) {
                stringResource(R.string.calling_label_incoming_call_someone_calling, callState.callerName ?: "")
            } else stringResource(R.string.calling_label_incoming_call)

            CallerDetails(
                conversationId = callState.conversationId,
                conversationName = callState.conversationName,
                isCameraOn = callState.isCameraOn,
                isCbrEnabled = callState.isCbrEnabled,
                avatarAssetId = callState.avatarAssetId,
                conversationType = callState.conversationType,
                membership = callState.membership,
                callingLabel = isCallingString,
                protocolInfo = callState.protocolInfo,
                mlsVerificationStatus = callState.mlsVerificationStatus,
                proteusVerificationStatus = callState.proteusVerificationStatus
            )
        }
    }
}

fun launchLockScreenForIncomingCallScreen() {
    appLogger.d("IncomingCall - App locked")
}

@Composable
fun AudioPermissionCheckFlow(
    onAcceptCall: () -> Unit,
    onPermanentPermissionDecline: () -> Unit,
) = rememberCallingRecordAudioRequestFlow(
    onAudioPermissionGranted = {
        appLogger.d("IncomingCall - Audio permission granted")
        onAcceptCall()
    },
    onAudioPermissionDenied = { /* Nothing to do */ },
    onAudioPermissionPermanentlyDenied = onPermanentPermissionDecline
)

@Preview
@Composable
fun PreviewIncomingCallScreen() {
    IncomingCallContent(
        callState = CallState(ConversationId("value", "domain")),
        toggleMute = { },
        toggleSpeaker = { },
        toggleVideo = { },
        declineCall = { },
        acceptCall = { },
        onVideoPreviewCreated = { },
        onSelfClearVideoPreview = { },
        onPermissionPermanentlyDenied = { },
    )
}
