package com.wire.android.ui.calling.ongoing

import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
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
import com.wire.android.R
import com.wire.android.ui.calling.ConversationName
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.controlbuttons.CameraButton
import com.wire.android.ui.calling.controlbuttons.CameraFlipButton
import com.wire.android.ui.calling.controlbuttons.HangUpButton
import com.wire.android.ui.calling.controlbuttons.MicrophoneButton
import com.wire.android.ui.calling.controlbuttons.SpeakerButton
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.participantsview.VerticalCallingPager
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.CommonTopAppBar
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.common.topappbar.ConnectivityUIState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun OngoingCallScreen(
    ongoingCallViewModel: OngoingCallViewModel = hiltViewModel(),
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
    commonTopAppBarViewModel: CommonTopAppBarViewModel = hiltViewModel(),
) {

    with(sharedCallingViewModel.callState) {
        OngoingCallContent(
            conversationName,
            participants,
            isMuted ?: true,
            isCameraOn ?: false,
            isSpeakerOn,
            securityClassificationType,
            commonTopAppBarViewModel.connectivityState,
            sharedCallingViewModel::toggleSpeaker,
            sharedCallingViewModel::toggleMute,
            sharedCallingViewModel::hangUpCall,
            sharedCallingViewModel::toggleVideo,
            sharedCallingViewModel::setVideoPreview,
            sharedCallingViewModel::clearVideoPreview,
            sharedCallingViewModel::navigateBack,
            ongoingCallViewModel::requestVideoStreams
        )
        isCameraOn?.let {
            BackHandler(enabled = it, sharedCallingViewModel::navigateBack)
        }
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
    classificationType: SecurityClassificationType,
    connectivityState: ConnectivityUIState,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    hangUpCall: () -> Unit,
    toggleVideo: () -> Unit,
    setVideoPreview: (view: View) -> Unit,
    clearVideoPreview: () -> Unit,
    navigateBack: () -> Unit,
    requestVideoStreams: (participants: List<UICallParticipant>) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = if (classificationType == SecurityClassificationType.NONE) BottomSheetValue.Collapsed else BottomSheetValue.Expanded
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )
    BottomSheetScaffold(
        topBar = {
            CommonTopAppBar(
                connectivityUIState = connectivityState,
                onReturnToCallClick = { }
            )
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
                classificationType = classificationType,
                toggleSpeaker = toggleSpeaker,
                toggleMute = toggleMute,
                onHangUpCall = hangUpCall,
                onToggleVideo = toggleVideo
            )
        },
    ) {
        if (participants.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WireCircularProgressIndicator(
                    progressColor = MaterialTheme.wireColorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    size = dimensions().spacing32x
                )
                Text(
                    text = stringResource(id = R.string.connectivity_status_bar_connecting),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        } else {
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
                    onSelfClearVideoPreview = clearVideoPreview,
                    requestVideoStreams = requestVideoStreams
                )
            }
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
    classificationType: SecurityClassificationType,
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
            modifier = Modifier.size(MaterialTheme.wireDimensions.defaultCallingHangUpButtonSize),
            onHangUpButtonClicked = onHangUpCall
        )
    }
    SecurityClassificationBanner(classificationType)
}

@Composable
@Preview
fun ComposablePreview() {
    OngoingCallTopBar("Default") { }
}
