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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.calling.CallPreview
import com.wire.android.ui.calling.CallState
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.controlButtons.AcceptButton
import com.wire.android.ui.calling.controlButtons.CallOptionsControls
import com.wire.android.ui.calling.controlButtons.DeclineButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow

@Composable
fun IncomingCallScreen(
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
    incomingCallViewModel: IncomingCallViewModel = hiltViewModel()
) {
    val audioPermissionCheck = AudioBluetoothPermissionCheckFlow(
        { incomingCallViewModel.acceptCall() },
        { incomingCallViewModel.declineCall() }
    )

    with(sharedCallingViewModel) {
        IncomingCallContent(
            callState = callState,
            toggleMute = ::toggleMute,
            toggleSpeaker = ::toggleSpeaker,
            toggleVideo = ::toggleVideo,
            declineCall = incomingCallViewModel::declineCall,
            acceptCall = audioPermissionCheck::launch,
            onVideoPreviewCreated = ::setVideoPreview
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
    onVideoPreviewCreated: (view: View) -> Unit
) {

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        topBar = { IncomingCallTopBar { } },
        sheetShape = RoundedCornerShape(dimensions().corner16x, dimensions().corner16x, 0.dp, 0.dp),
        backgroundColor = MaterialTheme.wireColorScheme.callingIncomingBackground,
        sheetGesturesEnabled = false,
        scaffoldState = scaffoldState,
        sheetPeekHeight = dimensions().defaultIncomingCallSheetPeekHeight,
        sheetContent = {
            CallOptionsControls(
                isMuted = callState.isMuted,
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
                    DeclineButton { declineCall() }
                    Text(
                        text = stringResource(id = R.string.calling_label_decline),
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
                        text = stringResource(id = R.string.calling_label_accept),
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
        CallPreview(
            conversationName = callState.conversationName,
            isCameraOn = callState.isCameraOn,
            avatarAssetId = callState.avatarAssetId,
            onVideoPreviewCreated = { onVideoPreviewCreated(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomingCallTopBar(
    onCollapse: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCollapse,
        title = stringResource(id = R.string.calling_label_constant_bit_rate),
        titleStyle = MaterialTheme.wireTypography.title03,
        navigationIconType = NavigationIconType.Collapse,
        elevation = 0.dp,
        actions = {}
    )
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
