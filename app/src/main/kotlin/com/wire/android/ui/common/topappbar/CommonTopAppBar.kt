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

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.style.BackgroundType
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.theme.WireColorScheme
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.UpdateSystemBarIconsAppearance
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.network.NetworkState

@Composable
fun CommonTopAppBar(
    commonTopAppBarState: CommonTopAppBarState,
    backgroundType: BackgroundType,
    onReturnToCallClick: (ConnectivityUIState.Call.Established) -> Unit,
    onReturnToIncomingCallClick: (ConnectivityUIState.Call.Incoming) -> Unit,
    onReturnToOutgoingCallClick: (ConnectivityUIState.Call.Outgoing) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(
        targetState = MaterialTheme.wireColorScheme to getColorType(commonTopAppBarState.connectivityState, backgroundType),
        label = "connectivity state transition"
    )
    val backgroundColor = transition.animateColor(label = "top app bar background color") { (colorScheme, colorType) ->
        colorScheme.getBackgroundColor(colorType)
    }
    val systemBarUseDarkIcons = transition.animateFloat(label = "system bar icons appearance") { (colorScheme, colorType) ->
        if (colorScheme.getStatusBarUseDarkIcons(colorType)) 1f else 0f
    }

    UpdateSystemBarIconsAppearance(systemBarUseDarkIcons.value > 0.5f)

    Column(
        modifier = modifier
            .drawBehind { drawRect(backgroundColor.value) }
            .statusBarsPadding()
    ) {
        ConnectivityStatusBar(
            networkState = commonTopAppBarState.networkState,
            connectivityInfo = commonTopAppBarState.connectivityState,
            onReturnToCallClick = onReturnToCallClick,
            onReturnToIncomingCallClick = onReturnToIncomingCallClick,
            onReturnToOutgoingCallClick = onReturnToOutgoingCallClick,
            modifier = modifier,
        )
    }
}

private enum class ConnectivityStatusColorType { Calls, Connection, Auth, Default }

private fun getColorType(connectivityState: ConnectivityUIState, backgroundType: BackgroundType) = when (connectivityState) {
    is ConnectivityUIState.Calls -> ConnectivityStatusColorType.Calls
    is ConnectivityUIState.Connecting,
    is ConnectivityUIState.WaitingConnection -> ConnectivityStatusColorType.Connection
    is ConnectivityUIState.None -> when (backgroundType) {
        BackgroundType.Auth -> ConnectivityStatusColorType.Auth
        BackgroundType.Default -> ConnectivityStatusColorType.Default
    }
}

@Composable
private fun WireColorScheme.getBackgroundColor(statusColorType: ConnectivityStatusColorType): Color = when (statusColorType) {
    ConnectivityStatusColorType.Calls -> positive
    ConnectivityStatusColorType.Connection -> primary
    ConnectivityStatusColorType.Auth,
    ConnectivityStatusColorType.Default -> Color.Transparent
}

private fun WireColorScheme.getStatusBarUseDarkIcons(statusColorType: ConnectivityStatusColorType): Boolean = when (statusColorType) {
    ConnectivityStatusColorType.Calls,
    ConnectivityStatusColorType.Connection -> connectivityBarShouldUseDarkIcons
    ConnectivityStatusColorType.Auth -> false // splash is always dark so use light icons
    ConnectivityStatusColorType.Default -> useDarkSystemBarIcons
}

@Composable
private fun ConnectivityStatusBar(
    connectivityInfo: ConnectivityUIState,
    networkState: NetworkState,
    onReturnToCallClick: (ConnectivityUIState.Call.Established) -> Unit,
    onReturnToIncomingCallClick: (ConnectivityUIState.Call.Incoming) -> Unit,
    onReturnToOutgoingCallClick: (ConnectivityUIState.Call.Outgoing) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .animateContentSize()
            .fillMaxWidth(),
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

            is ConnectivityUIState.WaitingConnection -> {
                val color = MaterialTheme.wireColorScheme.onPrimary
                val waitingStatus: @Composable () -> Unit = {
                    StatusLabel(
                        stringResource = R.string.connectivity_status_bar_waiting_for_network,
                        color
                    )
                }

                if (!BuildConfig.PRIVATE_BUILD) {
                    waitingStatus()
                    return@Column
                }

                WaitingStatusLabelInternal(connectivityInfo, networkState, waitingStatus)
            }

            ConnectivityUIState.None -> {}
        }
    }
}

@Composable
private fun WaitingStatusLabelInternal(
    connectivityInfo: ConnectivityUIState.WaitingConnection,
    networkState: NetworkState,
    waitingStatus: @Composable () -> Unit,
) {
    assert(BuildConfig.PRIVATE_BUILD) { "This composable should only be used in the internal versions" }

    val cause = connectivityInfo.cause?.javaClass?.simpleName ?: "null"
    val delay = connectivityInfo.retryDelay ?: "null"
    var fontSize by remember { mutableStateOf(1f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        waitingStatus()
        Text(
            text = "Cause: $cause Delay: $delay, Net: $networkState",
            style = MaterialTheme.wireTypography.title03.copy(
                fontSize = MaterialTheme.wireTypography.title03.fontSize * fontSize,
                color = MaterialTheme.wireColorScheme.onPrimary,
            ),
            onTextLayout = {
                // This is used to make sure the text fits in the available space
                // so no needed information is cut off. It introduces a small delay in the text
                // rendering but it is not important as this code is only used in the debug version
                if (it.hasVisualOverflow) {
                    fontSize *= 0.9f
                }
            },
        )
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
    string: String,
    color: Color = MaterialTheme.wireColorScheme.onPrimary
) {
    Text(
        text = string.uppercase(),
        color = color,
        style = MaterialTheme.wireTypography.title03,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = MaterialTheme.wireDimensions.spacing6x)
    )
}

@Composable
private fun StatusLabel(
    stringResource: Int,
    color: Color = MaterialTheme.wireColorScheme.onPrimary
) {
    StatusLabel(
        string = stringResource(id = stringResource),
        color = color,
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
            id = if (isMuted) {
                R.drawable.ic_microphone_white_muted
            } else {
                R.drawable.ic_microphone_white
            }
        ),
        contentDescription = stringResource(
            id = if (isMuted) {
                R.string.content_description_calling_call_muted
            } else {
                R.string.content_description_calling_call_unmuted
            }
        ),
        tint = tint
    )
}

@Composable
private fun PreviewCommonTopAppBar(
    connectivityUIState: ConnectivityUIState,
    backgroundType: BackgroundType = BackgroundType.Default,
    content: @Composable () -> Unit = {},
) = WireTheme {
    EdgeToEdgePreview(
        useDarkIcons = MaterialTheme.wireColorScheme.getStatusBarUseDarkIcons(getColorType(connectivityUIState, backgroundType))
    ) {
        CommonTopAppBar(CommonTopAppBarState(connectivityUIState), backgroundType, {}, {}, {})
        content()
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityEstablishedCallNotMuted() =
    PreviewCommonTopAppBar(
        ConnectivityUIState.Calls(listOf(ConnectivityUIState.Call.Established(UserId("v", "d"), ConversationId("what", "ever"), false)))
    )

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityEstablishedCallAndIncomingCalls() =
    PreviewCommonTopAppBar(
        ConnectivityUIState.Calls(
            listOf(
                ConnectivityUIState.Call.Established(UserId("v", "d"), ConversationId("1", "1"), false),
                ConnectivityUIState.Call.Incoming(UserId("v", "d"), ConversationId("2", "2"), "John Doe"),
                ConnectivityUIState.Call.Incoming(UserId("v", "d"), ConversationId("3", "3"), "Adam Smith"),
            )
        )
    )

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityIncomingCall() =
    PreviewCommonTopAppBar(
        ConnectivityUIState.Calls(
            listOf(ConnectivityUIState.Call.Incoming(UserId("v", "d"), ConversationId("2", "2"), "John Doe"))
        )
    )

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityOutgoingCall() =
    PreviewCommonTopAppBar(
        ConnectivityUIState.Calls(
            listOf(ConnectivityUIState.Call.Outgoing(UserId("v", "d"), ConversationId("2", "2"), "John Doe"))
        )
    )

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityConnecting() =
    PreviewCommonTopAppBar(ConnectivityUIState.Connecting)

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityWaitingConnection() =
    PreviewCommonTopAppBar(ConnectivityUIState.WaitingConnection(null, null))

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityNone() =
    PreviewCommonTopAppBar(ConnectivityUIState.None)

@PreviewMultipleThemes
@Composable
fun PreviewCommonTopAppBar_ConnectivityNone_Splash() =
    WireAuthBackgroundLayout {
        PreviewCommonTopAppBar(ConnectivityUIState.None, BackgroundType.Auth)
    }
