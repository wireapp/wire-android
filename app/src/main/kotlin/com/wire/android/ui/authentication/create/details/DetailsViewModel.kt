package com.wire.android.ui.authentication.create.details

import androidx.compose.ui.text.input.TextFieldValue

interface DetailsViewModel {
    val detailsState: DetailsViewState
    fun goBackToPreviousStep()
    fun onDetailsContinue()
    fun onDetailsChange(newText: TextFieldValue, fieldType: DetailsFieldType)

    enum class DetailsFieldType {
        FirstName, LastName, Password, ConfirmPassword
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
