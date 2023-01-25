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

package com.wire.android.ui.home.settings.backup.dialog.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

@Stable
class CreateBackupDialogStateHolder {
    companion object {
        val INITIAL_STEP = BackUpDialogStep.SetPassword
    }

    var currentBackupDialogStep: BackUpDialogStep by mutableStateOf(INITIAL_STEP)

    var isBackupFinished: Boolean by mutableStateOf(false)

    var backupProgress: Float by mutableStateOf(0.0f)

    fun toCreateBackup() {
        currentBackupDialogStep = BackUpDialogStep.CreatingBackup
    }

    fun toBackupFailure() {
        currentBackupDialogStep = BackUpDialogStep.Failure
    }

    fun toFinished() {
        isBackupFinished = true
        backupProgress = 1.0f
    }
}

@Composable
fun rememberBackUpDialogState(): CreateBackupDialogStateHolder {
    return remember("someData") { CreateBackupDialogStateHolder() }
}

sealed interface BackUpDialogStep {
    object SetPassword : BackUpDialogStep
    object CreatingBackup : BackUpDialogStep
    object Failure : BackUpDialogStep
}
