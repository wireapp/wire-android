package com.wire.android.ui.authentication.create.code

import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.kalium.logic.configuration.ServerConfig
import kotlinx.coroutines.flow.MutableSharedFlow

interface CreateAccountCodeViewModel {
    val codeState: CreateAccountCodeViewState
    val hideKeyboard: MutableSharedFlow<Unit>
    fun onCodeChange(newValue: CodeFieldValue, serverConfig: ServerConfig)
    fun resendCode(serverConfig: ServerConfig)
    fun goBackToPreviousStep()
    fun onCodeErrorDismiss()
}
