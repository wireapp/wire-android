package com.wire.android.ui.calling

import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.calling.common.VerticalCallingPager
import com.wire.android.ui.calling.controlButtons.CameraButton
import com.wire.android.ui.calling.controlButtons.CameraFlipButton
import com.wire.android.ui.calling.controlButtons.HangUpButton
import com.wire.android.ui.calling.controlButtons.MicrophoneButton
import com.wire.android.ui.calling.controlButtons.SpeakerButton
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun OngoingCallScreen(
    ongoingCallViewModel: OngoingCallViewModel = hiltViewModel(),
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel()
) {

    with(sharedCallingViewModel) {
        OngoingCallContent(
            callState.conversationName,
            callState.participants,
            callState.isMuted ?: true,
            callState.isCameraOn ?: false,
            callState.isSpeakerOn,
            ::toggleSpeaker,
            ::toggleMute,
            ::hangUpCall,
            ::toggleVideo,
            ::setVideoPreview,
            ::clearVideoPreview,
            ::navigateBack
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OngoingCallContent(
    conversationName: ConversationName?,
    participants: List<UICallParticipant>,
    isMuted: Boolean,
    isCameraOn: Boolean,
    isSpeakerOn: Boolean,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    hangUpCall: () -> Unit,
    toggleVideo: () -> Unit,
    setVideoPreview: (view: View) -> Unit,
    clearVideoPreview: () -> Unit,
    navigateBack: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = BottomSheetValue.Collapsed
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )
    BottomSheetScaffold(
        topBar = {
            OngoingCallTopBar(
                conversationName = when (conversationName) {
                    is ConversationName.Known -> conversationName.name
                    is ConversationName.Unknown -> stringResource(id = conversationName.resourceId)
                    else -> ""
                },
                onCollapse = navigateBack
            )
        },
        sheetShape = RoundedCornerShape(topStart = dimensions().corner16x, topEnd = dimensions().corner16x),
        sheetPeekHeight = dimensions().defaultSheetPeekHeight,
        scaffoldState = scaffoldState,
        sheetContent = {
            CallingControls(
                isMuted = isMuted,
                isCameraOn = isCameraOn,
                isSpeakerOn = isSpeakerOn,
                toggleSpeaker = toggleSpeaker,
                toggleMute = toggleMute,
                onHangUpCall = hangUpCall,
                onToggleVideo = toggleVideo
            )
        },
    ) {
        Box(
            modifier = Modifier.padding(
                bottom = 95.dp
            )
        ) {
            VerticalCallingPager(
                participants = participants,
                isSelfUserCameraOn = isCameraOn,
                isSelfUserMuted = isMuted,
                onSelfVideoPreviewCreated = setVideoPreview,
                onSelfClearVideoPreview = clearVideoPreview
            )
        }
    }
}

@Composable
private fun OngoingCallTopBar(
    conversationName: String,
    onCollapse: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCollapse,
        titleStyle = MaterialTheme.wireTypography.title02,
        maxLines = 1,
        title = conversationName,
        navigationIconType = NavigationIconType.Collapse,
        elevation = 0.dp,
        actions = {}
    )
}

//TODO(refactor) use CallOptionsControls to avoid duplication
@Composable
private fun CallingControls(
    isMuted: Boolean,
    isCameraOn: Boolean,
    isSpeakerOn: Boolean,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    onHangUpCall: () -> Unit,
    onToggleVideo: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensions().spacing16x)
    ) {
        MicrophoneButton(isMuted) { toggleMute() }
        CameraButton(
            isCameraOn = isCameraOn,
            onCameraPermissionDenied = { },
            onCameraButtonClicked = onToggleVideo
        )
        if (isCameraOn) {
            val context = LocalContext.current
            CameraFlipButton {
                Toast.makeText(context, "Not implemented yet =)", Toast.LENGTH_SHORT).show()
            }
        } else SpeakerButton(
            isSpeakerOn = isSpeakerOn,
            onSpeakerButtonClicked = toggleSpeaker
        )

        HangUpButton(
            modifier = Modifier
                .width(MaterialTheme.wireDimensions.defaultCallingHangUpButtonSize)
                .height(MaterialTheme.wireDimensions.defaultCallingHangUpButtonSize),
            onHangUpButtonClicked = onHangUpCall
        )
    }
}

@Composable
@Preview
fun ComposablePreview() {
    OngoingCallTopBar("Default") { }
}
