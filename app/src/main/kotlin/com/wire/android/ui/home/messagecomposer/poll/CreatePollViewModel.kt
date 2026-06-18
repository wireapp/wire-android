/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer.poll

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.R
import com.wire.android.model.SnackBarMessage
import com.wire.android.model.asSnackBarMessage
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.feature.message.MessageOperationResult
import com.wire.kalium.logic.feature.message.poll.SendPollMessageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CreatePollViewModel(
    savedStateHandle: SavedStateHandle,
    private val sendPollMessage: SendPollMessageUseCase
) : ViewModel() {

    private val navArgs: CreatePollNavArgs = savedStateHandle.navArgs()

    val questionTextState = TextFieldState()
    val optionTextStates = mutableStateListOf(TextFieldState(), TextFieldState())

    var state: CreatePollState by mutableStateOf(CreatePollState())
        private set

    private val textWatchJobs = mutableMapOf<TextFieldState, Job>()

    private val _actions = MutableSharedFlow<CreatePollAction>()
    val actions = _actions.asSharedFlow()

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        watchTextState(questionTextState)
        optionTextStates.forEach(::watchTextState)
        updateCanSend()
    }

    fun addOption() {
        if (!state.canAddOption) return
        val optionTextState = TextFieldState()
        optionTextStates.add(optionTextState)
        watchTextState(optionTextState)
        updateCanSend()
    }

    fun removeOption(index: Int) {
        if (optionTextStates.size <= MIN_OPTIONS || index !in optionTextStates.indices) return
        val removedTextState = optionTextStates.removeAt(index)
        textWatchJobs.remove(removedTextState)?.cancel()
        updateCanSend()
    }

    fun setAllowMultipleAnswers(allowMultipleAnswers: Boolean) {
        state = state.copy(allowMultipleAnswers = allowMultipleAnswers)
    }

    fun setHideVoterNames(hideVoterNames: Boolean) {
        state = state.copy(hideVoterNames = hideVoterNames)
    }

    fun sendPoll() {
        if (!state.canSend || state.isSending) return
        viewModelScope.launch {
            state = state.copy(isSending = true, canSend = false)
            when (
                sendPollMessage(
                    conversationId = navArgs.conversationId,
                    question = questionTextState.trimmedText(),
                    options = pollOptions(),
                    allowMultipleAnswers = state.allowMultipleAnswers,
                    hideVoterNames = state.hideVoterNames
                )
            ) {
                is MessageOperationResult.Success -> _actions.emit(CreatePollAction.NavigateBack)
                is MessageOperationResult.Failure -> {
                    state = state.copy(isSending = false)
                    updateCanSend()
                    _infoMessage.emit(UIText.StringResource(R.string.create_poll_error_generic).asSnackBarMessage())
                }
            }
        }
    }

    private fun watchTextState(textFieldState: TextFieldState) {
        textWatchJobs[textFieldState] = viewModelScope.launch {
            textFieldState.textAsFlow().collect {
                updateCanSend()
            }
        }
    }

    private fun updateCanSend() {
        state = state.copy(
            canSend = !state.isSending && questionTextState.trimmedText().isNotEmpty() && pollOptions().size >= MIN_OPTIONS,
            canAddOption = optionTextStates.size < MAX_OPTIONS
        )
    }

    private fun pollOptions(): List<String> = optionTextStates
        .map { it.trimmedText() }
        .filter { it.isNotEmpty() }

    private fun TextFieldState.trimmedText(): String = text.toString().trim()

    companion object {
        const val MIN_OPTIONS = 2
        const val MAX_OPTIONS = 10
    }
}

data class CreatePollState(
    val allowMultipleAnswers: Boolean = false,
    val hideVoterNames: Boolean = false,
    val isSending: Boolean = false,
    val canSend: Boolean = false,
    val canAddOption: Boolean = true,
)

sealed interface CreatePollAction {
    data object NavigateBack : CreatePollAction
}
