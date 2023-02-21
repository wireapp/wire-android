/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.common.topappbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.wire.android.R
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun CommonTopAppBar(
    connectivityUIState: ConnectivityUIState,
    onReturnToCallClick: () -> Unit
) {
    ConnectivityStatusBar(
        connectivityInfo = connectivityUIState.info,
        onReturnToCallClick = onReturnToCallClick
    )
}

@Composable
private fun ConnectivityStatusBar(
    connectivityInfo: ConnectivityUIState.Info,
    onReturnToCallClick: () -> Unit
) {
    val isVisible = connectivityInfo !is ConnectivityUIState.Info.None

    // So it keeps the current colour while the animation is collapsing the bar
    val initialColour = MaterialTheme.wireColorScheme.connectivityBarOngoingCallBackgroundColor
    var lastVisibleBackgroundColor by remember {
        mutableStateOf(initialColour)
    }

    val backgroundColor = when (connectivityInfo) {
        is ConnectivityUIState.Info.EstablishedCall ->
            MaterialTheme.wireColorScheme.connectivityBarOngoingCallBackgroundColor

        ConnectivityUIState.Info.Connecting, ConnectivityUIState.Info.WaitingConnection ->
            MaterialTheme.wireColorScheme.primary

        ConnectivityUIState.Info.None -> lastVisibleBackgroundColor
    }
    if (!isVisible) {
        clearStatusBarColor()
    }

    if (isVisible) {
        val darkIcons = MaterialTheme.wireColorScheme.connectivityBarShouldUseDarkIcons
        rememberSystemUiController().setStatusBarColor(
            color = backgroundColor,
            darkIcons = darkIcons
        )
    }

    val barModifier = Modifier
        .fillMaxWidth()
        .height(MaterialTheme.wireDimensions.ongoingCallLabelHeight)
        .background(backgroundColor).run {
            if (connectivityInfo is ConnectivityUIState.Info.EstablishedCall) {
                clickable(onClick = onReturnToCallClick)
            } else this
        }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandIn(initialSize = { fullSize -> IntSize(fullSize.width, 0) }),
        exit = shrinkOut(targetSize = { fullSize -> IntSize(fullSize.width, 0) })
    ) {
        Column(
            modifier = barModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (connectivityInfo is ConnectivityUIState.Info.EstablishedCall) {
                OngoingCallContent(connectivityInfo.isMuted)
            } else {
                val isConnecting = connectivityInfo is ConnectivityUIState.Info.Connecting
                ConnectivityIssueContent(isConnecting)
            }
        }
    }
}

@Composable
private fun ConnectivityIssueContent(isConnecting: Boolean) {
    val stringResource = if (isConnecting) R.string.connectivity_status_bar_connecting
    else R.string.connectivity_status_bar_waiting_for_network

    StatusLabel(stringResource)
}

@Composable
private fun OngoingCallContent(isMuted: Boolean) {
    Row {
        MicrophoneIcon(isMuted)
        CameraIcon()
        StatusLabel(R.string.connectivity_status_bar_return_to_call)
    }
}

@Composable
private fun StatusLabel(stringResource: Int) {
    Text(
        text = stringResource(id = stringResource).uppercase(),
        color = MaterialTheme.wireColorScheme.onPrimary,
        style = MaterialTheme.wireTypography.title03,
    )
}

@Composable
fun StatusLabel(message: String) {
    Text(
        text = message,
        textAlign = TextAlign.Center,
        color = MaterialTheme.wireColorScheme.onPrimary,
        style = MaterialTheme.wireTypography.title03,
    )
}

@Composable
private fun CameraIcon() {
    Icon(
        painter = painterResource(id = R.drawable.ic_camera_white_paused),
        contentDescription = stringResource(R.string.content_description_calling_call_paused_camera),
        modifier = Modifier.padding(
            start = MaterialTheme.wireDimensions.spacing8x,
            end = MaterialTheme.wireDimensions.spacing8x
        ),
        tint = MaterialTheme.wireColorScheme.onPrimary
    )
}

@Composable
private fun MicrophoneIcon(isMuted: Boolean) {
    Icon(
        painter = painterResource(
            id = if (isMuted) R.drawable.ic_microphone_white_muted
            else R.drawable.ic_microphone_white
        ),
        contentDescription = stringResource(
            id = if (isMuted) R.string.content_description_calling_call_muted
            else R.string.content_description_calling_call_unmuted
        ),
        tint = MaterialTheme.wireColorScheme.onPrimary
    )
}

@Composable
private fun clearStatusBarColor() {
    val backgroundColor = MaterialTheme.wireColorScheme.background
    val darkIcons = MaterialTheme.wireColorScheme.useDarkSystemBarIcons

    rememberSystemUiController().setSystemBarsColor(
        color = backgroundColor,
        darkIcons = darkIcons
    )
}

@Preview("is NOT muted")
@Composable
fun PreviewCommonTopAppBarCallIsNotMuted() {
    ConnectivityStatusBar(
        connectivityInfo = ConnectivityUIState.Info.EstablishedCall(
            ConversationId("what", "ever"), false
        ),
        onReturnToCallClick = { }
    )
}

@Preview("is muted")
@Composable
fun PreviewCommonTopAppBarCallIsMuted() {
    ConnectivityStatusBar(
        connectivityInfo = ConnectivityUIState.Info.EstablishedCall(
            ConversationId("what", "ever"), true
        ),
        onReturnToCallClick = { }
    )
}

@Preview("is connecting")
@Composable
fun PreviewCommonTopAppConnectionStatusIsConnecting() {
    ConnectivityStatusBar(
        connectivityInfo = ConnectivityUIState.Info.Connecting,
        onReturnToCallClick = { }
    )
}

@Preview("is waiting connection")
@Composable
fun PreviewCommonTopAppConnectionStatusIsWaitingConnection() {
    ConnectivityStatusBar(
        connectivityInfo = ConnectivityUIState.Info.WaitingConnection,
        onReturnToCallClick = { }
    )
}

@Preview("is None")
@Composable
fun PreviewCommonTopAppConnectionStatusIsNone() {
    ConnectivityStatusBar(
        connectivityInfo = ConnectivityUIState.Info.None,
        onReturnToCallClick = { }
    )
}
