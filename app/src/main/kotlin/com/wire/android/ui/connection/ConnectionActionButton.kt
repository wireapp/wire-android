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

package com.wire.android.ui.connection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.snackbar.collectAndShowSnackbar
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.legalhold.dialog.connectionfailed.LegalHoldSubjectConnectionFailedDialog
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

const val CONNECTION_ACTION_BUTTONS_TEST_TAG = "connection_buttons"

@Composable
fun ConnectionActionButton(
    userId: UserId,
    userName: String,
    fullName: String,
    connectionStatus: ConnectionState,
    isConversationStarted: Boolean,
    modifier: Modifier = Modifier,
    onConnectionRequestIgnored: (String) -> Unit = {},
    onOpenConversation: (ConversationId) -> Unit = {},
    viewModel: ConnectionActionButtonViewModel =
        hiltViewModelScoped<ConnectionActionButtonViewModelImpl, ConnectionActionButtonViewModel, ConnectionActionButtonViewModelImpl.Factory, ConnectionActionButtonArgs>(
            ConnectionActionButtonArgs(userId, userName)
        ),
) {
    LocalSnackbarHostState.current.collectAndShowSnackbar(snackbarFlow = viewModel.infoMessage)
    val unblockUserDialogState = rememberVisibilityState<UnblockUserDialogState>()
    val unableStartConversationDialogState = rememberVisibilityState<UnableStartConversationDialogState>()

    LaunchedEffect(viewModel.actionableState().isPerformingAction) {
        unblockUserDialogState.update { it.copy(loading = viewModel.actionableState().isPerformingAction) }
    }
    UnblockUserDialogContent(
        dialogState = unblockUserDialogState,
        onUnblock = { viewModel.onUnblockUser() },
    )

    UnableStartConversationDialogContent(dialogState = unableStartConversationDialogState)

    if (!viewModel.actionableState().isPerformingAction) {
        unblockUserDialogState.dismiss()
    }

    with(viewModel.actionableState()) {
        if (missingLegalHoldConsentDialogState is MissingLegalHoldConsentDialogState.Visible) {
            LegalHoldSubjectConnectionFailedDialog(viewModel::onMissingLegalHoldConsentDismissed)
        }
    }

    Box(modifier = modifier) {
        when (connectionStatus) {
            ConnectionState.SENT -> WireSecondaryButton(
                text = stringResource(R.string.connection_label_cancel_request),
                loading = viewModel.actionableState().isPerformingAction,
                onClick = viewModel::onCancelConnectionRequest,
                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                modifier = Modifier.testTag(CONNECTION_ACTION_BUTTONS_TEST_TAG),
            )

            ConnectionState.ACCEPTED -> WirePrimaryButton(
                text = stringResource(if (isConversationStarted) R.string.label_open_conversation else R.string.label_start_conversation),
                loading = viewModel.actionableState().isPerformingAction,
                onClick = viewModel::onOpenConversation,
                modifier = Modifier.testTag(CONNECTION_ACTION_BUTTONS_TEST_TAG),
            )

            ConnectionState.IGNORED -> WirePrimaryButton(
                text = stringResource(R.string.connection_label_accept),
                loading = viewModel.actionableState().isPerformingAction,
                onClick = viewModel::onAcceptConnectionRequest,
                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_tick),
                        contentDescription = null,
                        modifier = Modifier.padding(dimensions().spacing8x)
                    )
                },
                modifier = Modifier.testTag(CONNECTION_ACTION_BUTTONS_TEST_TAG),
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
                            contentDescription = null,
                            modifier = Modifier.padding(dimensions().spacing8x)
                        )
                    },
                    modifier = Modifier.testTag(CONNECTION_ACTION_BUTTONS_TEST_TAG),
                )
                Spacer(modifier = Modifier.height(dimensions().spacing8x))
                WirePrimaryButton(
                    text = stringResource(R.string.connection_label_ignore),
                    loading = viewModel.actionableState().isPerformingAction,
                    state = WireButtonState.Error,
                    onClick = viewModel::onIgnoreConnectionRequest,
                    clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.padding(dimensions().spacing8x)
                        )
                    },
                    modifier = Modifier.testTag(CONNECTION_ACTION_BUTTONS_TEST_TAG),
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
                    modifier = Modifier.testTag(CONNECTION_ACTION_BUTTONS_TEST_TAG),
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
                        contentDescription = null,
                        modifier = Modifier.padding(dimensions().spacing8x)
                    )
                },
                modifier = Modifier.testTag(CONNECTION_ACTION_BUTTONS_TEST_TAG),
            )
        }
    }
    HandleActions(viewModel.actions) { action ->
        when (action) {
            is OpenConversation -> onOpenConversation(action.conversationId)
            is ConnectionRequestIgnored -> onConnectionRequestIgnored(action.userName)
            is MissingKeyPackages -> unableStartConversationDialogState.show(UnableStartConversationDialogState(fullName))
        }
    }
}

@Composable
fun UnableStartConversationDialogContent(dialogState: VisibilityState<UnableStartConversationDialogState>) {
    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(id = R.string.missing_keypackage_dialog_title),
            text = LocalContext.current.resources.stringWithStyledArgs(
                R.string.missing_keypackage_dialog_body,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().onBackground,
                colorsScheme().onBackground,
                state.userName
            ),
            onDismiss = dialogState::dismiss,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = R.string.label_ok),
                type = WireDialogButtonType.Primary,
            ),
        )
    }
}

data class UnableStartConversationDialogState(val userName: String)

@Composable
@PreviewMultipleThemes
fun PreviewOtherUserConnectionActionButtonPending() {
    WireTheme {
        ConnectionActionButton(
            userId = UserId("value", "domain"),
            userName = "Username",
            fullName = "some user",
            connectionStatus = ConnectionState.PENDING,
            isConversationStarted = false
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
            fullName = "some user",
            connectionStatus = ConnectionState.NOT_CONNECTED,
            isConversationStarted = false
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
            fullName = "some user",
            connectionStatus = ConnectionState.BLOCKED,
            isConversationStarted = false
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
            fullName = "some user",
            connectionStatus = ConnectionState.CANCELLED,
            isConversationStarted = false
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
            fullName = "some user",
            connectionStatus = ConnectionState.ACCEPTED,
            isConversationStarted = false
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
            fullName = "some user",
            connectionStatus = ConnectionState.SENT,
            isConversationStarted = false
        )
    }
}
