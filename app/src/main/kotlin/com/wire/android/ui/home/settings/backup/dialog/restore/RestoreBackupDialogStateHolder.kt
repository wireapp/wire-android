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

package com.wire.android.ui.home.settings.backup.dialog.restore

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.R
import kotlinx.parcelize.Parcelize

@Stable
class RestoreDialogStateHolder {
    companion object {
        val INITIAL_STEP = RestoreDialogStep.ChooseBackupFile

        fun saver(): Saver<RestoreDialogStateHolder, *> {
            return Saver(
                save = { it.currentRestoreDialogStep },
                restore = { RestoreDialogStateHolder().apply { currentRestoreDialogStep = it } }
            )
        }
    }

    var currentRestoreDialogStep: RestoreDialogStep by mutableStateOf(INITIAL_STEP)

    var restoreProgress: Float by mutableStateOf(0.0f)

    var isRestoreCompleted: Boolean by mutableStateOf(false)

    fun toEnterPassword() {
        currentRestoreDialogStep = RestoreDialogStep.EnterPassword
    }

    fun toRestoreFailure(restoreFailure: RestoreFailure) {
        currentRestoreDialogStep = RestoreDialogStep.Failure(restoreFailure)
    }

    fun toRestoreBackup() {
        currentRestoreDialogStep = RestoreDialogStep.RestoreBackup
    }

    fun toFinished() {
        restoreProgress = 1.0f
        isRestoreCompleted = true
    }
}

@Composable
fun rememberRestoreDialogState(): RestoreDialogStateHolder {
    return rememberSaveable(saver = RestoreDialogStateHolder.saver()) { RestoreDialogStateHolder() }
}

sealed interface RestoreDialogStep : Parcelable {
    @Parcelize data object ChooseBackupFile : RestoreDialogStep

    @Parcelize data object EnterPassword : RestoreDialogStep

    @Parcelize data object RestoreBackup : RestoreDialogStep

    @Parcelize data class Failure(val restoreFailure: RestoreFailure) : RestoreDialogStep
}

enum class RestoreFailure(@StringRes val title: Int, @StringRes val message: Int) {
    IncompatibleBackup(
        R.string.backup_dialog_restore_incompatible_version_error_title,
        R.string.backup_dialog_restore_incompatible_version_error_message
    ),
    WrongBackup(
        R.string.backup_dialog_restore_wrong_user_error_title,
        R.string.backup_dialog_restore_wrong_user_error_message
    ),
    GeneralFailure(
        R.string.backup_dialog_restore_general_error_title,
        R.string.backup_dialog_restore_general_error_message
    )
}
