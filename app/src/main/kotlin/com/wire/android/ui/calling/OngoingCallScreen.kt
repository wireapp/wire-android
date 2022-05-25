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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.calling.controlButtons.CameraButton
import com.wire.android.ui.calling.controlButtons.HangUpButton
import com.wire.android.ui.calling.controlButtons.MicrophoneButton
import com.wire.android.ui.calling.controlButtons.SpeakerButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OngoingCallScreen(ongoingCallViewModel: OngoingCallViewModel = hiltViewModel()) {
    OngoingCallContent(ongoingCallViewModel)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OngoingCallContent(ongoingCallViewModel: OngoingCallViewModel) {

    val scaffoldState = rememberBottomSheetScaffoldState()
    BottomSheetScaffold(
        topBar = {
            //TODO to handle null name in different way
            OngoingCallTopBar(
                ongoingCallViewModel.callEstablishedState.conversationName
                    ?: stringResource(id = R.string.calling_label_default_caller_name)
            ) {}
        },
        sheetShape = RoundedCornerShape(topStart = dimensions().corner16x, topEnd = dimensions().corner16x),
        backgroundColor = MaterialTheme.wireColorScheme.ongoingCallBackground,
        sheetPeekHeight = dimensions().defaultSheetPeekHeight,
        scaffoldState = scaffoldState,
        sheetContent = {
            with(ongoingCallViewModel) {
                CallingControls(
                    callEstablishedState,
                    { muteOrUnMuteCall() },
                    { hangUpCall() }
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserProfileAvatar(
                userAvatarAsset = ongoingCallViewModel.callEstablishedState.avatarAssetId,
                size = dimensions().onGoingCallUserAvatarSize
            )
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
    ongoingCallState: OngoingCallState,
    onMuteOrUnMuteCall: () -> Unit,
    onHangUpCall: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, dimensions().spacing16x, 0.dp, 0.dp)
    ) {
        MicrophoneButton(ongoingCallState.isMuted) { onMuteOrUnMuteCall() }
        CameraButton(onCameraPermissionDenied = { }, onCameraButtonClicked = { })
        SpeakerButton(onSpeakerButtonClicked = { })
        HangUpButton(
            modifier = Modifier
                .width(MaterialTheme.wireDimensions.defaultCallingControlsSize)
                .height(MaterialTheme.wireDimensions.defaultCallingControlsSize),
        ) { onHangUpCall() }
    }
}

@Preview
@Composable
fun ComposablePreview() {
    OngoingCallTopBar("Default") {}
}
