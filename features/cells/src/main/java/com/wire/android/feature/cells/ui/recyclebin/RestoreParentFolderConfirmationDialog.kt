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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme

@Composable
fun RestoreParentFolderConfirmationDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val description = stringResource(
        id = R.string.dialog_restore_parent_folder_description,
        itemName
    )
    WireDialog(
        title = stringResource(id = R.string.dialog_restore_folder_title),
        text = buildAnnotatedString {
            // We look for the position of %1$s and make that part bold
            val startIndex = description.indexOf(itemName)
            val endIndex = startIndex + itemName.length

            // Append the part before the folder name
            append(description.substring(0, startIndex))

            // Make the folder name bold
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(itemName)
            pop()

            // Append the part after the folder name (if any)
            append(description.substring(endIndex))
        },
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(id = R.string.dialog_restore_parent_folder_button),
            type = WireDialogButtonType.Primary,
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(id = R.string.cancel),
            onClick = onDismiss
        ),
        buttonsHorizontalAlignment = false,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

@MultipleThemePreviews
@Composable
fun PreviewRestoreParentFolderConfirmationDialog() {
    WireTheme {
        RestoreParentFolderConfirmationDialog(
            itemName = "Test",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
