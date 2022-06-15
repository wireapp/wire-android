package com.wire.android.ui.authentication.create.email

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.configuration.server.ServerConfig

interface CreateAccountEmailViewModel {
    val emailState: CreateAccountEmailViewState
    fun onEmailChange(newText: TextFieldValue)
    fun goBackToPreviousStep()
    fun onEmailContinue()
    fun openLogin()
    fun onTermsDialogDismiss()
    fun onTermsAccept()
    fun onEmailErrorDismiss()
}
