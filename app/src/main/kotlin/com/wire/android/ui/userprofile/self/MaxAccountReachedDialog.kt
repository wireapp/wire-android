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

package com.wire.android.ui.userprofile.self

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
fun MaxAccountReachedDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, @StringRes buttonText: Int) {
    WireDialog(
        title = stringResource(id = R.string.max_account_reached_dialog_title),
        text = stringResource(id = R.string.max_account_reached_dialog_message),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(buttonText),
            onClick = onConfirm,
            type = WireDialogButtonType.Primary
        )
    )
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
fun PreviewMaxAccountReachedDialogWithOkButton() {
    MaxAccountReachedDialog(onConfirm = { }, onDismiss = { }, buttonText = R.string.label_ok)
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
fun PreviewMaxAccountReachedDialogWithOpenProfileButton() {
    MaxAccountReachedDialog(onConfirm = { }, onDismiss = { }, buttonText = R.string.max_account_reached_dialog_button_open_profile
    )
}
