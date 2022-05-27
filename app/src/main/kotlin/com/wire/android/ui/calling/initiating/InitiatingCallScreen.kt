package com.wire.android.ui.calling.initiating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.wire.android.ui.calling.CallState
import com.wire.android.ui.calling.ConversationName
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.controlButtons.CallOptionsControls
import com.wire.android.ui.calling.controlButtons.HangUpButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY

@Composable
fun InitiatingCallScreen(
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
    initiatingCallViewModel: InitiatingCallViewModel = hiltViewModel()
) {
    InitiatingCallContent(
        callState = sharedCallingViewModel.callState,
        onNavigateBack = { sharedCallingViewModel.navigateBack() },
        onHangUpCall = { sharedCallingViewModel.hangUpCall() }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InitiatingCallContent(
    callState: CallState,
    onNavigateBack: () -> Unit,
    onHangUpCall: () -> Unit
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
                CallOptionsControls()
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                )
                HangUpButton(
                    modifier = Modifier
                        .height(dimensions().initiatingCallHangUpButtonSize)
                        .width(dimensions().initiatingCallHangUpButtonSize)
                ) { onHangUpCall() }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (callState.conversationName) {
                    is ConversationName.Known -> callState.conversationName.name
                    is ConversationName.Unknown -> stringResource(id = callState.conversationName.resourceId)
                    else -> ""
                },
                style = MaterialTheme.wireTypography.title01,
                modifier = Modifier.padding(top = dimensions().spacing24x)
            )
            Text(
                text = stringResource(id = R.string.calling_label_ringing_call),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.padding(top = dimensions().spacing8x)
            )
            UserProfileAvatar(
                userAvatarAsset = callState.avatarAssetId,
                size = dimensions().initiatingCallUserAvatarSize,
                modifier = Modifier.padding(top = dimensions().spacing16x)
            )
        }
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
