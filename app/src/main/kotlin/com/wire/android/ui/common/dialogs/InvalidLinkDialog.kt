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

package com.wire.android.ui.common.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.home.conversations.InvalidLinkDialogState

@Composable
fun InvalidLinkDialog(dialogState: InvalidLinkDialogState, hideDialog: () -> Unit) {
    if (dialogState is InvalidLinkDialogState.Visible) {
        WireDialog(
            title = stringResource(R.string.label_invalid_link_title),
            text = stringResource(R.string.invalid_link_dialog_body),
            buttonsHorizontalAlignment = false,
            onDismiss = hideDialog,
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.label_ok),
                type = WireDialogButtonType.Primary,
                onClick = hideDialog
            )
        )
    }
}
