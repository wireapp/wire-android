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

import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.backup.BackupFileFormat
import okio.Path

data class BackupAndRestoreState(
    val backupRestoreProgress: BackupRestoreProgress,
    val restoreFileValidation: RestoreFileValidation,
    val restorePasswordValidation: PasswordValidation,
    val backupCreationProgress: BackupCreationProgress,
    val lastBackupData: Long?,
    val passwordValidation: ValidatePasswordResult,
    val backupFileFormat: BackupFileFormat,
) {

    data class CreatedBackup(val path: Path, val assetName: String, val isEncrypted: Boolean)

    companion object {
        val INITIAL_STATE = BackupAndRestoreState(
            backupRestoreProgress = BackupRestoreProgress.InProgress(),
            restoreFileValidation = RestoreFileValidation.Initial,
            backupCreationProgress = BackupCreationProgress.InProgress(),
            restorePasswordValidation = PasswordValidation.NotVerified,
            passwordValidation = ValidatePasswordResult.Valid,
            lastBackupData = null,
            backupFileFormat = BackupFileFormat.ANDROID,
        )
    }
}

sealed interface PasswordValidation {
    data object NotVerified : PasswordValidation
    data object Entered : PasswordValidation
    data object NotValid : PasswordValidation
    data object Valid : PasswordValidation
}

sealed interface BackupCreationProgress {
    data class Finished(val fileName: String) : BackupCreationProgress
    data class InProgress(val value: Float = 0f) : BackupCreationProgress
    data object Failed : BackupCreationProgress
}

sealed interface BackupRestoreProgress {
    data object Finished : BackupRestoreProgress
    data class InProgress(val value: Float = 0f) : BackupRestoreProgress
    data object Failed : BackupRestoreProgress
}

sealed class RestoreFileValidation {
    data object Initial : RestoreFileValidation()
    data object ValidNonEncryptedBackup : RestoreFileValidation()
    data object IncompatibleBackup : RestoreFileValidation()
    data object WrongBackup : RestoreFileValidation()
    data object GeneralFailure : RestoreFileValidation()
    data object PasswordRequired : RestoreFileValidation()
}
