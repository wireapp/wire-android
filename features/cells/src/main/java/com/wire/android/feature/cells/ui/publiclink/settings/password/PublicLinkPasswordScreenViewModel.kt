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
package com.wire.android.feature.cells.ui.publiclink.settings.password

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.UpdatePublicLinkPasswordUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.util.RandomPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PublicLinkPasswordScreenViewModel @Inject constructor(
    private val generateRandomPassword: RandomPassword,
    private val createPassword: CreatePublicLinkPasswordUseCase,
    private val updatePassword: UpdatePublicLinkPasswordUseCase,
    val savedStateHandle: SavedStateHandle,
) : ActionsViewModel<PublicLinkPasswordScreenAction>() {

    private val navArgs: PublicLinkPasswordNavArgs = savedStateHandle.navArgs()

    internal var isPasswordCreated = navArgs.passwordEnabled
        private set

    private val _state = MutableStateFlow(
        PublicLinkPasswordScreenViewState(
            isEnabled = navArgs.passwordEnabled,
        )
    )

    val state = _state.asStateFlow()
    val passwordTextState: TextFieldState = TextFieldState()

    init {
        viewModelScope.launch {
            passwordTextState.textAsFlow()
                .debounce(TYPING_DEBOUNCE_TIME)
                .collect { value ->
                    updateState { copy(isPasswordValid = value.isNotEmpty()) }
                }
        }

        if (isPasswordCreated) {
            loadPasswordFromLocalStorage()
        }
    }

    fun onEnableClick() {

        val passwordEnabled = !_state.value.isEnabled

        if (passwordEnabled) {
            updateState {
                copy(
                    isEnabled = true,
                    screenState = PasswordScreenState.SETUP_PASSWORD,
                )
            }
        } else {
            updateState { copy(isEnabled = false) }
            if (isPasswordCreated) {
                removePassword()
            }
        }
    }

    fun generatePassword() {
        val password = generateRandomPassword()
        passwordTextState.setTextAndPlaceCursorAtEnd(password)
    }

    fun setPassword() = viewModelScope.launch {
        updateState { copy(isUpdating = true) }
        val password = passwordTextState.text.toString()
        if (isPasswordCreated) {
            updatePassword(navArgs.linkUuid, password)
        } else {
            createPassword(navArgs.linkUuid, password)
        }
            .onSuccess {
                isPasswordCreated = true
                sendAction(CopyPasswordAndClose(password))
            }
            .onFailure {
                sendAction(ShowError(R.string.public_link_set_password_error))
                updateState { copy(isUpdating = false) }
            }
    }

    fun resetPassword() = viewModelScope.launch {
        updateState {
            copy(
                isPasswordValid = false,
                screenState = PasswordScreenState.SETUP_PASSWORD,
            )
        }
        passwordTextState.clearText()
    }

    private fun removePassword() = viewModelScope.launch {
        updatePassword(navArgs.linkUuid, null)
            .onSuccess {
                updateState {
                    copy(
                        isEnabled = false,
                        screenState = PasswordScreenState.SETUP_PASSWORD,
                    )
                }
                isPasswordCreated = false
                passwordTextState.clearText()
            }
            .onFailure {
                sendAction(ShowError(R.string.public_link_remove_password_error))
                updateState { copy(isEnabled = true) }
            }
    }

    private fun loadPasswordFromLocalStorage() {
        // TODO: implement saving/loading password in database
        updateState {
            copy(
                isPasswordValid = false,
                screenState = PasswordScreenState.AVAILABLE,
            )
        }
        passwordTextState.setTextAndPlaceCursorAtEnd("test_password_placeholder")
    }

    private fun updateState(block: PublicLinkPasswordScreenViewState.() -> PublicLinkPasswordScreenViewState) {
        _state.update(block)
    }

    private companion object {
        const val TYPING_DEBOUNCE_TIME = 200L
    }
}

internal sealed interface PublicLinkPasswordScreenAction
internal data class CopyPasswordAndClose(val password: String) : PublicLinkPasswordScreenAction
internal data class ShowError(val message: Int) : PublicLinkPasswordScreenAction

internal data class PublicLinkPasswordScreenViewState(
    val isEnabled: Boolean = false,
    val isPasswordValid: Boolean = false,
    val isUpdating: Boolean = false,
    val screenState: PasswordScreenState = PasswordScreenState.INITIAL,
)

internal enum class PasswordScreenState {
    INITIAL, SETUP_PASSWORD, AVAILABLE, NOT_AVAILABLE
}
