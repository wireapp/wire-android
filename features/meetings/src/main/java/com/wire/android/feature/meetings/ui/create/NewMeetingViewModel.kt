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
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.feature.meetings.mapper.toRepeatingInterval
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.ui.create.NewMeetingState.Companion.initialState
import com.wire.android.feature.meetings.ui.create.NewMeetingState.InitialLoadingState
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModel.Companion.MEETING_NAME_MAX_COUNT
import com.wire.android.feature.meetings.ui.MeetingsManualViewModelFactoryGroup
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.Contact
import com.wire.android.ui.common.ActionsManager
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.util.time.CurrentTimeZoneProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.meeting.Meeting
import com.wire.kalium.logic.data.meeting.UpsertMeeting
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.conversation.RenameConversationUseCase
import com.wire.kalium.logic.feature.conversation.RenamingResult
import com.wire.kalium.logic.feature.meeting.CreateNewMeetingUseCase
import com.wire.kalium.logic.feature.meeting.GetNextUnfinishedMeetingOccurrenceUseCase
import com.wire.kalium.logic.feature.meeting.UpdateMeetingUseCase
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
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
    val currentTimeZoneProvider: CurrentTimeZoneProvider
    val type: NewMeetingType
    val titleTextState: TextFieldState
    val state: NewMeetingState

    fun updateSelectedContact(selected: Boolean, contact: Contact) {}
    fun confirmSelectedContacts() {}
    fun resetSelectedContacts() {}
    fun updateStartTime(startTime: Instant) {}
    fun updateEndTime(endTime: Instant) {}
    fun updateRepeatingInterval(interval: MeetingItem.RepeatingInterval?) {}
    fun submitCreation() {}
    fun submitUpdate() {}
    fun dismissCreationError() {}
    fun retryUpdateConversationName(conversationId: ConversationId) {}

    companion object {
        const val MEETING_NAME_MAX_COUNT = 64
    }
}

class NewMeetingViewModelPreview(
    override val type: NewMeetingType
) : NewMeetingViewModel {
    override val currentTimeProvider: CurrentTimeProvider = CurrentTimeProvider.Preview
    override val currentTimeZoneProvider: CurrentTimeZoneProvider = CurrentTimeZoneProvider.Preview
    override val titleTextState: TextFieldState = TextFieldState()
    override val state: NewMeetingState = initialState(currentTimeProvider, currentTimeZoneProvider)
}

@Suppress("TooManyFunctions")
@WireAssistedViewModelBinding(MeetingsManualViewModelFactoryGroup::class)
class NewMeetingViewModelImpl @AssistedInject constructor(
    @Assisted val navArgs: NewMeetingNavArgs,
    override val currentTimeProvider: CurrentTimeProvider,
    override val currentTimeZoneProvider: CurrentTimeZoneProvider,
    private val createNewMeeting: CreateNewMeetingUseCase,
    private val updateMeeting: UpdateMeetingUseCase,
    private val getNextUnfinishedMeetingOccurrence: GetNextUnfinishedMeetingOccurrenceUseCase,
    private val observeConversationMembers: ObserveConversationMembersUseCase,
    private val renameConversationUseCase: RenameConversationUseCase,
    private val contactMapper: ContactMapper,
) : ActionsViewModel<NewMeetingViewActions>(), NewMeetingViewModel {
    @AssistedFactory
    interface Factory {
        fun create(navArgs: NewMeetingNavArgs): NewMeetingViewModelImpl
    }

    override val type: NewMeetingType = navArgs.type
    override val titleTextState: TextFieldState = TextFieldState()
    override var state: NewMeetingState by mutableStateOf(initialState(currentTimeProvider, currentTimeZoneProvider))
        private set

    init {
        observeTitleChanges()
        loadInitialDataForEditing()
    }

    private fun loadInitialDataForEditing() {
        viewModelScope.launch {
            try {
                val meetingType = navArgs.type
                if (meetingType is NewMeetingType.Edit) {
                    val meetingOccurrence = getNextUnfinishedMeetingOccurrence(meetingType.id, currentTimeProvider())
                    if (meetingOccurrence != null) {
                        val otherContacts = observeConversationMembers(meetingOccurrence.meeting.conversationId).firstOrNull()?.let {
                            it.map { it.user }.filterIsInstance<OtherUser>().map { contactMapper.fromOtherUser(it) }.toPersistentSet()
                        } ?: persistentSetOf()
                        titleTextState.setTextAndPlaceCursorAtEnd(meetingOccurrence.meeting.title)
                        state = state.copy(
                            startTime = meetingOccurrence.occurrenceStartTime,
                            endTime = meetingOccurrence.occurrenceEndTime,
                            tzid = meetingOccurrence.meeting.tzid,
                            repeatingInterval = meetingOccurrence.meeting.recurrence?.toRepeatingInterval(),
                            selectedContacts = otherContacts,
                            confirmedContacts = otherContacts,
                        )
                    }
                }
                state = state.copy(initialLoading = InitialLoadingState.Loaded)
            } catch (_: Exception) {
                state = state.copy(initialLoading = InitialLoadingState.Error)
            }
        }
    }

    private fun observeTitleChanges() {
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
        val latestEndTime = startTime.latestEndTimeOnSameDay(currentTimeZoneProvider())
        val updatedEndTime = minOf(startTime.plus(currentDuration), latestEndTime)
        state = state.copy(
            startTime = startTime,
            // adjust end time based on the new start time but try to keep the same duration, unless it extends into the next day
            endTime = updatedEndTime,
            tzid = tzidAfterTimeChange(startTime != state.startTime || updatedEndTime != state.endTime)
        )
        validateStartAndEndTime()
    }

    override fun updateEndTime(endTime: Instant) {
        state = state.copy(
            endTime = endTime,
            tzid = tzidAfterTimeChange(endTime != state.endTime)
        )
        validateStartAndEndTime()
    }

    // While editing, keep the original meeting tzid unless the user changes a time value.
    // Time picker changes are made in the user's local timezone, so edited times should carry the local tzid.
    private fun tzidAfterTimeChange(timeChanged: Boolean): String =
        if (type is NewMeetingType.Edit && !timeChanged) state.tzid else currentTimeZoneProvider().id

    override fun updateRepeatingInterval(interval: MeetingItem.RepeatingInterval?) {
        state = state.copy(repeatingInterval = interval)
    }

    private fun validateTitle(): Boolean {
        val titleError = when {
            titleTextState.text.trim().isEmpty() -> NewMeetingState.TitleError.TitleEmptyError
            titleTextState.text.trim().length > MEETING_NAME_MAX_COUNT -> NewMeetingState.TitleError.TitleExceedsLimitError
            else -> null
        }
        state = state.copy(
            titleError = titleError,
            continueButtonEnabled = titleTextState.text.trim().isNotEmpty() &&
                    titleError == null &&
                    state.startTimeError == null &&
                    state.endTimeError == null
        )
        return titleError == null
    }

    private fun validateStartAndEndTime(): Boolean {
        val editing = type is NewMeetingType.Edit // for editing, we allow times in the past
        val startTimeError = when {
            !editing && state.startTime < currentTimeProvider() -> NewMeetingState.TimeError.StartTimeInPastError
            else -> null
        }
        val endTimeError = when {
            !editing && state.endTime < currentTimeProvider() -> NewMeetingState.TimeError.EndTimeInPastError
            state.endTime < state.startTime -> NewMeetingState.TimeError.EndTimeBeforeStartTimeError
            else -> null
        }
        state = state.copy(
            startTimeError = startTimeError,
            endTimeError = endTimeError,
            continueButtonEnabled = titleTextState.text.trim().isNotEmpty() &&
                    state.titleError == null &&
                    startTimeError == null &&
                    endTimeError == null
        )
        return startTimeError == null && endTimeError == null
    }

    override fun submitCreation() {
        viewModelScope.launch {
            val titleValid = validateTitle()
            val startAndEndTimeValid = when (type) {
                NewMeetingType.MeetNow -> {
                    val startTime = currentTimeProvider()
                    state = state.copy(startTime = startTime, endTime = startTime.plus(1.hours))
                    true // for "meet now", we set the start time to the current time and end time to +1 hour, so it's already valid
                }

                NewMeetingType.Schedule -> validateStartAndEndTime()
                is NewMeetingType.Edit -> false
            }
            if (titleValid && startAndEndTimeValid && !state.isSubmitting) {
                state = state.copy(isSubmitting = true, continueButtonEnabled = false)
                val creationResult = createNewMeeting(
                    createMeeting = UpsertMeeting(
                        title = titleTextState.text.trim().toString(),
                        startTime = state.startTime,
                        endTime = state.endTime,
                        tzid = state.tzid,
                        recurrence = state.repeatingInterval?.let { Meeting.Recurrence(it.frequency, it.interval.toLong(), null) },
                        otherParticipants = state.confirmedContacts.map { UserId(it.id, it.domain) }
                    )
                )
                state = state.copy(isSubmitting = false, continueButtonEnabled = true)
                when (creationResult) {
                    is CreateNewMeetingUseCase.Result.Success -> sendAction(NewMeetingViewActions.Success)
                    is CreateNewMeetingUseCase.Result.Failure -> state = state.copy(submitError = NewMeetingState.SubmitError.Other)
                }
            }
        }
    }

    override fun submitUpdate() {
        val meetingType = type as? NewMeetingType.Edit ?: return
        viewModelScope.launch {
            val titleValid = validateTitle()
            val startAndEndTimeValid = validateStartAndEndTime()
            if (titleValid && startAndEndTimeValid && !state.isSubmitting) {
                state = state.copy(isSubmitting = true, continueButtonEnabled = false)
                val updateResult = updateMeeting(
                    meetingId = meetingType.id,
                    updateMeeting = UpsertMeeting(
                        title = titleTextState.text.trim().toString(),
                        startTime = state.startTime,
                        endTime = state.endTime,
                        tzid = state.tzid,
                        recurrence = state.repeatingInterval?.let { Meeting.Recurrence(it.frequency, it.interval.toLong(), null) },
                        otherParticipants = state.confirmedContacts.map { UserId(it.id, it.domain) }
                    )
                )
                state = state.copy(isSubmitting = false, continueButtonEnabled = true)
                when (updateResult) {
                    is UpdateMeetingUseCase.Result.Success -> sendAction(NewMeetingViewActions.Success)
                    is UpdateMeetingUseCase.Result.Failure -> state = state.copy(
                        submitError = when (updateResult) {
                            is UpdateMeetingUseCase.Result.Failure.UpdateConversationNameFailure ->
                                NewMeetingState.SubmitError.UpdateConversationNameFailure(updateResult.conversationId)

                            else -> NewMeetingState.SubmitError.Other
                        }
                    )
                }
            }
        }
    }

    override fun retryUpdateConversationName(conversationId: ConversationId) {
        viewModelScope.launch {
            state = state.copy(isSubmitting = true, continueButtonEnabled = false)
            renameConversationUseCase(conversationId = conversationId, conversationName = titleTextState.text.trim().toString()).let {
                state = state.copy(isSubmitting = false, continueButtonEnabled = true)
                when (it) {
                    is RenamingResult.Failure ->
                        state = state.copy(submitError = NewMeetingState.SubmitError.UpdateConversationNameFailure(conversationId))
                    RenamingResult.Success -> sendAction(NewMeetingViewActions.Success)
                }
            }
        }
    }

    override fun dismissCreationError() {
        state = state.copy(submitError = null)
    }
}

internal fun getNextFullHour(now: Instant, timeZone: TimeZone): Instant {
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

// Find the latest possible end time on the same day as the given Instant, in the given time zone.
// The latest possible end time is 23:59:00 on the same day in the given time zone.
private fun Instant.latestEndTimeOnSameDay(timeZone: TimeZone): Instant {
    val localStartTime = toLocalDateTime(timeZone)
    return LocalDateTime(
        year = localStartTime.year,
        monthNumber = localStartTime.monthNumber,
        dayOfMonth = localStartTime.dayOfMonth,
        hour = 23,
        minute = 59,
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
    val tzid: String,
    val repeatingInterval: MeetingItem.RepeatingInterval? = null,
    val submitError: SubmitError? = null,
    val isSubmitting: Boolean = false,
    val initialLoading: InitialLoadingState = InitialLoadingState.Loading,
) {
    enum class InitialLoadingState { Error, Loading, Loaded }

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

    sealed interface SubmitError {
        data class UpdateConversationNameFailure(val conversationId: ConversationId) : SubmitError
        data object Other : SubmitError // TODO Add more specific error types in the future
    }

    companion object {
        fun initialState(currentTimeProvider: CurrentTimeProvider, currentTimeZoneProvider: CurrentTimeZoneProvider): NewMeetingState {
            val startTime = getNextFullHour(currentTimeProvider(), currentTimeZoneProvider())
            return NewMeetingState(
                startTime = startTime,
                endTime = startTime.plus(1.hours),
                tzid = currentTimeZoneProvider().id,
            )
        }
    }
}

sealed interface NewMeetingViewActions {
    data object Success : NewMeetingViewActions
}
