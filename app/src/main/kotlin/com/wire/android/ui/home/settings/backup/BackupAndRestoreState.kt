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
import okio.Path

data class BackupAndRestoreState(
    val backupRestoreProgress: BackupRestoreProgress,
    val restoreFileValidation: RestoreFileValidation,
    val restorePasswordValidation: PasswordValidation,
    val backupCreationProgress: BackupCreationProgress,
    val passwordValidation: ValidatePasswordResult = ValidatePasswordResult.Valid
) {

    data class CreatedBackup(val path: Path, val assetName: String, val assetSize: Long, val isEncrypted: Boolean)
    companion object {
        val INITIAL_STATE = BackupAndRestoreState(
            backupRestoreProgress = BackupRestoreProgress.InProgress(),
            restoreFileValidation = RestoreFileValidation.Initial,
            backupCreationProgress = BackupCreationProgress.InProgress(),
            restorePasswordValidation = PasswordValidation.NotVerified,
            passwordValidation = ValidatePasswordResult.Valid,
        )
    }
}

sealed interface PasswordValidation {
    object NotVerified : PasswordValidation
    object Entered : PasswordValidation
    object NotValid : PasswordValidation
    object Valid : PasswordValidation
}

sealed interface BackupCreationProgress {
    data class Finished(val fileName: String) : BackupCreationProgress
    data class InProgress(val value: Float = 0f) : BackupCreationProgress
    object Failed : BackupCreationProgress
}

sealed interface BackupRestoreProgress {
    object Finished : BackupRestoreProgress
    data class InProgress(val value: Float = 0f) : BackupRestoreProgress
    object Failed : BackupRestoreProgress
}

sealed class RestoreFileValidation {
    object Initial : RestoreFileValidation()
    object ValidNonEncryptedBackup : RestoreFileValidation()
    object IncompatibleBackup : RestoreFileValidation()
    object WrongBackup : RestoreFileValidation()
    object GeneralFailure : RestoreFileValidation()
    object PasswordRequired : RestoreFileValidation()
}
