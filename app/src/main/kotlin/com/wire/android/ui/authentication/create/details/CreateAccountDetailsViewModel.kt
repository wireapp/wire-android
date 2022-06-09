package com.wire.android.ui.authentication.create.details

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.configuration.server.ServerConfig

interface CreateAccountDetailsViewModel {
    val detailsState: CreateAccountDetailsViewState
    fun goBackToPreviousStep()
    fun onDetailsContinue(serverConfig: ServerConfig)
    fun onDetailsChange(newText: TextFieldValue, fieldType: DetailsFieldType)
    fun onDetailsErrorDismiss()

    enum class DetailsFieldType {
        FirstName, LastName, Password, ConfirmPassword, TeamName
    }

    companion object {
        const val EMAIL = "email"
    }
}
