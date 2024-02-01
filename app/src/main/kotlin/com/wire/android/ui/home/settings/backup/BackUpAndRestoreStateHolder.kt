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

package com.wire.android.ui.home.settings.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState

class BackUpAndRestoreStateHolder {

    var dialogState: BackupAndRestoreDialog by mutableStateOf(
        BackupAndRestoreDialog.None
    )

    var permissionPermanentlyDeniedDialogState: PermissionPermanentlyDeniedDialogState by mutableStateOf(
        PermissionPermanentlyDeniedDialogState.Hidden
    )

    fun showBackupDialog() {
        dialogState = BackupAndRestoreDialog.CreateBackup
    }

    fun showRestoreDialog() {
        dialogState = BackupAndRestoreDialog.RestoreBackup
    }

    fun dismissDialog() {
        dialogState = BackupAndRestoreDialog.None
    }

    fun showPermissionPermanentlyDeniedDialog(title: Int, description: Int) {
        permissionPermanentlyDeniedDialogState = PermissionPermanentlyDeniedDialogState.Visible(
            title = title,
            description = description
        )
    }

    fun hidePermissionPermanentlyDeniedDialog() {
        permissionPermanentlyDeniedDialogState = PermissionPermanentlyDeniedDialogState.Hidden
    }

}

@Composable
fun rememberBackUpAndRestoreStateHolder(): BackUpAndRestoreStateHolder {
    return remember {
        BackUpAndRestoreStateHolder()
    }
}

sealed class BackupAndRestoreDialog {
    object None : BackupAndRestoreDialog()
    object CreateBackup : BackupAndRestoreDialog()
    object RestoreBackup : BackupAndRestoreDialog()
}

