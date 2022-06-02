package com.wire.android.ui.calling.initiating

import android.view.View
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.calling.CallPreview
import com.wire.android.ui.calling.CallState
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.controlButtons.CallOptionsControls
import com.wire.android.ui.calling.controlButtons.HangUpButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.EMPTY

@Composable
fun InitiatingCallScreen(
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
    initiatingCallViewModel: InitiatingCallViewModel = hiltViewModel()
) {
    InitiatingCallContent(
        callState = sharedCallingViewModel.callState,
        toggleMute = sharedCallingViewModel::toggleMute,
        toggleVideo = sharedCallingViewModel::toggleVideo,
        onNavigateBack = sharedCallingViewModel::navigateBack,
        onHangUpCall = sharedCallingViewModel::hangUpCall,
        onVideoPreviewCreated = { sharedCallingViewModel.setVideoPreview(it) }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InitiatingCallContent(
    callState: CallState,
    toggleMute: () -> Unit,
    toggleVideo: () -> Unit,
    onNavigateBack: () -> Unit,
    onHangUpCall: () -> Unit,
    onVideoPreviewCreated: (view: View) -> Unit
) {

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        topBar = { InitiatingCallTopBar { onNavigateBack() } },
        sheetShape = RoundedCornerShape(topStart = dimensions().corner16x, topEnd = dimensions().corner16x),
        backgroundColor = MaterialTheme.wireColorScheme.initiatingCallBackground,
        scaffoldState = scaffoldState,
        sheetGesturesEnabled = false,
        sheetPeekHeight = dimensions().defaultInitiatingCallSheetPeekHeight,
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CallOptionsControls(
                    isMuted = callState.isMuted,
                    isCameraOn = callState.isCameraOn,
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
private fun InitiatingCallTopBar(
    onNavigateBack: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        title = String.EMPTY,
        navigationIconType = null,
        elevation = 0.dp,
        actions = { }
    )
}

@Preview
@Composable
fun ComposablePreview() {
    InitiatingCallScreen()
}
