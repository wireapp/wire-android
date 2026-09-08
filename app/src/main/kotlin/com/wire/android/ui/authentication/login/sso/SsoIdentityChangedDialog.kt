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

package com.wire.android.ui.authentication.login.sso

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun SsoIdentityChangedDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.sso_identity_changed_dialog_title),
        text = stringResource(R.string.sso_identity_changed_dialog_message),
        onDismiss = onDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(R.string.sso_identity_changed_dialog_confirm),
            type = WireDialogButtonType.Primary,
            state = WireButtonState.Error,
        ),
        buttonsHorizontalAlignment = false,
    )
}

@PreviewMultipleThemes
@Composable
private fun SsoIdentityChangedDialogPreview() = WireTheme {
    SsoIdentityChangedDialog(
        onDismiss = {},
        onConfirm = {},
    )
}
