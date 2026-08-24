/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.login

import androidx.compose.runtime.Composable
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme

@MultipleThemePreviews
@Composable
fun PreviewLoginErrorDialog() = WireTheme {
    LoginErrorDialog(
        dialogErrorData = LoginDialogErrorData.Known(
            type = LoginDialogType.InvalidCredentials,
            actionText = "OK",
        ),
        onDialogDismiss = {},
    )
}
