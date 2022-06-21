package com.wire.android.ui.authentication.create.code

import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.kalium.logic.configuration.server.ServerConfig

interface CreateAccountCodeViewModel {
    val codeState: CreateAccountCodeViewState
    fun onCodeChange(newValue: CodeFieldValue)
    fun resendCode()
    fun goBackToPreviousStep()
    fun onCodeErrorDismiss()
    fun onTooManyDevicesError()
}
