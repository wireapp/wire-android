/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.GenerateRandomPasswordUseCase
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePasswordGuestLinkViewModel @Inject constructor(
    private val generateGuestRoomLink: GenerateGuestRoomLinkUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val generateRandomPasswordUseCase: GenerateRandomPasswordUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editGuestAccessNavArgs: CreatePasswordGuestLinkNavArgs = savedStateHandle.navArgs<CreatePasswordGuestLinkNavArgs>()
    private val conversationId: QualifiedID = editGuestAccessNavArgs.conversationId

    val passwordTextState: TextFieldState = TextFieldState()
    val confirmPasswordTextState: TextFieldState = TextFieldState()
    var state by mutableStateOf(CreatePasswordGuestLinkState())
        @VisibleForTesting set

    init {
        viewModelScope.launch {
            combine(passwordTextState.textAsFlow(), confirmPasswordTextState.textAsFlow(), ::Pair)
                .distinctUntilChanged()
                .collectLatest { (password, confirmPassword) ->
                    state = state.copy(isPasswordValid = validatePassword(password.toString()).isValid && password == confirmPassword)
                }
        }
    }

    fun onGenerateLink() {
        state = state.copy(isLoading = true)
        viewModelScope.launch {
            generateGuestRoomLink(
                conversationId = conversationId,
                password = passwordTextState.text.toString()
            ).also { result ->
                state = if (result is GenerateGuestRoomLinkResult.Failure) {
                    state.copy(error = result.cause, isLoading = false)
                } else {
                    state.copy(error = null, isLoading = false, isLinkCreationSuccessful = true)
                }
            }
        }
    }

    fun onErrorDialogDismissed() {
        state = state.copy(error = null)
    }

    fun onGenerateRandomPassword() {
        val password = generateRandomPasswordUseCase()
        passwordTextState.setTextAndPlaceCursorAtEnd(password)
        confirmPasswordTextState.setTextAndPlaceCursorAtEnd(password)
    }
}
