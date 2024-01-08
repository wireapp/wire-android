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
package com.wire.android.ui.home.settings.account.deleteAccount

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.toTitleCase

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    WireDialog(
        title = stringResource(id = R.string.delete_acount_dialog_title).toTitleCase(),
        text = stringResource(id = R.string.delete_acount_dialog_text),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(id = R.string.label_continue).toTitleCase(),
            onClick = onConfirm,
            type = WireDialogButtonType.Primary
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(id = R.string.cancel_login_button_label).toTitleCase(),
            onClick = onDismiss,
            type = WireDialogButtonType.Secondary
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun DeleteAccountDialogPreview() {
    DeleteAccountDialog()
}
