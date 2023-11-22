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
 */

package com.wire.android.ui.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.snackbar.collectAndShowSnackbar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConnectionActionButton(
    userId: UserId,
    userName: String,
    connectionStatus: ConnectionState,
    onConnectionRequestIgnored: (String) -> Unit = {},
    onOpenConversation: (ConversationId) -> Unit = {},
    viewModel: ConnectionActionButtonViewModel =
        hiltViewModelScoped<ConnectionActionButtonViewModelImpl, ConnectionActionButtonViewModel, ConnectionActionButtonArgs>(
            ConnectionActionButtonArgs(userId, userName)
        ),
) {
    LocalSnackbarHostState.current.collectAndShowSnackbar(snackbarFlow = viewModel.infoMessage)
    val unblockUserDialogState = rememberVisibilityState<UnblockUserDialogState>()

    UnblockUserDialogContent(
        dialogState = unblockUserDialogState,
        onUnblock = { viewModel.onUnblockUser() },
        isLoading = viewModel.actionableState().isPerformingAction,
    )

    if (!viewModel.actionableState().isPerformingAction) {
        unblockUserDialogState.dismiss()
    }

    when (connectionStatus) {
        ConnectionState.SENT -> WireSecondaryButton(
            text = stringResource(R.string.connection_label_cancel_request),
            loading = viewModel.actionableState().isPerformingAction,
            onClick = viewModel::onCancelConnectionRequest,
            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        )

        ConnectionState.ACCEPTED -> WirePrimaryButton(
            text = stringResource(R.string.label_open_conversation),
            loading = viewModel.actionableState().isPerformingAction,
            onClick = { viewModel.onOpenConversation(onOpenConversation) },
        )

        ConnectionState.IGNORED -> WirePrimaryButton(
            text = stringResource(R.string.connection_label_accept),
            loading = viewModel.actionableState().isPerformingAction,
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
                loading = viewModel.actionableState().isPerformingAction,
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
                loading = viewModel.actionableState().isPerformingAction,
                state = WireButtonState.Error,
                onClick = {
                    viewModel.onIgnoreConnectionRequest {
                        onConnectionRequestIgnored(it)
                    }
                },
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
                loading = viewModel.actionableState().isPerformingAction,
                onClick = {
                    unblockUserDialogState.show(
                        UnblockUserDialogState(
                            userId = userId,
                            userName = userName
                        )
                    )
                },
                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
            )
        }

        ConnectionState.NOT_CONNECTED,
        ConnectionState.CANCELLED,
        ConnectionState.MISSING_LEGALHOLD_CONSENT -> WirePrimaryButton(
            text = stringResource(R.string.connection_label_connect),
            loading = viewModel.actionableState().isPerformingAction,
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
@PreviewMultipleThemes
fun PreviewOtherUserConnectionActionButtonPending() {
    WireTheme {
        ConnectionActionButton(
            userId = UserId("value", "domain"),
            userName = "Username",
            connectionStatus = ConnectionState.PENDING,
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherUserConnectionActionButtonNotConnected() {
    WireTheme {
        ConnectionActionButton(
            userId = UserId("value", "domain"),
            userName = "Username",
            connectionStatus = ConnectionState.NOT_CONNECTED,
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherUserConnectionActionButtonBlocked() {
    WireTheme {
        ConnectionActionButton(
            userId = UserId("value", "domain"),
            userName = "Username",
            connectionStatus = ConnectionState.BLOCKED,
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherUserConnectionActionButtonCanceled() {
    WireTheme {
        ConnectionActionButton(
            userId = UserId("value", "domain"),
            userName = "Username",
            connectionStatus = ConnectionState.CANCELLED,
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherUserConnectionActionButtonAccepted() {
    WireTheme {
        ConnectionActionButton(
            userId = UserId("value", "domain"),
            userName = "Username",
            connectionStatus = ConnectionState.ACCEPTED,
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherUserConnectionActionButtonSent() {
    WireTheme {
        ConnectionActionButton(
            userId = UserId("value", "domain"),
            userName = "Username",
            connectionStatus = ConnectionState.SENT,
        )
    }
}
