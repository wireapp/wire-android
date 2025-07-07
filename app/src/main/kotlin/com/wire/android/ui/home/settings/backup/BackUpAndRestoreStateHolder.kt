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

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.parcelize.Parcelize

class BackUpAndRestoreStateHolder {

    companion object {
        fun saver(): Saver<BackUpAndRestoreStateHolder, *> {
            return Saver(
                save = { it.dialogState },
                restore = { BackUpAndRestoreStateHolder().apply { dialogState = it } }
            )
        }
    }

    var dialogState: BackupAndRestoreDialog by mutableStateOf(
        BackupAndRestoreDialog.None
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
}

@Composable
fun rememberBackUpAndRestoreStateHolder(): BackUpAndRestoreStateHolder {
    return rememberSaveable(saver = BackUpAndRestoreStateHolder.saver()) {
        BackUpAndRestoreStateHolder()
    }
}

@Parcelize
sealed class BackupAndRestoreDialog : Parcelable {
    object None : BackupAndRestoreDialog()
    object CreateBackup : BackupAndRestoreDialog()
    object RestoreBackup : BackupAndRestoreDialog()
}
