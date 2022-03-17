package com.wire.android.ui.authentication.create.details

import androidx.compose.ui.text.input.TextFieldValue

interface CreateAccountDetailsViewModel {
    val detailsState: CreateAccountDetailsViewState
    fun goBackToPreviousStep()
    fun onDetailsContinue()
    fun onDetailsChange(newText: TextFieldValue, fieldType: DetailsFieldType)

    enum class DetailsFieldType {
        FirstName, LastName, Password, ConfirmPassword, TeamName
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
