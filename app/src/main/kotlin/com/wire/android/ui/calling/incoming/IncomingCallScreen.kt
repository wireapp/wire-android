package com.wire.android.ui.calling.incoming

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.calling.CallState
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.common.CallVideoPreview
import com.wire.android.ui.calling.common.CallerDetails
import com.wire.android.ui.calling.controlButtons.AcceptButton
import com.wire.android.ui.calling.controlButtons.CallOptionsControls
import com.wire.android.ui.calling.controlButtons.DeclineButton
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow
import com.wire.kalium.logic.data.call.ConversationType

@Composable
fun IncomingCallScreen(
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
    incomingCallViewModel: IncomingCallViewModel = hiltViewModel()
) {
    val showDialog = remember { mutableStateOf(false) }

    val audioPermissionCheck = AudioBluetoothPermissionCheckFlow(
        { incomingCallViewModel.acceptCall() },
        { incomingCallViewModel.declineCall() }
    )

    if (showDialog.value) {
        WireDialog(
            title = stringResource(id = R.string.calling_ongoing_call_title_alert),
            text = stringResource(id = R.string.calling_ongoing_call_join_message_alert),
            onDismiss = { showDialog.value = false },
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { showDialog.value = false },
                text = stringResource(id = R.string.label_cancel),
                type = WireDialogButtonType.Secondary
            ),
            optionButton2Properties = WireDialogButtonProperties(
                onClick = {
                    audioPermissionCheck.launch()
                    showDialog.value = false
                },
                text = stringResource(id = R.string.calling_ongoing_call_join_anyway),
                type = WireDialogButtonType.Primary
            )
        )
    }

    with(sharedCallingViewModel) {
        IncomingCallContent(
            callState = callState,
            toggleMute = ::toggleMute,
            toggleSpeaker = ::toggleSpeaker,
            toggleVideo = ::toggleVideo,
            declineCall = incomingCallViewModel::declineCall,
            acceptCall = {
                incomingCallViewModel.establishedCallConversationId?.let {
                    showDialog.value = true
                } ?: run {
                    audioPermissionCheck.launch()
                }
            },
            onVideoPreviewCreated = ::setVideoPreview,
            onSelfClearVideoPreview = ::clearVideoPreview
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
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

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        sheetShape = RoundedCornerShape(dimensions().corner16x, dimensions().corner16x, 0.dp, 0.dp),
        backgroundColor = MaterialTheme.wireColorScheme.callingIncomingBackground,
        sheetGesturesEnabled = false,
        scaffoldState = scaffoldState,
        sheetPeekHeight = dimensions().defaultIncomingCallSheetPeekHeight,
        sheetContent = {
            CallOptionsControls(
                isMuted = callState.isMuted ?: true,
                isCameraOn = callState.isCameraOn ?: false,
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
                    DeclineButton { declineCall() }
                    Text(
                        text = stringResource(id = R.string.calling_button_label_decline),
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
                    AcceptButton { acceptCall() }
                    Text(
                        text = stringResource(id = R.string.calling_button_label_accept),
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
                isCameraOn = callState.isCameraOn ?: false,
                onVideoPreviewCreated = onVideoPreviewCreated,
                onSelfClearVideoPreview = onSelfClearVideoPreview
            )
            val isCallingString = if (callState.conversationType == ConversationType.Conference) {
                stringResource(R.string.calling_label_incoming_call_someone_calling, callState.callerName ?: "")
            } else stringResource(R.string.calling_label_incoming_call)

            CallerDetails(
                conversationName = callState.conversationName,
                isCameraOn = callState.isCameraOn ?: false,
                avatarAssetId = callState.avatarAssetId,
                conversationType = callState.conversationType,
                membership = callState.membership,
                callingLabel = isCallingString,
                classifiedType = callState.classifiedType
            )
        }
    }
}

@Composable
private fun AudioBluetoothPermissionCheckFlow(
    onAcceptCall: () -> Unit,
    onDeclineCall: () -> Unit
) = rememberCallingRecordAudioBluetoothRequestFlow(onAudioBluetoothPermissionGranted = {
    appLogger.d("IncomingCall - Permissions granted")
    onAcceptCall()
}) {
    appLogger.d("IncomingCall - Permissions denied")
    onDeclineCall()
}

@Preview
@Composable
fun ComposablePreview() {
    IncomingCallScreen()
}
