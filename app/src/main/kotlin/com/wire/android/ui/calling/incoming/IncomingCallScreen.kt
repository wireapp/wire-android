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

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.CallActivity
import com.wire.android.ui.calling.common.CallVideoPreview
import com.wire.android.ui.calling.common.CallerDetails
import com.wire.android.ui.calling.common.ObserveRotation
import com.wire.android.ui.calling.common.SharedCallingViewModel
import com.wire.android.ui.calling.controlbuttons.AcceptButton
import com.wire.android.ui.calling.controlbuttons.CallOptionsControls
import com.wire.android.ui.calling.controlbuttons.HangUpButton
import com.wire.android.ui.calling.model.CallState
import com.wire.android.ui.calling.model.ConversationName
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.bottomsheet.WireBottomSheetScaffold
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.rememberRecordAudioPermissionFlow
import com.wire.kalium.logic.data.call.ConversationTypeForCall
import com.wire.kalium.logic.data.id.ConversationId

@Suppress("ParameterWrapping")
@Composable
fun IncomingCallScreen(
    conversationId: ConversationId,
    shouldTryToAnswerCallAutomatically: Boolean,
    incomingCallViewModel: IncomingCallViewModel = hiltViewModel<IncomingCallViewModel, IncomingCallViewModel.Factory>(
        key = "incoming_$conversationId",
        creationCallback = { factory -> factory.create(conversationId = conversationId) }
    ),
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel<SharedCallingViewModel, SharedCallingViewModel.Factory>(
        key = "shared_$conversationId",
        creationCallback = { factory -> factory.create(conversationId = conversationId) }
    ),
    onCallAccepted: () -> Unit
) {
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val audioPermissionCheck = AudioPermissionCheckFlow(
        onAcceptCall = incomingCallViewModel::acceptCall,
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
                onConfirm = ::acceptCallAnyway,
            )
        }
    }
    LaunchedEffect(incomingCallViewModel.incomingCallState.flowState) {
        when (incomingCallViewModel.incomingCallState.flowState) {
            is IncomingCallState.FlowState.CallClosed -> {
                activity.finishAndRemoveTask()
            }

            is IncomingCallState.FlowState.CallAccepted -> {
                onCallAccepted()
            }

            is IncomingCallState.FlowState.Default -> { /* do nothing */
            }
        }
    }
    LaunchedEffect(shouldTryToAnswerCallAutomatically) {
        if (shouldTryToAnswerCallAutomatically) {
            appLogger.d("IncomingCall - Trying to automatically accept the call")
            audioPermissionCheck.launch()
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) incomingCallViewModel.bringBackNotificationIfNeeded()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            incomingCallViewModel.bringBackNotificationIfNeeded()
        }
    }
    HandleActions(incomingCallViewModel.actions) { action ->
        when (action) {
            IncomingCallViewActions.AppLocked -> (activity as CallActivity).openAppLockActivity()
            is IncomingCallViewActions.RejectedCall -> activity.finishAndRemoveTask()
        }
    }

    with(sharedCallingViewModel) {
        IncomingCallContent(
            callState = callState,
            toggleMute = { sharedCallingViewModel.toggleMute(true) },
            toggleVideo = ::toggleVideo,
            declineCall = incomingCallViewModel::declineCall,
            acceptCall = audioPermissionCheck::launch,
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
private fun IncomingCallContent(
    callState: CallState,
    toggleMute: () -> Unit,
    toggleVideo: () -> Unit,
    declineCall: () -> Unit,
    acceptCall: () -> Unit,
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
        sheetSwipeEnabled = false,
        scaffoldState = scaffoldState,
        sheetPeekHeight = dimensions().defaultIncomingCallSheetPeekHeight,
        sheetContent = {
            CallOptionsControls(
                isMuted = callState.isMuted ?: true,
                isCameraOn = callState.isCameraOn,
                isSpeakerOn = callState.isSpeakerOn,
                toggleSpeaker = {},
                toggleMute = toggleMute,
                toggleVideo = toggleVideo,
                shouldShowSpeakerButton = false,
                onCameraPermissionPermanentlyDenied = onCameraPermissionPermanentlyDenied
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
                        size = dimensions().bigCallingControlsSize,
                        iconSize = dimensions().bigCallingHangUpButtonIconSize,
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
                        size = dimensions().bigCallingControlsSize,
                        iconSize = dimensions().bigCallingAcceptButtonIconSize,
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

            val groupCallerName = if (callState.conversationTypeForCall == ConversationTypeForCall.Conference) {
                callState.callerName
            } else {
                null
            }

            CallerDetails(
                conversationId = callState.conversationId,
                conversationName = callState.conversationName,
                isCameraOn = callState.isCameraOn,
                isCbrEnabled = callState.isCbrEnabled,
                avatarAssetId = callState.avatarAssetId,
                conversationTypeForCall = callState.conversationTypeForCall,
                membership = callState.membership,
                groupCallerName = groupCallerName,
                protocolInfo = callState.protocolInfo,
                mlsVerificationStatus = callState.mlsVerificationStatus,
                proteusVerificationStatus = callState.proteusVerificationStatus,
                onMinimiseScreen = onMinimiseScreen,
                accentId = callState.accentId
            )
        }
    }
}

@Composable
fun AudioPermissionCheckFlow(
    onAcceptCall: () -> Unit,
    onPermanentPermissionDecline: () -> Unit,
) = rememberRecordAudioPermissionFlow(
    onPermissionGranted = {
        appLogger.d("IncomingCall - Audio permission granted")
        onAcceptCall()
    },
    onPermissionDenied = { /* Nothing to do */ },
    onPermissionPermanentlyDenied = onPermanentPermissionDecline
)

@Preview
@Composable
fun PreviewIncomingOneOnOneCallScreen() {
    IncomingCallContent(
        callState = CallState(
            conversationId = ConversationId("value", "domain"),
            conversationName = ConversationName.Known("Jon Doe"),
            conversationTypeForCall = ConversationTypeForCall.OneOnOne
        ),
        toggleMute = { },
        toggleVideo = { },
        declineCall = { },
        acceptCall = { },
        onVideoPreviewCreated = { },
        onSelfClearVideoPreview = { },
        onCameraPermissionPermanentlyDenied = { },
        onMinimiseScreen = { }
    )
}

@Preview
@Composable
fun PreviewIncomingGroupCallScreen() {
    IncomingCallContent(
        callState = CallState(
            conversationId = ConversationId("value", "domain"),
            conversationName = ConversationName.Known("Fake group name"),
            callerName = "Jon Doe",
            conversationTypeForCall = ConversationTypeForCall.Conference
        ),
        toggleMute = { },
        toggleVideo = { },
        declineCall = { },
        acceptCall = { },
        onVideoPreviewCreated = { },
        onSelfClearVideoPreview = { },
        onCameraPermissionPermanentlyDenied = { },
        onMinimiseScreen = { }
    )
}
