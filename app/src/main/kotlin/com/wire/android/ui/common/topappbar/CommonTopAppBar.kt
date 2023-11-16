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
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.wire.android.R
import com.wire.android.ui.legalhold.banner.LegalHoldStatusBar
import com.wire.android.ui.legalhold.banner.LegalHoldUIState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun CommonTopAppBar(
    commonTopAppBarState: CommonTopAppBarState,
    onReturnToCallClick: (ConnectivityUIState.EstablishedCall) -> Unit,
    onPendingClicked: () -> Unit,
) {
    Column {
        ConnectivityStatusBar(
            connectivityInfo = commonTopAppBarState.connectivityState,
            onReturnToCallClick = onReturnToCallClick
        )
        LegalHoldStatusBar(
            legalHoldState = commonTopAppBarState.legalHoldState,
            onPendingClicked = onPendingClicked
        )
    }
}

@Composable
private fun ConnectivityStatusBar(
    connectivityInfo: ConnectivityUIState,
    onReturnToCallClick: (ConnectivityUIState.EstablishedCall) -> Unit
) {
    val isVisible = connectivityInfo !is ConnectivityUIState.None
    val backgroundColor = when (connectivityInfo) {
        is ConnectivityUIState.EstablishedCall -> MaterialTheme.wireColorScheme.positive
        ConnectivityUIState.Connecting, ConnectivityUIState.WaitingConnection -> MaterialTheme.wireColorScheme.primary
        ConnectivityUIState.None -> MaterialTheme.wireColorScheme.background
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
        .animateContentSize()
        .fillMaxWidth()
        .height(MaterialTheme.wireDimensions.ongoingCallLabelHeight)
        .background(backgroundColor)
        .run {
            if (connectivityInfo is ConnectivityUIState.EstablishedCall) {
                clickable(onClick = { onReturnToCallClick(connectivityInfo) })
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
            when (connectivityInfo) {
                is ConnectivityUIState.EstablishedCall -> OngoingCallContent(connectivityInfo.isMuted)
                ConnectivityUIState.Connecting -> StatusLabel(R.string.connectivity_status_bar_connecting, MaterialTheme.wireColorScheme.onPrimary)
                ConnectivityUIState.WaitingConnection -> StatusLabel(R.string.connectivity_status_bar_waiting_for_network, MaterialTheme.wireColorScheme.onPrimary)
                ConnectivityUIState.None -> {}
            }
        }
    }
}

@Composable
private fun OngoingCallContent(isMuted: Boolean) {
    Row {
        MicrophoneIcon(isMuted, MaterialTheme.wireColorScheme.onPositive)
        CameraIcon(MaterialTheme.wireColorScheme.onPositive)
        StatusLabel(R.string.connectivity_status_bar_return_to_call, MaterialTheme.wireColorScheme.onPositive)
    }
}

@Composable
private fun StatusLabel(stringResource: Int, color: Color = MaterialTheme.wireColorScheme.onPrimary) {
    Text(
        text = stringResource(id = stringResource).uppercase(),
        color = color,
        style = MaterialTheme.wireTypography.title03,
    )
}

@Composable
fun StatusLabel(message: String, color: Color = MaterialTheme.wireColorScheme.onPrimary) {
    Text(
        text = message,
        textAlign = TextAlign.Center,
        color = color,
        style = MaterialTheme.wireTypography.title03,
    )
}

@Composable
private fun CameraIcon(tint: Color = MaterialTheme.wireColorScheme.onPositive) {
    Icon(
        painter = painterResource(id = R.drawable.ic_camera_white_paused),
        contentDescription = stringResource(R.string.content_description_calling_call_paused_camera),
        modifier = Modifier.padding(
            start = MaterialTheme.wireDimensions.spacing8x,
            end = MaterialTheme.wireDimensions.spacing8x
        ),
        tint = tint
    )
}

@Composable
private fun MicrophoneIcon(isMuted: Boolean, tint: Color = MaterialTheme.wireColorScheme.onPositive) {
    Icon(
        painter = painterResource(
            id = if (isMuted) R.drawable.ic_microphone_white_muted
            else R.drawable.ic_microphone_white
        ),
        contentDescription = stringResource(
            id = if (isMuted) R.string.content_description_calling_call_muted
            else R.string.content_description_calling_call_unmuted
        ),
        tint = tint
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

@Composable
private fun PreviewCommonTopAppBar(connectivityUIState: ConnectivityUIState, legalHoldUIState: LegalHoldUIState) {
    WireTheme {
        CommonTopAppBar(CommonTopAppBarState(connectivityUIState, legalHoldUIState), {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityCallNotMuted_LegalHoldNone() =
    PreviewCommonTopAppBar(ConnectivityUIState.EstablishedCall(ConversationId("what", "ever"), false), LegalHoldUIState.None)

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityCallNotMuted_LegalHoldPending() =
    PreviewCommonTopAppBar(ConnectivityUIState.EstablishedCall(ConversationId("what", "ever"), false), LegalHoldUIState.Pending)

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityCallNotMuted_LegalHoldActive() =
    PreviewCommonTopAppBar(ConnectivityUIState.EstablishedCall(ConversationId("what", "ever"), false),LegalHoldUIState.Active)

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityConnecting_LegalHoldNone() =
    PreviewCommonTopAppBar(ConnectivityUIState.Connecting, LegalHoldUIState.None)

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityConnecting_LegalHoldPending() =
    PreviewCommonTopAppBar(ConnectivityUIState.Connecting, LegalHoldUIState.Pending)
@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityConnecting_LegalHoldActive() =
    PreviewCommonTopAppBar(ConnectivityUIState.Connecting, LegalHoldUIState.Active)

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityNone_LegalHoldNone() =
    PreviewCommonTopAppBar(ConnectivityUIState.None, LegalHoldUIState.None)

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityNone_LegalHoldPending() =
    PreviewCommonTopAppBar(ConnectivityUIState.None, LegalHoldUIState.Pending)

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityNone_LegalHoldActive() =
    PreviewCommonTopAppBar(ConnectivityUIState.None, LegalHoldUIState.Active)
