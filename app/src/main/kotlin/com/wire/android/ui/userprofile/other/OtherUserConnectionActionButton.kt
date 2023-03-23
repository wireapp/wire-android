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

package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
fun OtherUserConnectionActionButton(
    connectionStatus: ConnectionState,
    onSendConnectionRequest: () -> Unit,
    onOpenConversation: () -> Unit,
    onCancelConnectionRequest: () -> Unit,
    acceptConnectionRequest: () -> Unit,
    ignoreConnectionRequest: () -> Unit,
    onUnblockUser: () -> Unit,
) {
    when (connectionStatus) {
        ConnectionState.SENT -> WireSecondaryButton(
            text = stringResource(R.string.connection_label_cancel_request),
            onClick = onCancelConnectionRequest,
            clickBlockParams = ClickBlockParams(blockUntilSynced = true, blockUntilConnected = true),
        )
        ConnectionState.ACCEPTED -> WirePrimaryButton(
            text = stringResource(R.string.label_open_conversation),
            onClick = onOpenConversation,
        )
        ConnectionState.IGNORED -> WirePrimaryButton(
            text = stringResource(R.string.connection_label_accept),
            onClick = acceptConnectionRequest,
            clickBlockParams = ClickBlockParams(blockUntilSynced = true, blockUntilConnected = true),
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
                onClick = acceptConnectionRequest,
                clickBlockParams = ClickBlockParams(blockUntilSynced = true, blockUntilConnected = true),
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
                onClick = ignoreConnectionRequest,
                clickBlockParams = ClickBlockParams(blockUntilSynced = true, blockUntilConnected = true),
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
                onClick = onUnblockUser,
                clickBlockParams = ClickBlockParams(blockUntilSynced = true, blockUntilConnected = true),
            )
        }
        else -> WirePrimaryButton(
            text = stringResource(R.string.connection_label_connect),
            onClick = onSendConnectionRequest,
            clickBlockParams = ClickBlockParams(blockUntilSynced = true, blockUntilConnected = true),
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
fun PreviewOtherUserConnectionActionButton() {
    OtherUserConnectionActionButton(
        connectionStatus = ConnectionState.ACCEPTED,
        onSendConnectionRequest = {},
        onOpenConversation = {},
        onCancelConnectionRequest = {},
        acceptConnectionRequest = {},
        ignoreConnectionRequest = {},
        onUnblockUser = {})
}
