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
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.ui.create.NewMeetingState.Companion.initialState
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModel.Companion.MEETING_NAME_MAX_COUNT
import com.wire.android.model.Contact
import com.wire.android.ui.common.ActionsManager
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.CurrentTimeProvider
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

interface NewMeetingViewModel : ActionsManager<NewMeetingViewActions> {
    val currentTimeProvider: CurrentTimeProvider
    val type: NewMeetingType
    val titleTextState: TextFieldState
    val state: NewMeetingState

    fun updateSelectedContact(selected: Boolean, contact: Contact) {}
    fun confirmSelectedContacts() {}
    fun resetSelectedContacts() {}
    fun updateStartTime(startTime: Instant) {}
    fun updateEndTime(endTime: Instant) {}
    fun updateRepeatingInterval(interval: MeetingItem.RepeatingInterval) {}
    fun createMeeting() {}

    companion object {
        const val MEETING_NAME_MAX_COUNT = 64
    }
}

class NewMeetingViewModelPreview(
    override val type: NewMeetingType
) : NewMeetingViewModel {
    override val currentTimeProvider: CurrentTimeProvider = CurrentTimeProvider.Preview
    override val titleTextState: TextFieldState = TextFieldState()
    override val state: NewMeetingState = initialState(currentTimeProvider)
}

class NewMeetingViewModelImpl(
    savedStateHandle: SavedStateHandle,
    override val currentTimeProvider: CurrentTimeProvider,
) : ActionsViewModel<NewMeetingViewActions>(), NewMeetingViewModel {
    val navArgs: NewMeetingNavArgs = savedStateHandle.navArgs()
    override val type: NewMeetingType = navArgs.type
    override val titleTextState: TextFieldState = TextFieldState()
    override var state: NewMeetingState by mutableStateOf(initialState(currentTimeProvider))
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

    override fun updateStartTime(startTime: Instant) {
        state = state.copy(startTime = startTime)
    }

    override fun updateEndTime(endTime: Instant) {
        state = state.copy(endTime = endTime)
    }

    override fun updateRepeatingInterval(interval: MeetingItem.RepeatingInterval) {
        state = state.copy(repeatingInterval = interval)
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

    private fun validateStartTime(): Boolean {
        state = state.copy(
            startTimeError = when {
                state.startTime < currentTimeProvider() -> NewMeetingState.TimeError.StartTimeInPastError
                else -> null
            }
        )
        return state.startTimeError == null
    }

    private fun validateEndTime(): Boolean {
        state = state.copy(
            endTimeError = when {
                state.endTime < currentTimeProvider() -> NewMeetingState.TimeError.EndTimeInPastError
                state.endTime < state.startTime -> NewMeetingState.TimeError.EndTimeBeforeStartTimeError
                else -> null
            }
        )
        return state.endTimeError == null
    }

    override fun createMeeting() {
        val titleValid = validateTitle()
        val startTimeValid = validateStartTime()
        val endTimeValid = validateEndTime()
        if (titleValid && startTimeValid && endTimeValid) {
            // TODO implement meeting creation
            sendAction(NewMeetingViewActions.Success)
        }
    }
}

internal fun getNextFullHour(now: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant {
    val localNow = now.toLocalDateTime(timeZone)
    val hasPassedTime = localNow.minute > 0 || localNow.second > 0 || localNow.nanosecond > 0
    val targetDateTime = if (hasPassedTime) {
        val futureHour = now.plus(1, DateTimeUnit.HOUR, timeZone)
        val localFuture = futureHour.toLocalDateTime(timeZone)
        LocalDateTime(
            year = localFuture.year,
            monthNumber = localFuture.monthNumber,
            dayOfMonth = localFuture.dayOfMonth,
            hour = localFuture.hour,
            minute = 0,
            second = 0,
            nanosecond = 0
        )
    } else {
        localNow
    }
    return targetDateTime.toInstant(timeZone)
}

@Stable
data class NewMeetingState(
    val selectedContacts: ImmutableSet<Contact> = persistentSetOf(),
    val confirmedContacts: ImmutableSet<Contact> = persistentSetOf(),
    val continueButtonEnabled: Boolean = false,
    val titleError: TitleError? = null,
    val startTime: Instant,
    val startTimeError: TimeError? = null,
    val endTime: Instant,
    val endTimeError: TimeError? = null,
    val repeatingInterval: MeetingItem.RepeatingInterval = MeetingItem.RepeatingInterval.Never,
) {
    @Stable
    sealed interface TitleError {
        data object TitleEmptyError : TitleError
        data object TitleExceedsLimitError : TitleError
    }

    sealed interface TimeError {
        data object StartTimeInPastError : TimeError
        data object EndTimeInPastError : TimeError
        data object EndTimeBeforeStartTimeError : TimeError
    }

    companion object {
        fun initialState(currentTimeProvider: CurrentTimeProvider): NewMeetingState {
            val startTime = getNextFullHour(currentTimeProvider())
            return NewMeetingState(startTime = startTime, endTime = startTime.plus(1.hours))
        }
    }
}

sealed interface NewMeetingViewActions {
    data object Success : NewMeetingViewActions
}
