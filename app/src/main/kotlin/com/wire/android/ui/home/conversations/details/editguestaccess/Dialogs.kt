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

package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.home.conversations.details.options.DisableConfirmationDialog

@Composable
fun RevokeGuestConfirmationDialog(onConfirm: () -> Unit, onDialogDismiss: () -> Unit) {
    DisableConfirmationDialog(
        text = R.string.revoke_guest__room_link_dialog_text,
        title = R.string.revoke_guest__room_link_dialog_title,
        onConfirm = onConfirm,
        onDismiss = onDialogDismiss
    )
}

@Composable
fun DisableGuestConfirmationDialog(onConfirm: () -> Unit, onDialogDismiss: () -> Unit) {
    DisableConfirmationDialog(
        text = R.string.disable_guest_dialog_text,
        title = R.string.disable_guest_dialog_title,
        onConfirm = onConfirm,
        onDismiss = onDialogDismiss
    )
}

@Composable
fun GenerateGuestRoomLinkFailureDialog(onDismiss: () -> Unit) {
    WireDialog(
        title = stringResource(id = R.string.label_general_error),
        text = stringResource(id = R.string.guest_link_generate_error_message),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
fun RevokeGuestRoomLinkFailureDialog(onDismiss: () -> Unit) {
    WireDialog(
        title = stringResource(id = R.string.label_general_error),
        text = stringResource(id = R.string.guest_link_revoke_error_message),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDisableGuestConformationDialog() {
    DisableGuestConfirmationDialog({}, {})
}

@Preview()
@Composable
fun PreviewGenerateGuestRoomLinkFailureDialog() {
    GenerateGuestRoomLinkFailureDialog({})
}

@Preview()
@Composable
fun PreviewRevokeGuestRoomLinkFailureDialog() {
    RevokeGuestRoomLinkFailureDialog({})
}
