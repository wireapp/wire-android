package com.wire.android.ui.authentication.create.code

import com.wire.android.ui.common.textfield.CodeFieldValue

interface CreateAccountCodeViewModel {
    val codeState: CreateAccountCodeViewState
    fun onCodeChange(newValue: CodeFieldValue)
    fun resendCode()
    fun onCodeContinue()
    fun goBackToPreviousStep()
}
