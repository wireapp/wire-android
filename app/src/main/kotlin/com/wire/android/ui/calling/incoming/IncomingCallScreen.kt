package com.wire.android.ui.calling.incoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.calling.controlButtons.AcceptButton
import com.wire.android.ui.calling.controlButtons.CameraButton
import com.wire.android.ui.calling.controlButtons.DeclineButton
import com.wire.android.ui.calling.controlButtons.MicrophoneButton
import com.wire.android.ui.calling.controlButtons.SpeakerButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IncomingCallScreen() {
    IncomingCallContent()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun IncomingCallContent() {

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded)
    )
    BottomSheetScaffold(
        topBar = { IncomingCallTopBar { } },
        sheetShape = RoundedCornerShape(MaterialTheme.wireDimensions.corner16x, MaterialTheme.wireDimensions.corner16x, 0.dp, 0.dp),
        backgroundColor = MaterialTheme.wireColorScheme.callingIncomingBackground,
        sheetGesturesEnabled = false,
        scaffoldState = scaffoldState,
        sheetContent = {
            CallingControls()
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Alexandra Olaho",
                style = MaterialTheme.wireTypography.title01,
                modifier = Modifier.padding(0.dp, MaterialTheme.wireDimensions.spacing24x, 0.dp, 0.dp)
            )
            Text(
                text = stringResource(id = R.string.calling_label_incoming_call),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.padding(0.dp, MaterialTheme.wireDimensions.spacing8x, 0.dp, 0.dp)
            )
            UserProfileAvatar(
                size = MaterialTheme.wireDimensions.callingIncomingUserAvatarSize,
                modifier = Modifier.padding(0.dp, MaterialTheme.wireDimensions.spacing56x, 0.dp, 0.dp)
            )
        }
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
private fun CallingControls() {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, MaterialTheme.wireDimensions.spacing32x, 0.dp, 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MicrophoneButton()
            Text(
                text = stringResource(id = R.string.calling_label_microphone),
                style = MaterialTheme.wireTypography.label01,
                modifier = Modifier.padding(0.dp, MaterialTheme.wireDimensions.spacing8x, 0.dp, 0.dp)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CameraButton()
            Text(
                text = stringResource(id = R.string.calling_label_camera),
                style = MaterialTheme.wireTypography.label01,
                modifier = Modifier.padding(0.dp, MaterialTheme.wireDimensions.spacing8x, 0.dp, 0.dp)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpeakerButton()
            Text(
                text = stringResource(id = R.string.calling_label_speaker),
                style = MaterialTheme.wireTypography.label01,
                modifier = Modifier.padding(0.dp, MaterialTheme.wireDimensions.spacing8x, 0.dp, 0.dp)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                MaterialTheme.wireDimensions.spacing40x,
                MaterialTheme.wireDimensions.spacing32x,
                MaterialTheme.wireDimensions.spacing40x,
                0.dp
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(alignment = Alignment.CenterStart)
        ) {
            DeclineButton()
            Text(
                text = stringResource(id = R.string.calling_label_decline),
                style = MaterialTheme.wireTypography.body03,
                modifier = Modifier.padding(0.dp, MaterialTheme.wireDimensions.spacing8x, 0.dp, MaterialTheme.wireDimensions.spacing40x)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
        ) {
            AcceptButton()
            Text(
                text = stringResource(id = R.string.calling_label_accept),
                style = MaterialTheme.wireTypography.body03,
                modifier = Modifier.padding(0.dp, MaterialTheme.wireDimensions.spacing8x, 0.dp, MaterialTheme.wireDimensions.spacing40x)
            )
        }
    }
}

@Preview
@Composable
fun ComposablePreview() {
    IncomingCallScreen()
}
