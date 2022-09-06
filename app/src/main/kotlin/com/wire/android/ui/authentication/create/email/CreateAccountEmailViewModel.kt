package com.wire.android.ui.authentication.create.email

import androidx.compose.ui.text.input.TextFieldValue

interface CreateAccountEmailViewModel {
    val emailState: CreateAccountEmailViewState
    fun tosUrl(): String
    fun onEmailChange(newText: TextFieldValue)
    fun goBackToPreviousStep()
    fun onEmailContinue()
    fun openLogin()
    fun onTermsDialogDismiss()
    fun onTermsAccept()
    fun onEmailErrorDismiss()
    fun dismissClientUpdateDialog()

    fun dismissApiVersionNotSupportedDialog()

    fun updateTheApp()
}
