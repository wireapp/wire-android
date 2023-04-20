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

package com.wire.android.ui.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.bundleOf
import com.sebaslogen.resaca.hilt.hiltViewModelScoped
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.navigation.EXTRA_CONNECTION_STATE
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.EXTRA_USER_NAME
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConnectionActionButton(
    userId: UserId,
    userName: String,
    connectionStatus: ConnectionState
) {
    val viewModel: ConnectionActionButtonBaseViewModel = if (LocalInspectionMode.current) {
        ConnectionActionButtonPreviewModel(connectionStatus)
    } else {
        hiltViewModelScoped<ConnectionActionButtonViewModel>(
            key = "${ConnectionActionButtonViewModel.MY_ARGS_KEY}$userId",
            defaultArguments = bundleOf(
                EXTRA_USER_ID to userId.toString(),
                EXTRA_USER_NAME to userName,
                EXTRA_CONNECTION_STATE to connectionStatus.toString()
            )
        )
    }

    when (viewModel.state()) {
        ConnectionState.SENT -> WireSecondaryButton(
            text = stringResource(R.string.connection_label_cancel_request),
            onClick = viewModel::onCancelConnectionRequest,
            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        )
        ConnectionState.ACCEPTED -> WirePrimaryButton(
            text = stringResource(R.string.label_open_conversation),
            onClick = viewModel::onOpenConversation,
        )
        ConnectionState.IGNORED -> WirePrimaryButton(
            text = stringResource(R.string.connection_label_accept),
            onClick = viewModel::onAcceptConnectionRequest,
            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_tick),
                    contentDescription = stringResource(R.string.content_description_right_arrow),
                    modifier = Modifier.padding(dimensions().spacing8x)
                )
            }
        )
        ConnectionState.PENDING -> Column {
            WirePrimaryButton(
                text = stringResource(R.string.connection_label_accept),
                onClick = viewModel::onAcceptConnectionRequest,
                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_tick),
                        contentDescription = stringResource(R.string.content_description_right_arrow),
                        modifier = Modifier.padding(dimensions().spacing8x)
                    )
                }
            )
            Spacer(modifier = Modifier.height(dimensions().spacing8x))
            WirePrimaryButton(
                text = stringResource(R.string.connection_label_ignore),
                state = WireButtonState.Error,
                onClick = viewModel::onIgnoreConnectionRequest,
                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.content_description_right_arrow),
                        modifier = Modifier.padding(dimensions().spacing8x)
                    )
                }
            )
        }
        ConnectionState.BLOCKED -> {
            WireSecondaryButton(
                text = stringResource(R.string.user_profile_unblock_user),
                onClick = viewModel::onUnblockUser,
                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
            )
        }
        ConnectionState.NOT_CONNECTED,
        ConnectionState.CANCELLED,
        ConnectionState.MISSING_LEGALHOLD_CONSENT -> WirePrimaryButton(
            text = stringResource(R.string.connection_label_connect),
            onClick = viewModel::onSendConnectionRequest,
            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_contact),
                    contentDescription = stringResource(R.string.content_description_right_arrow),
                    modifier = Modifier.padding(dimensions().spacing8x)
                )
            }
        )
    }
}

@Composable
@Preview
fun PreviewOtherUserConnectionActionButtonPending() {
    ConnectionActionButton(
        userId = UserId("value", "domain"),
        userName = "Username",
        connectionStatus = ConnectionState.PENDING
    )
}

@Composable
@Preview
fun PreviewOtherUserConnectionActionButtonNotConnected() {
    ConnectionActionButton(
        userId = UserId("value", "domain"),
        userName = "Username",
        connectionStatus = ConnectionState.NOT_CONNECTED
    )
}

@Composable
@Preview
fun PreviewOtherUserConnectionActionButtonBlocked() {
    ConnectionActionButton(
        userId = UserId("value", "domain"),
        userName = "Username",
        connectionStatus = ConnectionState.BLOCKED
    )
}

@Composable
@Preview
fun PreviewOtherUserConnectionActionButtonCanceled() {
    ConnectionActionButton(
        userId = UserId("value", "domain"),
        userName = "Username",
        connectionStatus = ConnectionState.CANCELLED
    )
}

@Composable
@Preview
fun PreviewOtherUserConnectionActionButtonAccepted() {
    ConnectionActionButton(
        userId = UserId("value", "domain"),
        userName = "Username",
        connectionStatus = ConnectionState.ACCEPTED
    )
}

@Composable
@Preview
fun PreviewOtherUserConnectionActionButtonSent() {
    ConnectionActionButton(
        userId = UserId("value", "domain"),
        userName = "Username",
        connectionStatus = ConnectionState.SENT
    )
}
