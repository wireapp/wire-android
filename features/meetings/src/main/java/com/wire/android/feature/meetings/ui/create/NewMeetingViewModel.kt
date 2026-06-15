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
package com.wire.android.feature.meetings.ui.create

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.meetings.navArgs
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModel.Companion.MEETING_NAME_MAX_COUNT
import com.wire.android.model.Contact
import com.wire.android.ui.common.ActionsManager
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface NewMeetingViewModel : ActionsManager<NewMeetingViewActions> {
    val type: NewMeetingType
    val titleTextState: TextFieldState
    val state: NewMeetingState

    fun updateSelectedContact(selected: Boolean, contact: Contact) {}
    fun confirmSelectedContacts() {}
    fun resetSelectedContacts() {}
    fun createMeeting() {}

    companion object {
        const val MEETING_NAME_MAX_COUNT = 64
    }
}

class NewMeetingViewModelPreview(
    override val type: NewMeetingType
) : NewMeetingViewModel {
    override val titleTextState: TextFieldState = TextFieldState()
    override val state: NewMeetingState = NewMeetingState()
}

class NewMeetingViewModelImpl(
    savedStateHandle: SavedStateHandle
) : ActionsViewModel<NewMeetingViewActions>(), NewMeetingViewModel {
    val navArgs: NewMeetingNavArgs = savedStateHandle.navArgs()
    override val type: NewMeetingType = navArgs.type
    override val titleTextState: TextFieldState = TextFieldState()
    override var state: NewMeetingState by mutableStateOf(NewMeetingState())
        private set

    init {
        viewModelScope.launch {
            titleTextState.textAsFlow().collectLatest {
                if (state.titleError != null) validateTitle()
                validateContinueButton()
            }
        }
    }

    override fun updateSelectedContact(selected: Boolean, contact: Contact) {
        state = state.copy(
            selectedContacts = when (selected) {
                true -> state.selectedContacts.plus(contact).toPersistentSet()
                false -> state.selectedContacts.minus(contact).toPersistentSet()
            }
        )
    }

    override fun confirmSelectedContacts() {
        state = state.copy(confirmedContacts = state.selectedContacts)
    }

    override fun resetSelectedContacts() {
        state = state.copy(selectedContacts = state.confirmedContacts)
    }

    private fun validateContinueButton() {
        state = state.copy(continueButtonEnabled = titleTextState.text.isNotEmpty())
    }

    private fun validateTitle(): Boolean {
        state = state.copy(
            titleError = when {
                titleTextState.text.isEmpty() -> NewMeetingState.TitleError.TitleEmptyError
                titleTextState.text.length > MEETING_NAME_MAX_COUNT -> NewMeetingState.TitleError.TitleExceedsLimitError
                else -> null
            }
        )
        return state.titleError == null
    }

    override fun createMeeting() {
        if (validateTitle()) {
            // TODO implement meeting creation
            sendAction(NewMeetingViewActions.Success)
        }
    }
}

@Stable
data class NewMeetingState(
    val selectedContacts: ImmutableSet<Contact> = persistentSetOf(),
    val confirmedContacts: ImmutableSet<Contact> = persistentSetOf(),
    val continueButtonEnabled: Boolean = false,
    val titleError: TitleError? = null,
) {
    @Stable
    sealed interface TitleError {
        data object TitleEmptyError : TitleError
        data object TitleExceedsLimitError : TitleError
    }
}

sealed interface NewMeetingViewActions {
    data object Success : NewMeetingViewActions
}
