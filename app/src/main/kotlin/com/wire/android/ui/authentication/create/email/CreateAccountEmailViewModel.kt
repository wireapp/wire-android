package com.wire.android.ui.authentication.create.email

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.configuration.ServerConfig

interface CreateAccountEmailViewModel {
    val emailState: CreateAccountEmailViewState
    fun onEmailChange(newText: TextFieldValue)
    fun goBackToPreviousStep()
    fun onEmailContinue(serverConfig: ServerConfig)
    fun openLogin()
    fun onTermsDialogDismiss()
    fun onTermsAccept(serverConfig: ServerConfig)
    fun onEmailErrorDismiss()
}
