package com.wire.android.ui.calling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.waz.avs.VideoPreview
import com.wire.android.ui.calling.controlButtons.CameraButton
import com.wire.android.ui.calling.controlButtons.HangUpButton
import com.wire.android.ui.calling.controlButtons.MicrophoneButton
import com.wire.android.ui.calling.controlButtons.SpeakerButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OngoingCallScreen(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    ongoingCallViewModel: OngoingCallViewModel = hiltViewModel(),
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel()
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

    val scaffoldState = rememberBottomSheetScaffoldState()
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
            backgroundColor = MaterialTheme.wireColorScheme.ongoingCallBackground,
            sheetPeekHeight = dimensions().defaultSheetPeekHeight,
            scaffoldState = scaffoldState,
            sheetContent = {
                CallingControls(
                    isMuted = callState.isMuted,
                    isCameraOn = callState.isCameraOn,
                    toggleMute = ::toggleMute,
                    onHangUpCall = ::hangUpCall,
                    onToggleVideo = ::toggleVideo
                )

            },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (callState.isCameraOn) {
                    //TODO fix memory leak when the app goes to background with video turned on
                    // https://issuetracker.google.com/issues/198012639
                    // The issue is marked as fixed in the issue tracker,
                    // but we are still getting it with our current compose version 1.2.0-beta01
                    AndroidView(factory = {
                        val videoPreview = VideoPreview(it)
                        sharedCallingViewModel.setVideoPreview(videoPreview)
                        videoPreview
                    })
                } else sharedCallingViewModel.clearVideoPreview()
                UserProfileAvatar(
                    userAvatarAsset = callState.avatarAssetId,
                    size = dimensions().onGoingCallUserAvatarSize
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OngoingCallTopBar(
    conversationName: String,
    onCollapse: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCollapse,
        title = conversationName,
        navigationIconType = NavigationIconType.Collapse,
        elevation = 0.dp,
        actions = {}
    )
}

@Composable
private fun CallingControls(
    isMuted: Boolean,
    isCameraOn: Boolean,
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
        SpeakerButton(onSpeakerButtonClicked = { })
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

@Preview
@Composable
fun ComposablePreview() {
    OngoingCallTopBar("Default") { }
}
