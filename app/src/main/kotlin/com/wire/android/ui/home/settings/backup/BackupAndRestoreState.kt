package com.wire.android.ui.home.settings.backup

import okio.Path

data class BackupAndRestoreState(
    val backupRestoreProgress: BackupRestoreProgress,
    val restoreFileValidation: RestoreFileValidation,
    val restorePasswordValidation: PasswordValidation,
    val backupCreationProgress: BackupCreationProgress,
    val backupCreationPasswordValidation: PasswordValidation
) {

    data class CreatedBackup(val path: Path, val assetName: String, val assetSize: Long, val isEncrypted: Boolean)
    companion object {
        val INITIAL_STATE = BackupAndRestoreState(
            backupRestoreProgress = BackupRestoreProgress.InProgress(),
            restoreFileValidation = RestoreFileValidation.Pending,
            backupCreationProgress = BackupCreationProgress.InProgress(),
            restorePasswordValidation = PasswordValidation.NotVerified,
            backupCreationPasswordValidation = PasswordValidation.Valid,
        )
    }
}

sealed interface PasswordValidation {
    object NotVerified : PasswordValidation
    object NotValid : PasswordValidation
    object Valid : PasswordValidation
}

sealed interface BackupCreationProgress {
    object Finished : BackupCreationProgress
    data class InProgress(val value: Float = 0f) : BackupCreationProgress
    object Failed : BackupCreationProgress
}

sealed interface BackupRestoreProgress {
    object Finished : BackupRestoreProgress
    data class InProgress(val value: Float = 0f) : BackupRestoreProgress
    object Failed : BackupRestoreProgress
}

sealed class RestoreFileValidation {
    object Pending : RestoreFileValidation()
    object ValidNonEncryptedBackup : RestoreFileValidation()
    object IncompatibleBackup : RestoreFileValidation()
    object WrongBackup : RestoreFileValidation()
    object GeneralFailure : RestoreFileValidation()
    object PasswordRequired : RestoreFileValidation()
}
