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

package com.wire.android.ui.calling.incoming

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.WakeUpScreenPopUpNavigationAnimation
import com.wire.android.ui.calling.CallState
import com.wire.android.ui.calling.CallingNavArgs
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.common.CallVideoPreview
import com.wire.android.ui.calling.common.CallerDetails
import com.wire.android.ui.calling.common.MicrophonePermissionDeniedDialog
import com.wire.android.ui.calling.controlbuttons.AcceptButton
import com.wire.android.ui.calling.controlbuttons.CallOptionsControls
import com.wire.android.ui.calling.controlbuttons.HangUpButton
import com.wire.android.ui.common.bottomsheet.WireBottomSheetScaffold
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.destinations.OngoingCallScreenDestination
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.extension.openAppInfoScreen
import com.wire.android.util.permission.rememberCallingRecordAudioRequestFlow
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.id.ConversationId

@RootNavGraph
@Destination(
    navArgsDelegate = CallingNavArgs::class,
    style = WakeUpScreenPopUpNavigationAnimation::class
)
@Composable
fun IncomingCallScreen(
    navigator: Navigator,
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
    incomingCallViewModel: IncomingCallViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val audioPermissionCheck = AudioPermissionCheckFlow(
        incomingCallViewModel::acceptCall,
        incomingCallViewModel::showPermissionDialog
    )

    MicrophonePermissionDeniedDialog(
        shouldShow = incomingCallViewModel.incomingCallState.shouldShowPermissionDialog,
        onDismiss = incomingCallViewModel::dismissPermissionDialog,
        onOpenSettings = {
            context.openAppInfoScreen()
        }
    )

    with(incomingCallViewModel) {
        if (incomingCallState.shouldShowJoinCallAnywayDialog) {
            JoinAnywayDialog(
                onDismiss = ::dismissJoinCallAnywayDialog,
                onConfirm = ::acceptCallAnyway
            )
        }
    }
    LaunchedEffect(incomingCallViewModel.incomingCallState.flowState) {
        when (val flowState = incomingCallViewModel.incomingCallState.flowState) {
            is IncomingCallState.FlowState.CallClosed -> navigator.navigateBack()
            is IncomingCallState.FlowState.CallAccepted -> navigator.navigate(
                NavigationCommand(
                    OngoingCallScreenDestination(flowState.conversationId),
                    BackStackMode.REMOVE_CURRENT_AND_REPLACE
                )
            )

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
            declineCall = incomingCallViewModel::declineCall,
            acceptCall = audioPermissionCheck::launch,
            onVideoPreviewCreated = ::setVideoPreview,
            onSelfClearVideoPreview = ::clearVideoPreview
        )
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
    onSelfClearVideoPreview: () -> Unit
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
                toggleVideo = toggleVideo
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

@Composable
fun AudioPermissionCheckFlow(
    onAcceptCall: () -> Unit,
    onPermanentPermissionDecline: () -> Unit,
) = rememberCallingRecordAudioRequestFlow(
    onAudioPermissionGranted = {
        appLogger.d("IncomingCall - Audio permission granted")
        onAcceptCall()
    },
    onAudioPermissionDenied = { },
    onAudioPermissionPermanentlyDenied = onPermanentPermissionDecline
)

@Preview
@Composable
fun PreviewIncomingCallScreen() {
    IncomingCallContent(CallState(ConversationId("value", "domain")), {}, {}, {}, {}, {}, {}, {})
}
