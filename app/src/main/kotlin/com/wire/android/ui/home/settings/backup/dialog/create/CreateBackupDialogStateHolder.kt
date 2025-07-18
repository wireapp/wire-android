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

package com.wire.android.ui.home.settings.backup.dialog.create

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.parcelize.Parcelize

@Stable
class CreateBackupDialogStateHolder {
    companion object {
        val INITIAL_STEP = BackUpDialogStep.SetPassword

        fun saver(): Saver<CreateBackupDialogStateHolder, *> {
            return Saver(
                save = { it.currentBackupDialogStep },
                restore = { CreateBackupDialogStateHolder().apply { currentBackupDialogStep = it } }
            )
        }
    }

    var currentBackupDialogStep: BackUpDialogStep by mutableStateOf(INITIAL_STEP)

    val isBackupFinished: Boolean
        get() = currentBackupDialogStep is BackUpDialogStep.Finished

    val backupProgress: Float
        get() = when (val step = currentBackupDialogStep) {
            BackUpDialogStep.SetPassword -> 0f
            is BackUpDialogStep.CreatingBackup -> step.progress
            BackUpDialogStep.Failure -> 1f
            is BackUpDialogStep.Finished -> 1f
        }

    val backupFileName: String
        get() = (currentBackupDialogStep as? BackUpDialogStep.Finished)?.fileName ?: ""

    fun toCreatingBackup(progress: Float = 0f) {
        currentBackupDialogStep = BackUpDialogStep.CreatingBackup(progress)
    }

    fun toBackupFailure() {
        currentBackupDialogStep = BackUpDialogStep.Failure
    }

    fun toFinished(fileName: String) {
        currentBackupDialogStep = BackUpDialogStep.Finished(fileName)
    }
}

@Composable
fun rememberBackUpDialogState(): CreateBackupDialogStateHolder {
    return rememberSaveable(saver = CreateBackupDialogStateHolder.saver()) { CreateBackupDialogStateHolder() }
}

sealed interface BackUpDialogStep : Parcelable {
    @Parcelize data object SetPassword : BackUpDialogStep

    @Parcelize data class CreatingBackup(val progress: Float) : BackUpDialogStep

    @Parcelize data class Finished(val fileName: String) : BackUpDialogStep

    @Parcelize data object Failure : BackUpDialogStep
}
