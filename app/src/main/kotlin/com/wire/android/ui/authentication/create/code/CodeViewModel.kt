package com.wire.android.ui.authentication.create.code

import com.wire.android.ui.common.textfield.CodeFieldValue

interface CodeViewModel {
    val codeState: CodeViewState
    fun onCodeChange(newValue: CodeFieldValue)
    fun resendCode()
    fun onCodeContinue()
    fun goBackToPreviousStep()
}
