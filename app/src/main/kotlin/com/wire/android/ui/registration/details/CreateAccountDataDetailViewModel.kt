/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.registration.details

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.config.orDefault
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountDataDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
) : ViewModel() {

    val createAccountNavArgs: CreateAccountNavArgs = savedStateHandle.navArgs()

    val firstNameTextState: TextFieldState = TextFieldState()
    val lastNameTextState: TextFieldState = TextFieldState()
    val passwordTextState: TextFieldState = TextFieldState()
    val confirmPasswordTextState: TextFieldState = TextFieldState()
    val teamNameTextState: TextFieldState = TextFieldState()
    var detailsState: CreateAccountDataDetailViewState by mutableStateOf(CreateAccountDataDetailViewState(createAccountNavArgs.flowType))

    val serverConfig: ServerConfig.Links = createAccountNavArgs.customServerConfig.orDefault()

    init {
        viewModelScope.launch {
            combine(
                firstNameTextState.textAsFlow(),
                lastNameTextState.textAsFlow(),
                passwordTextState.textAsFlow(),
                confirmPasswordTextState.textAsFlow(),
                teamNameTextState.textAsFlow(),
            ) { firstName, lastName, password, confirmPassword, teamName ->
                firstName.isNotBlank() && lastName.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
                        && (detailsState.type == CreateAccountFlowType.CreatePersonalAccount || teamName.isNotBlank())
            }.collect { fieldsNotEmpty ->
                detailsState = detailsState.copy(
                    error = CreateAccountDataDetailViewState.DetailsError.None,
                    continueEnabled = fieldsNotEmpty && !detailsState.loading
                )
            }
        }
    }

    fun onDetailsContinue() {
        detailsState = detailsState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            val detailsError = when {
                !validatePasswordUseCase(passwordTextState.text.toString()).isValid ->
                    CreateAccountDataDetailViewState.DetailsError.TextFieldError.InvalidPasswordError

                passwordTextState.text.toString() != confirmPasswordTextState.text.toString() ->
                    CreateAccountDataDetailViewState.DetailsError.TextFieldError.PasswordsNotMatchingError

                else -> CreateAccountDataDetailViewState.DetailsError.None
            }
            detailsState = detailsState.copy(
                loading = false,
                continueEnabled = true,
                error = detailsError
            )
            if (detailsState.error is CreateAccountDataDetailViewState.DetailsError.None) detailsState = detailsState.copy(success = true)
        }
    }

    fun onDetailsErrorDismiss() {
        detailsState = detailsState.copy(error = CreateAccountDataDetailViewState.DetailsError.None)
    }
}
