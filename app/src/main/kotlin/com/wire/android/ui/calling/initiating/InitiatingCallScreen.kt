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
import com.wire.android.R
import com.wire.android.ui.calling.controlButtons.CallOptionsControls
import com.wire.android.ui.calling.controlButtons.HangUpButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY

@Composable
fun InitiatingCallScreen() {
    InitiatingCallContent()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InitiatingCallContent() {

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        topBar = { InitiatingCallTopBar { } },
        sheetShape = RoundedCornerShape(topStart = MaterialTheme.wireDimensions.corner16x, topEnd = MaterialTheme.wireDimensions.corner16x),
        backgroundColor = MaterialTheme.wireColorScheme.initiatingCallBackground,
        scaffoldState = scaffoldState,
        sheetGesturesEnabled = false,
        sheetPeekHeight = MaterialTheme.wireDimensions.defaultInitiatingCallSheetPeekHeight,
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
                        .height(72.dp)
                        .width(72.dp)
                ) { }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Name here",
                style = MaterialTheme.wireTypography.title01,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing24x)
            )
            Text(
                text = stringResource(id = R.string.calling_label_ringing_call),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing8x)
            )
            UserProfileAvatar(
                size = MaterialTheme.wireDimensions.initiatingCallUserAvatarSize,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing16x)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InitiatingCallTopBar(
    onCollapse: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCollapse,
        title = String.EMPTY,
        navigationIconType = NavigationIconType.Close,
        elevation = 0.dp,
        actions = { }
    )
}

@Preview
@Composable
fun ComposablePreview() {
    InitiatingCallScreen()
}
