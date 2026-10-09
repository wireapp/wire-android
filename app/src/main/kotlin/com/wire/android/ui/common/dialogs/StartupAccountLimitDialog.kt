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

package com.wire.android.ui.common.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialog
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialogState
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun StartupAccountLimitDialog(
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
    excessAccounts: Int,
    maxAccounts: Int = BuildConfig.MAX_ACCOUNTS,
) {
    val message = when (maxAccounts) {
        1 -> pluralStringResource(R.plurals.startup_account_limit_message_one_allowed, excessAccounts)
        2 -> pluralStringResource(R.plurals.startup_account_limit_message_two_allowed, excessAccounts)
        else -> pluralStringResource(R.plurals.startup_account_limit_message_other_allowed, excessAccounts, maxAccounts)
    }
    WireDialog(
        title = pluralStringResource(R.plurals.startup_account_limit_title, excessAccounts, excessAccounts),
        text = message,
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = false,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(R.string.startup_account_limit_logout),
            onClick = onLogout,
            type = WireDialogButtonType.Primary,
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(R.string.startup_account_limit_later),
            onClick = onDismiss,
            type = WireDialogButtonType.Tertiary,
        ),
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewStartupAccountLimitDialog() {
    WireTheme {
        StartupAccountLimitDialog(onLogout = {}, onDismiss = {}, excessAccounts = 1)
    }
}

@Composable
internal fun StartupAccountLimitDialogFlow(
    excessAccounts: Int?,
    onDismiss: () -> Unit,
    logout: (Boolean) -> Unit,
    maxAccounts: Int = BuildConfig.MAX_ACCOUNTS,
) {
    val logoutOptionsState = rememberVisibilityState<LogoutOptionsDialogState>()
    if (excessAccounts != null) {
        StartupAccountLimitDialog(
            onLogout = {
                onDismiss()
                logoutOptionsState.show(LogoutOptionsDialogState(shouldWipeData = false))
            },
            onDismiss = onDismiss,
            excessAccounts = excessAccounts,
            maxAccounts = maxAccounts,
        )
    }
    LogoutOptionsDialog(dialogState = logoutOptionsState, logout = logout)
}
