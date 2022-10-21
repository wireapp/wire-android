package com.wire.android.ui.home.settings.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class BackUpAndRestoreStateHolder {

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
    return remember {
        BackUpAndRestoreStateHolder()
    }
}

sealed class BackupAndRestoreDialog {
    object None : BackupAndRestoreDialog()
    object CreateBackup : BackupAndRestoreDialog()
    object RestoreBackup : BackupAndRestoreDialog()
}

