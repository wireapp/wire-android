package com.wire.android.ui.authentication.create.code

import com.wire.android.ui.common.textfield.CodeFieldValue
import kotlinx.coroutines.flow.MutableSharedFlow

interface CreateAccountCodeViewModel {
    val codeState: CreateAccountCodeViewState
    val hideKeyboard: MutableSharedFlow<Unit>
    fun onCodeChange(newValue: CodeFieldValue)
    fun resendCode()
    fun onCodeContinue()
    fun goBackToPreviousStep()
}
