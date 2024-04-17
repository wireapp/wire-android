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
package com.wire.android.feature.sketch

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.wire.android.ui.theme.wireColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscardDialogConfirmation(
    scope: CoroutineScope,
    sheetState: SheetState,
    onDismissSketch: () -> Unit,
    onHideConfirmationDialog: () -> Unit,
) {
    AlertDialog(
        backgroundColor = MaterialTheme.wireColorScheme.background,
        contentColor = MaterialTheme.wireColorScheme.onBackground,
        onDismissRequest = onHideConfirmationDialog,
        title = { Text(stringResource(R.string.confirm_changes_title)) },
        text = { Text(stringResource(R.string.confirm_changes_text)) },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onHideConfirmationDialog()
                    onDismissSketch()
                }
            }) { Text(stringResource(R.string.confirm_changes_dismiss)) }
        },
        dismissButton = {
            TextButton(onClick = {
                onHideConfirmationDialog()
            }) { Text(stringResource(R.string.confirm_changes_confirm)) }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}
