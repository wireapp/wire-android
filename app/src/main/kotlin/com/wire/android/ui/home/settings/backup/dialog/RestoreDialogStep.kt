package com.wire.android.ui.home.settings.backup.dialog

sealed interface RestoreDialogStep {
    object Inform : RestoreDialogStep
    object Failure : RestoreDialogStep, RestoreFailures
    object Restore : RestoreDialogStep
}

sealed interface RestoreFailures {
    object IncompatibleBackup : RestoreFailures
    object WrongBackup : RestoreFailures
    object SomethingWentWrong : RestoreFailures
    object WrongPassword : RestoreFailures
}
