/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.authentication.create.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Cover this viewModel  with unit test
@HiltViewModel
class CreateAccountDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    authServerConfigProvider: AuthServerConfigProvider
) : ViewModel() {

    val createAccountNavArgs: CreateAccountNavArgs = savedStateHandle.navArgs()

    var detailsState: CreateAccountDetailsViewState by mutableStateOf(CreateAccountDetailsViewState(createAccountNavArgs.flowType))

    val serverConfig: ServerConfig.Links = authServerConfigProvider.authServer.value

    fun onDetailsChange(newText: TextFieldValue, fieldType: DetailsFieldType) {
        detailsState = when (fieldType) {
            DetailsFieldType.FirstName -> detailsState.copy(firstName = newText)
            DetailsFieldType.LastName -> detailsState.copy(lastName = newText)
            DetailsFieldType.Password -> detailsState.copy(password = newText)
            DetailsFieldType.ConfirmPassword -> detailsState.copy(confirmPassword = newText)
            DetailsFieldType.TeamName -> detailsState.copy(teamName = newText)
        }.let {
            it.copy(
                error = CreateAccountDetailsViewState.DetailsError.None,
                continueEnabled = it.fieldsNotEmpty() && !it.loading
            )
        }
    }

    fun onDetailsContinue(onSuccess: () -> Unit) {
        detailsState = detailsState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            val detailsError = when {
                !validatePasswordUseCase(detailsState.password.text).isValid ->
                    CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError

                detailsState.password.text != detailsState.confirmPassword.text ->
                    CreateAccountDetailsViewState.DetailsError.TextFieldError.PasswordsNotMatchingError

                else -> CreateAccountDetailsViewState.DetailsError.None
            }
            detailsState = detailsState.copy(
                loading = false,
                continueEnabled = true,
                error = detailsError
            )
            if (detailsState.error is CreateAccountDetailsViewState.DetailsError.None) onSuccess()
        }
    }

    fun onDetailsErrorDismiss() {
        detailsState = detailsState.copy(error = CreateAccountDetailsViewState.DetailsError.None)
    }

    enum class DetailsFieldType {
        FirstName, LastName, Password, ConfirmPassword, TeamName
    }
}
