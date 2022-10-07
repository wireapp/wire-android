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
fun CommonTopAppBar(connectivityUIState: ConnectivityUIState, onReturnToCallClick: () -> Unit) {
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
    if (!isVisible) {
        clearStatusBarColor()
    }
    // So it keeps the current colour while the animation is collapsing the bar
    val initialColour = MaterialTheme.wireColorScheme.connectivityBarOngoingCallBackgroundColor
    var lastVisibleBackgroundColor by remember {
        mutableStateOf(initialColour)
    }

    val backgroundColor = when (connectivityInfo) {
        is ConnectivityUIState.Info.EstablishedCall ->
            MaterialTheme.wireColorScheme.connectivityBarOngoingCallBackgroundColor

        ConnectivityUIState.Info.Connecting, ConnectivityUIState.Info.WaitingConnection ->
            MaterialTheme.wireColorScheme.connectivityBarIssueBackgroundColor

        ConnectivityUIState.Info.None -> lastVisibleBackgroundColor
    }

    if (isVisible) {
        lastVisibleBackgroundColor = backgroundColor
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
        color = MaterialTheme.wireColorScheme.connectivityBarTextColor,
        style = MaterialTheme.wireTypography.title03,
    )
}

@Composable
fun StatusLabel(message: String) {
    Text(
        text = message,
        textAlign = TextAlign.Center,
        color = MaterialTheme.wireColorScheme.connectivityBarTextColor,
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
        tint = MaterialTheme.wireColorScheme.connectivityBarIconColor
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
        tint = MaterialTheme.wireColorScheme.connectivityBarIconColor
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
fun CommonTopAppBarCallIsNotMuted() {
    ConnectivityStatusBar(
        connectivityInfo = ConnectivityUIState.Info.EstablishedCall(
            ConversationId("what", "ever"), false
        ),
        onReturnToCallClick = { }
    )
}

@Preview("is muted")
@Composable
fun CommonTopAppBarCallIsMuted() {
    ConnectivityStatusBar(
        connectivityInfo = ConnectivityUIState.Info.EstablishedCall(
            ConversationId("what", "ever"), true
        ),
        onReturnToCallClick = { }
    )
}

@Preview("is connecting")
@Composable
fun CommonTopAppConnectionStatusIsConnecting() {
    ConnectivityStatusBar(
        connectivityInfo = ConnectivityUIState.Info.Connecting,
        onReturnToCallClick = { }
    )
}

@Preview("is waiting connection")
@Composable
fun CommonTopAppConnectionStatusIsWaitingConnection() {
    ConnectivityStatusBar(
        connectivityInfo = ConnectivityUIState.Info.WaitingConnection,
        onReturnToCallClick = { }
    )
}

@Preview("is None")
@Composable
fun CommonTopAppConnectionStatusIsNone() {
    ConnectivityStatusBar(
        connectivityInfo = ConnectivityUIState.Info.None,
        onReturnToCallClick = { }
    )
}
