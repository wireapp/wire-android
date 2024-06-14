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
package com.wire.android.ui.joinConversation

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinConversationViaCodeViewModel @Inject constructor(
    private val joinViaCode: JoinConversationViaCodeUseCase
) : ViewModel() {

    var state: JoinViaDeepLinkDialogState by mutableStateOf(JoinViaDeepLinkDialogState.Idle)
        @VisibleForTesting set

    val passwordTextState: TextFieldState = TextFieldState()

    private fun passwordOrNull() = passwordTextState.text.toString().ifBlank { null }

    init {
        viewModelScope.launch {
            passwordTextState.textAsFlow().distinctUntilChanged().collectLatest {
                state = JoinViaDeepLinkDialogState.Idle
            }
        }
    }

    fun joinConversationViaCode(
        code: String,
        key: String,
        domain: String?
    ) {
        state = JoinViaDeepLinkDialogState.Loading
        viewModelScope.launch {
            val result = joinViaCode(
                code = code,
                key = key,
                domain = domain,
                password = passwordOrNull()
            )
            state = when (result) {
                is JoinConversationViaCodeUseCase.Result.Success -> JoinViaDeepLinkDialogState.Success(result.conversationId)
                is JoinConversationViaCodeUseCase.Result.Failure.Generic -> JoinViaDeepLinkDialogState.UnknownError
                JoinConversationViaCodeUseCase.Result.Failure.IncorrectPassword -> JoinViaDeepLinkDialogState.WrongPassword
            }
        }
    }
}
