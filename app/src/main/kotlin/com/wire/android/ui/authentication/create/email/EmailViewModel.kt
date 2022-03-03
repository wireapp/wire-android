package com.wire.android.ui.authentication.create.email

import androidx.compose.ui.text.input.TextFieldValue

interface EmailViewModel {
    val emailState: EmailViewState
    fun onEmailChange(newText: TextFieldValue)
    fun goBackToPreviousStep()
    fun onEmailContinue()
    fun openLogin()
    fun onTermsDialogDismiss()
    fun onTermsAccepted()
}
