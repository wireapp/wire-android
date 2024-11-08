/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.wire.android.R
import com.wire.android.ui.theme.ThemeOption
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun CommonTopAppBar(
    themeOption: ThemeOption,
    commonTopAppBarState: CommonTopAppBarState,
    onReturnToCallClick: (ConnectivityUIState.Call.Established) -> Unit,
    onReturnToIncomingCallClick: (ConnectivityUIState.Call.Incoming) -> Unit,
    onReturnToOutgoingCallClick: (ConnectivityUIState.Call.Outgoing) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ConnectivityStatusBar(
            themeOption = themeOption,
            connectivityInfo = commonTopAppBarState.connectivityState,
            onReturnToCallClick = onReturnToCallClick,
            onReturnToIncomingCallClick = onReturnToIncomingCallClick,
            onReturnToOutgoingCallClick = onReturnToOutgoingCallClick
        )
    }
}

@Composable
fun getBackgroundColor(connectivityInfo: ConnectivityUIState): Color {
    return when (connectivityInfo) {
        is ConnectivityUIState.Calls -> MaterialTheme.wireColorScheme.positive
        ConnectivityUIState.Connecting, ConnectivityUIState.WaitingConnection -> MaterialTheme.wireColorScheme.primary
        ConnectivityUIState.None -> MaterialTheme.wireColorScheme.background
    }
}

@Composable
private fun ConnectivityStatusBar(
    themeOption: ThemeOption,
    connectivityInfo: ConnectivityUIState,
    onReturnToCallClick: (ConnectivityUIState.Call.Established) -> Unit,
    onReturnToIncomingCallClick: (ConnectivityUIState.Call.Incoming) -> Unit,
    onReturnToOutgoingCallClick: (ConnectivityUIState.Call.Outgoing) -> Unit
) {
    val isVisible = connectivityInfo !is ConnectivityUIState.None
    val backgroundColor = getBackgroundColor(connectivityInfo)

    if (isVisible) {
        val darkIcons = MaterialTheme.wireColorScheme.connectivityBarShouldUseDarkIcons
        val systemUiController = rememberSystemUiController()
        systemUiController.setStatusBarColor(
            color = backgroundColor,
            darkIcons = darkIcons
        )
        LaunchedEffect(themeOption) {
            systemUiController.setStatusBarColor(
                color = backgroundColor,
                darkIcons = darkIcons
            )
        }
    } else {
        ClearStatusBarColor()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandIn(initialSize = { fullSize -> IntSize(fullSize.width, 0) }),
        exit = shrinkOut(targetSize = { fullSize -> IntSize(fullSize.width, 0) })
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth()
                .heightIn(min = MaterialTheme.wireDimensions.ongoingCallLabelHeight)
                .background(backgroundColor),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (connectivityInfo) {
                is ConnectivityUIState.Calls ->
                    CallsContent(
                        calls = connectivityInfo.calls,
                        onReturnToCallClick = onReturnToCallClick,
                        onReturnToIncomingCallClick = onReturnToIncomingCallClick,
                        onReturnToOutgoingCallClick = onReturnToOutgoingCallClick
                    )

                ConnectivityUIState.Connecting ->
                    StatusLabel(
                        R.string.connectivity_status_bar_connecting,
                        MaterialTheme.wireColorScheme.onPrimary
                    )

                ConnectivityUIState.WaitingConnection ->
                    StatusLabel(
                        R.string.connectivity_status_bar_waiting_for_network,
                        MaterialTheme.wireColorScheme.onPrimary
                    )

                ConnectivityUIState.None -> {}
            }
        }
    }
}

@Composable
private fun CallsContent(
    calls: List<ConnectivityUIState.Call>,
    onReturnToCallClick: (ConnectivityUIState.Call.Established) -> Unit,
    onReturnToIncomingCallClick: (ConnectivityUIState.Call.Incoming) -> Unit,
    onReturnToOutgoingCallClick: (ConnectivityUIState.Call.Outgoing) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing12x)) {
        calls.forEach { call ->
            when (call) {
                is ConnectivityUIState.Call.Established -> OngoingCallContent(
                    isMuted = call.isMuted,
                    modifier = Modifier
                        .clickable(
                            onClick = remember(call) {
                                {
                                    onReturnToCallClick(call)
                                }
                            }
                        )
                        .fillMaxWidth()
                        .heightIn(min = MaterialTheme.wireDimensions.ongoingCallLabelHeight)
                )

                is ConnectivityUIState.Call.Incoming -> IncomingCallContent(
                    callerName = call.callerName,
                    modifier = Modifier
                        .clickable(
                            onClick = remember(call) {
                                {
                                    onReturnToIncomingCallClick(call)
                                }
                            }
                        )
                        .fillMaxWidth()
                        .heightIn(min = MaterialTheme.wireDimensions.ongoingCallLabelHeight)
                )

                is ConnectivityUIState.Call.Outgoing -> OutgoingCallContent(
                    conversationName = call.conversationName,
                    modifier = Modifier
                        .clickable(
                            onClick = remember(call) {
                                {
                                    onReturnToOutgoingCallClick(call)
                                }
                            }
                        )
                        .fillMaxWidth()
                        .heightIn(min = MaterialTheme.wireDimensions.ongoingCallLabelHeight)
                )
            }
        }
    }
}

@Composable
private fun OngoingCallContent(isMuted: Boolean, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        MicrophoneIcon(isMuted, MaterialTheme.wireColorScheme.onPositive)
        CameraIcon(MaterialTheme.wireColorScheme.onPositive)
        StatusLabel(
            stringResource = R.string.connectivity_status_bar_return_to_call,
            color = MaterialTheme.wireColorScheme.onPositive,
        )
    }
}

@Composable
private fun IncomingCallContent(callerName: String?, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        StatusLabelWithValue(
            stringResource = R.string.connectivity_status_bar_return_to_incoming_call,
            callerName = callerName ?: stringResource(R.string.username_unavailable_label),
            color = MaterialTheme.wireColorScheme.onPositive
        )
    }
}

@Composable
private fun OutgoingCallContent(conversationName: String?, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        StatusLabelWithValue(
            stringResource = R.string.connectivity_status_bar_return_to_outgoing_call,
            callerName = conversationName ?: stringResource(R.string.username_unavailable_label),
            color = MaterialTheme.wireColorScheme.onPositive
        )
    }
}

@Composable
private fun StatusLabel(
    stringResource: Int,
    color: Color = MaterialTheme.wireColorScheme.onPrimary
) {
    Text(
        text = stringResource(id = stringResource).uppercase(),
        color = color,
        style = MaterialTheme.wireTypography.title03,
        modifier = Modifier.padding(vertical = MaterialTheme.wireDimensions.spacing6x)
    )
}

@Composable
private fun StatusLabelWithValue(
    stringResource: Int,
    callerName: String?,
    color: Color = MaterialTheme.wireColorScheme.onPrimary
) {
    val defaultCallerName = stringResource(R.string.username_unavailable_label)
    Text(
        text = stringResource(id = stringResource, callerName ?: defaultCallerName).uppercase(),
        color = color,
        style = MaterialTheme.wireTypography.title03,
        modifier = Modifier.padding(vertical = MaterialTheme.wireDimensions.spacing6x)
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
private fun MicrophoneIcon(
    isMuted: Boolean,
    tint: Color = MaterialTheme.wireColorScheme.onPositive
) {
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
private fun ClearStatusBarColor() {
    val backgroundColor = MaterialTheme.wireColorScheme.background
    val darkIcons = MaterialTheme.wireColorScheme.useDarkSystemBarIcons

    rememberSystemUiController().setSystemBarsColor(
        color = backgroundColor,
        darkIcons = darkIcons
    )
}

@Composable
private fun PreviewCommonTopAppBar(connectivityUIState: ConnectivityUIState) = WireTheme {
    CommonTopAppBar(ThemeOption.SYSTEM, CommonTopAppBarState(connectivityUIState), {}, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityEstablishedCallNotMuted() = WireTheme {
    PreviewCommonTopAppBar(
        ConnectivityUIState.Calls(listOf(ConnectivityUIState.Call.Established(ConversationId("what", "ever"), false)))
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityEstablishedCallAndIncomingCalls() = WireTheme {
    PreviewCommonTopAppBar(
        ConnectivityUIState.Calls(
            listOf(
                ConnectivityUIState.Call.Established(ConversationId("1", "1"), false),
                ConnectivityUIState.Call.Incoming(ConversationId("2", "2"), "John Doe"),
                ConnectivityUIState.Call.Incoming(ConversationId("3", "3"), "Adam Smith"),
            )
        )
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityConnecting() = WireTheme {
    PreviewCommonTopAppBar(ConnectivityUIState.Connecting)
}

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityNone() = WireTheme {
    PreviewCommonTopAppBar(ConnectivityUIState.None)
}
