/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialog
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialogState

@Composable
fun StartupAccountLimitDialog(onLogout: () -> Unit, onDismiss: () -> Unit, maxAccounts: Int = BuildConfig.MAX_ACCOUNTS) {
    WireDialog(
        title = stringResource(R.string.startup_account_limit_title),
        text = pluralStringResource(R.plurals.startup_account_limit_message, maxAccounts, maxAccounts) +
            "\n\n" + stringResource(R.string.startup_account_limit_backup_reminder) +
            "\n\n" + stringResource(R.string.startup_account_limit_guidance),
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
        StartupAccountLimitDialog(onLogout = {}, onDismiss = {})
    }
}

@Composable
internal fun StartupAccountLimitDialogFlow(visible: Boolean, onDismiss: () -> Unit, logout: (Boolean) -> Unit) {
    val logoutOptionsState = rememberVisibilityState<LogoutOptionsDialogState>()
    if (visible) {
        StartupAccountLimitDialog(
            onLogout = {
                onDismiss()
                logoutOptionsState.show(LogoutOptionsDialogState(shouldWipeData = false))
            },
            onDismiss = onDismiss,
        )
    }
    LogoutOptionsDialog(dialogState = logoutOptionsState, logout = logout)
}
