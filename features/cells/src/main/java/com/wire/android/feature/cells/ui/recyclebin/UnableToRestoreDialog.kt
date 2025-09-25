/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.recyclebin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme

@Composable
fun UnableToRestoreDialog(
    isFolder: Boolean,
    onDismiss: () -> Unit,
) {
    WireDialog(
        title = stringResource(
            id = if (isFolder) {
                R.string.dialog_unable_to_restore_folder_title
            } else {
                R.string.dialog_unable_to_restore_file_title
            }
        ),
        text = if (isFolder) {
            stringResource(id = R.string.dialog_unable_to_restore_folder_description)
        } else {
            stringResource(id = R.string.dialog_unable_to_restore_file_description)
        },
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.ok_label),
            type = WireDialogButtonType.Primary,
        ),
        buttonsHorizontalAlignment = false,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

@MultipleThemePreviews
@Composable
fun PreviewUnableToRestoreDialog() {
    WireTheme {
        UnableToRestoreDialog(
            isFolder = true,
            onDismiss = {}
        )
    }
}
