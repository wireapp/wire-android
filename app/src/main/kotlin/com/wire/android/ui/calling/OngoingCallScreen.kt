package com.wire.android.ui.calling

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.wire.android.ui.calling.common.VerticalCallingPager
import com.wire.android.ui.calling.controlButtons.CameraButton
import com.wire.android.ui.calling.controlButtons.CameraFlipButton
import com.wire.android.ui.calling.controlButtons.HangUpButton
import com.wire.android.ui.calling.controlButtons.MicrophoneButton
import com.wire.android.ui.calling.controlButtons.SpeakerButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun OngoingCallScreen(
    ongoingCallViewModel: OngoingCallViewModel,
    sharedCallingViewModel: SharedCallingViewModel,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    OngoingCallContent(sharedCallingViewModel)
    observeScreenLifecycleChanges(
        lifecycleOwner = lifecycleOwner,
        onPauseVideo = sharedCallingViewModel::pauseVideo
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OngoingCallContent(
    sharedCallingViewModel: SharedCallingViewModel
) {
    val sheetState = rememberBottomSheetState(
        initialValue = BottomSheetValue.Collapsed
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )
    with(sharedCallingViewModel) {
        BottomSheetScaffold(
            topBar = {
                val conversationName = callState.conversationName
                OngoingCallTopBar(
                    conversationName = when (conversationName) {
                        is ConversationName.Known -> conversationName.name
                        is ConversationName.Unknown -> stringResource(id = conversationName.resourceId)
                        else -> ""
                    }
                ) { }
            },
            sheetShape = RoundedCornerShape(topStart = dimensions().corner16x, topEnd = dimensions().corner16x),
            sheetPeekHeight = dimensions().defaultSheetPeekHeight,
            scaffoldState = scaffoldState,
            sheetContent = {
                CallingControls(
                    isMuted = callState.isMuted,
                    isCameraOn = callState.isCameraOn,
                    isSpeakerOn = callState.isSpeakerOn,
                    toggleSpeaker = ::toggleSpeaker,
                    toggleMute = ::toggleMute,
                    onHangUpCall = ::hangUpCall,
                    onToggleVideo = ::toggleVideo
                )
            },
        ) {
            Box(
                modifier = Modifier.padding(
                    bottom = 95.dp
                )
            ) {
                VerticalCallingPager(
                    participants = callState.participants,
                    isSelfUserCameraOn = callState.isCameraOn,
                    isSelfUserMuted = callState.isMuted,
                    onSelfVideoPreviewCreated = sharedCallingViewModel::setVideoPreview,
                    onSelfClearVideoPreview = sharedCallingViewModel::clearVideoPreview
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
            .padding(0.dp, dimensions().spacing16x, 0.dp, 0.dp)
    ) {
        MicrophoneButton(isMuted) { toggleMute() }
        CameraButton(
            isCameraOn = isCameraOn,
            onCameraPermissionDenied = { },
            onCameraButtonClicked = {
                onToggleVideo()
            }
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
private fun observeScreenLifecycleChanges(
    lifecycleOwner: LifecycleOwner,
    onPauseVideo: () -> Unit
) {
    // If `lifecycleOwner` changes, dispose and reset the effect
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                onPauseVideo()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
@Preview
fun ComposablePreview() {
    OngoingCallTopBar("Default") { }
}
