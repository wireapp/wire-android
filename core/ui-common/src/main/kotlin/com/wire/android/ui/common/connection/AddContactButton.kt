/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.common.connection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.R
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.connectionActionButtonViewModel
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.legalhold.dialog.connectionfailed.LegalHoldSubjectConnectionFailedDialog
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.snackbar.collectAndShowSnackbar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserId

@Composable
fun AddContactButton(
    userId: UserId,
    userName: String,
    modifier: Modifier = Modifier,
    viewModel: ConnectionActionButtonViewModel =
        connectionActionButtonViewModel(ConnectionActionButtonArgs(userId, userName)),
) {
    val state = viewModel.actionableState()
    LocalSnackbarHostState.current.collectAndShowSnackbar(snackbarFlow = viewModel.infoMessage)

    with(state) {
        if (missingLegalHoldConsentDialogState is MissingLegalHoldConsentDialogState.Visible) {
            LegalHoldSubjectConnectionFailedDialog(viewModel::onMissingLegalHoldConsentDismissed)
        }

        WireSecondaryIconButton(
            onButtonClicked = remember(viewModel) { { if (!isPerformingAction) viewModel.onSendConnectionRequest() } },
            iconResource = R.drawable.ic_add_contact,
            contentDescription = R.string.content_description_add_contact,
            loading = isPerformingAction,
            minSize = dimensions().buttonSmallMinSize,
            minClickableSize = dimensions().buttonSmallMinSize,
            modifier = modifier
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAddContactButton() {
    WireTheme {
        AddContactButton(
            userId = UserId("value", "domain"),
            userName = "Username",
            viewModel = object : ConnectionActionButtonViewModel {
                override fun actionableState() = ConnectionActionState(isPerformingAction = false)
            }
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAddContactButtonLoading() {
    WireTheme {
        AddContactButton(
            userId = UserId("value", "domain"),
            userName = "Username",
            viewModel = object : ConnectionActionButtonViewModel {
                override fun actionableState() = ConnectionActionState(isPerformingAction = true)
            }
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAddContactButtonDialog() {
    WireTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AddContactButton(
                userId = UserId("value", "domain"),
                userName = "Username",
                viewModel = object : ConnectionActionButtonViewModel {
                    override fun actionableState() = ConnectionActionState(
                        missingLegalHoldConsentDialogState = MissingLegalHoldConsentDialogState.Visible(UserId("value", "domain"))
                    )
                }
            )
        }
    }
}
