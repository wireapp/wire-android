package com.wire.android.ui.authentication.create.code

import androidx.compose.ui.text.input.TextFieldValue

interface CodeViewModel {
    val codeState: CodeViewState
    fun onCodeChange(newValue: TextFieldValue)
    fun onResendCodePressed()
    fun onCodeContinue()
    fun goBackToPreviousStep()

    companion object {
        const val CODE_LENGTH = 6
    }
}
