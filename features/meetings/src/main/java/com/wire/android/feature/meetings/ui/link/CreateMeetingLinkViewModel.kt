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
package com.wire.android.feature.meetings.ui.link

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.wire.android.di.CurrentAccount
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.feature.meetings.model.MeetingLink
import com.wire.android.feature.meetings.ui.MeetingsManualViewModelFactoryGroup
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModel.Companion.GENERATED_PASSWORD_LENGTH
import com.wire.android.ui.common.ActionsManager
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.meeting.ObserveMeetingUseCase
import com.wire.kalium.logic.feature.meeting.RotateMeetingLinkUseCase
import com.wire.kalium.logic.util.RandomPassword
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

interface CreateMeetingLinkViewModel : ActionsManager<CreateMeetingLinkViewAction> {
    val state: CreateMeetingLinkState
    val passwordTextState: TextFieldState
    val confirmPasswordTextState: TextFieldState
    fun onCreate(confirmWithoutPassword: Boolean = false)
    fun onDismissError() {}
    fun onDismissConfirmation()
    fun onGenerateRandomPassword()
}

@WireAssistedViewModelBinding(MeetingsManualViewModelFactoryGroup::class)
class CreateMeetingLinkViewModelImpl @AssistedInject constructor(
    @Assisted private val meetingId: MeetingId,
    private val observeMeeting: ObserveMeetingUseCase,
    private val rotateMeetingLink: RotateMeetingLinkUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val generatePassword: RandomPassword,
    @CurrentAccount private val selfUserId: UserId,
) : ActionsViewModel<CreateMeetingLinkViewAction>(), CreateMeetingLinkViewModel {
    @AssistedFactory
    interface Factory {
        fun create(meetingId: MeetingId): CreateMeetingLinkViewModelImpl
    }

    override var state: CreateMeetingLinkState by mutableStateOf(CreateMeetingLinkState.Loading)
        private set

    override val passwordTextState: TextFieldState = TextFieldState()
    override val confirmPasswordTextState: TextFieldState = TextFieldState()

    init {
        observePasswordChanges()
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            observeMeeting(meetingId).collect { meeting ->
                state = if (meeting == null || meeting.creatorId != selfUserId) {
                    CreateMeetingLinkState.NotAvailable
                } else {
                    (state as? CreateMeetingLinkState.Ready)?.copy(meetingLink = MeetingLink(meeting.link, meeting.hasPassword))
                        ?: CreateMeetingLinkState.Ready(MeetingLink(meeting.link, meeting.hasPassword))
                }
            }
        }
    }

    override fun onDismissError() {
        val ready = state as? CreateMeetingLinkState.Ready ?: return
        state = ready.copy(submitError = false)
    }

    override fun onDismissConfirmation() {
        val ready = state as? CreateMeetingLinkState.Ready ?: return
        state = ready.copy(confirmWithoutPassword = false)
    }

    private fun observePasswordChanges() {
        viewModelScope.launch {
            combine(passwordTextState.textAsFlow(), confirmPasswordTextState.textAsFlow(), ::Pair)
                .drop(1)
                .collectLatest { clearPasswordError() }
        }
    }

    private fun clearPasswordError() {
        val ready = state as? CreateMeetingLinkState.Ready ?: return
        state = ready.copy(invalidPassword = false, passwordsDoNotMatch = false, confirmWithoutPassword = false)
    }

    override fun onGenerateRandomPassword() {
        if ((state as? CreateMeetingLinkState.Ready)?.isSubmitting == false) {
            val password = generatePassword(minLength = GENERATED_PASSWORD_LENGTH, maxLength = GENERATED_PASSWORD_LENGTH)
            passwordTextState.setTextAndPlaceCursorAtEnd(password)
            confirmPasswordTextState.setTextAndPlaceCursorAtEnd(password)
        }
    }

    private val optionalPassword: String? get() = passwordTextState.text.toString().takeIf { it.isNotEmpty() }

    private fun validateOptionalPassword(): Boolean {
        val ready = state as? CreateMeetingLinkState.Ready ?: return false
        val password = optionalPassword
        val invalidPassword = password?.let { !validatePassword(it).isValid } ?: false
        val passwordsDoNotMatch = password != null && password != confirmPasswordTextState.text.toString()
        val passwordValid = !invalidPassword && !passwordsDoNotMatch
        state = ready.copy(invalidPassword = invalidPassword, passwordsDoNotMatch = passwordsDoNotMatch)
        return passwordValid
    }

    override fun onCreate(confirmWithoutPassword: Boolean) {
        val ready = state as? CreateMeetingLinkState.Ready ?: return
        if (!ready.isSubmitting && validateOptionalPassword()) {
            val validatedState = state as CreateMeetingLinkState.Ready
            val password = optionalPassword
            if (validatedState.meetingLink.hasPassword && password == null && !confirmWithoutPassword) {
                state = validatedState.copy(confirmWithoutPassword = true)
            } else {
                createLink(validatedState, password)
            }
        }
    }

    private fun createLink(ready: CreateMeetingLinkState.Ready, password: String?) {
        state = ready.copy(isSubmitting = true, confirmWithoutPassword = false, submitError = false)
        viewModelScope.launch {
            when (rotateMeetingLink(meetingId, password)) {
                is RotateMeetingLinkUseCase.Result.Failure -> {
                    (state as? CreateMeetingLinkState.Ready)?.let {
                        state = it.copy(isSubmitting = false, submitError = true)
                    }
                }
                is RotateMeetingLinkUseCase.Result.Success -> sendAction(CreateMeetingLinkViewAction.Success)
            }
        }
    }
}

sealed interface CreateMeetingLinkState {
    data object Loading : CreateMeetingLinkState
    data object NotAvailable : CreateMeetingLinkState
    data class Ready(
        val meetingLink: MeetingLink,
        val isSubmitting: Boolean = false,
        val submitError: Boolean = false,
        val confirmWithoutPassword: Boolean = false,
        val invalidPassword: Boolean = false,
        val passwordsDoNotMatch: Boolean = false,
    ) : CreateMeetingLinkState
}

sealed interface CreateMeetingLinkViewAction {
    data object Success : CreateMeetingLinkViewAction
}
