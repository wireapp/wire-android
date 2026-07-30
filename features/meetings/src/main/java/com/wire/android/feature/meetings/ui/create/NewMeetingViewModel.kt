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
import com.wire.kalium.logic.data.meeting.CreateMeeting
import com.wire.kalium.logic.data.meeting.Meeting
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.meeting.CreateNewMeetingUseCase
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
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
    fun updateRepeatingInterval(interval: MeetingItem.RepeatingInterval?) {}
    fun createMeeting() {}
    fun dismissCreationError() {}

    companion object {
        const val MEETING_NAME_MAX_COUNT = 128
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
    private val createNewMeeting: CreateNewMeetingUseCase,
) : ActionsViewModel<NewMeetingViewActions>(), NewMeetingViewModel {
    val navArgs: NewMeetingNavArgs = savedStateHandle.navArgs()
    override val type: NewMeetingType = navArgs.type
    override val titleTextState: TextFieldState = TextFieldState()
    override var state: NewMeetingState by mutableStateOf(initialState(currentTimeProvider))
        private set

    init {
        viewModelScope.launch {
            titleTextState.textAsFlow()
                .drop(1) // drop initial value to avoid showing error on start
                .collectLatest {
                    validateTitle()
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
        val currentDuration = state.endTime - state.startTime
        state = state.copy(
            startTime = startTime,
            endTime = startTime.plus(currentDuration) // adjust end time based on the new start time but keep the same duration
        )
        validateStartAndEndTime()
    }

    override fun updateEndTime(endTime: Instant) {
        state = state.copy(endTime = endTime)
        validateStartAndEndTime()
    }

    override fun updateRepeatingInterval(interval: MeetingItem.RepeatingInterval?) {
        state = state.copy(repeatingInterval = interval)
    }

    private fun validateTitle(): Boolean {
        state = state.copy(
            titleError = when {
                titleTextState.text.trim().isEmpty() -> NewMeetingState.TitleError.TitleEmptyError
                titleTextState.text.trim().length > MEETING_NAME_MAX_COUNT -> NewMeetingState.TitleError.TitleExceedsLimitError
                else -> null
            }
        ).withContinueButtonState()
        return state.titleError == null
    }

    private fun validateStartAndEndTime(): Boolean {
        state = state.copy(
            startTimeError = when {
                state.startTime < currentTimeProvider() -> NewMeetingState.TimeError.StartTimeInPastError
                else -> null
            },
            endTimeError = when {
                state.endTime < currentTimeProvider() -> NewMeetingState.TimeError.EndTimeInPastError
                state.endTime < state.startTime -> NewMeetingState.TimeError.EndTimeBeforeStartTimeError
                else -> null
            }
        ).withContinueButtonState()
        return state.startTimeError == null && state.endTimeError == null
    }

    private fun NewMeetingState.withContinueButtonState(): NewMeetingState = copy(
        continueButtonEnabled = titleTextState.text.trim().isNotEmpty() &&
                titleError == null &&
                startTimeError == null &&
                endTimeError == null
    )

    override fun createMeeting() {
        viewModelScope.launch {
            val titleValid = validateTitle()
            val startAndEndTimeValid = when (type) {
                NewMeetingType.MeetNow -> {
                    state = state.copy(startTime = currentTimeProvider(), endTime = currentTimeProvider().plus(1.hours))
                    true // for "meet now", we set the start time to the current time and end time to +1 hour, so it's already valid
                }

                NewMeetingType.Schedule -> validateStartAndEndTime()
            }
            if (titleValid && startAndEndTimeValid) {
                state = state.copy(isSubmitting = true, continueButtonEnabled = false)
                val creationResult = createNewMeeting(
                    CreateMeeting(
                        title = titleTextState.text.trim().toString(),
                        startTime = state.startTime,
                        endTime = state.endTime,
                        recurrence = state.repeatingInterval?.let { Meeting.Recurrence(it.frequency, it.interval.toLong(), null) },
                        otherParticipants = state.confirmedContacts.map { UserId(it.id, it.domain) }
                    )
                )
                state = state.copy(isSubmitting = false, continueButtonEnabled = true)
                when (creationResult) {
                    is CreateNewMeetingUseCase.Result.Success -> sendAction(NewMeetingViewActions.Success)
                    is CreateNewMeetingUseCase.Result.Failure -> state = state.copy(creationError = NewMeetingState.CreationError.Other)
                }
            }
        }
    }

    override fun dismissCreationError() {
        state = state.copy(creationError = null)
    }
}

internal fun getNextFullHour(now: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant {
    val futureHour = now.plus(1, DateTimeUnit.HOUR, timeZone)
    val localFuture = futureHour.toLocalDateTime(timeZone)
    return LocalDateTime(
            year = localFuture.year,
            monthNumber = localFuture.monthNumber,
            dayOfMonth = localFuture.dayOfMonth,
            hour = localFuture.hour,
            minute = 0,
            second = 0,
            nanosecond = 0
        ).toInstant(timeZone)
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
    val repeatingInterval: MeetingItem.RepeatingInterval? = null,
    val creationError: CreationError? = null,
    val isSubmitting: Boolean = false,
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

    sealed interface CreationError {
        data object Other : CreationError // TODO Add more specific error types in the future
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
