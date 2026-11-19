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

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.feature.meetings.mapper.toRepeatingInterval
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.ui.NewMeetingResult
import com.wire.android.framework.TestUser
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.Contact
import com.wire.android.search.groups.SearchGroup
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.util.time.CurrentTimeZoneProvider
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.meeting.CreateMeeting
import com.wire.kalium.logic.data.meeting.Meeting
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.data.meeting.UpdateMeeting
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.conversation.RenameConversationUseCase
import com.wire.kalium.logic.feature.conversation.RenamingResult
import com.wire.kalium.logic.feature.meeting.CreateNewMeetingUseCase
import com.wire.kalium.logic.feature.meeting.GetNextUnfinishedMeetingOccurrenceUseCase
import com.wire.kalium.logic.feature.meeting.UpdateMeetingUseCase
import com.wire.kalium.logic.feature.user.IsMeetingsEnabledUseCase
import com.wire.kalium.logic.util.RandomPassword
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@Suppress("LargeClass")
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class NewMeetingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenInitializationPending_whenFormEventsArrive_thenStateRemainsLoadingUntilReady() = runTest(dispatcher) {
        val guestLinkSupport = CompletableDeferred<Boolean>()
        val arrangement = Arrangement(dispatcher).withNewMeetingType(NewMeetingType.Schedule)
        coEvery { arrangement.isMeetingsEnabledUseCase.isGuestLinkSharingEnabled() } coAnswers { guestLinkSupport.await() }
        val (_, viewModel) = arrangeViewModel(arrangement)
        assertEquals(NewMeetingState.Loading, viewModel.state)

        enterTitle(viewModel, "Pending meeting")
        viewModel.updateSelectedEmail(true, "guest@example.com")
        viewModel.updateSelectedContact(true, CONTACT)
        viewModel.confirmSelectedContacts()
        viewModel.updateEndTime(Instant.parse("2026-01-01T13:00:00Z"))
        viewModel.onGenerateRandomPassword()
        viewModel.submitCreation()
        runCurrent()

        assertEquals(NewMeetingState.Loading, viewModel.state)
        coVerify(exactly = 0) { arrangement.createNewMeeting(any()) }
        guestLinkSupport.complete(true)
        runCurrent()
        assertTrue(viewModel.state is NewMeetingState.Ready)
        assertEquals(emptySet<String>(), viewModel.readyState.confirmedEmails)
        assertEquals(emptySet<Contact>(), viewModel.readyState.confirmedContacts)
        assertTrue(viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenInitializationFails_whenFormEventsArrive_thenStateRemainsErrorAndNoUpdateIsSubmitted() = runTest(dispatcher) {
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrenceFailure()
        )

        enterTitle(viewModel, "Changed title")
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("Password1!")
        viewModel.updateSelectedEmail(true, "guest@example.com")
        viewModel.confirmSelectedContacts()
        viewModel.dismissCreationError()
        viewModel.submitUpdate()
        viewModel.retryUpdateConversationName(CONVERSATION_ID)
        runCurrent()

        assertEquals(NewMeetingState.Error, viewModel.state)
        coVerify(exactly = 0) { arrangement.updateMeeting(any(), any()) }
        coVerify(exactly = 0) { arrangement.renameConversationUseCase(any(), any()) }
    }

    @Test
    fun givenEmailRejectedBySharedValidator_whenSelecting_thenItIsNotAdded() = runTest(dispatcher) {
        val arrangement = Arrangement(dispatcher).withNewMeetingType(NewMeetingType.Schedule)
        every { arrangement.validateEmail("person@example.com") } returns false
        val (_, viewModel) = arrangeViewModel(arrangement)

        viewModel.updateSelectedEmail(true, " person@example.com ")

        assertEquals(emptySet<String>(), viewModel.readyState.selectedEmails)
        verify(exactly = 1) { arrangement.validateEmail("person@example.com") }
    }

    @Test
    fun givenEmailSelections_whenValidatingConfirmingAndResetting_thenOnlyUniqueValidConfirmedEmailsRemain() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(NewMeetingType.Schedule))
        viewModel.updateSelectedEmail(true, "invalid")
        viewModel.updateSelectedEmail(true, " person@example.com ")
        viewModel.updateSelectedEmail(true, "PERSON@EXAMPLE.COM")
        assertEquals(setOf("person@example.com"), viewModel.readyState.selectedEmails)
        assertEquals(emptySet<String>(), viewModel.readyState.confirmedEmails)

        viewModel.confirmSelectedContacts()
        viewModel.updateSelectedEmail(false, "person@example.com")
        assertEquals(emptySet<String>(), viewModel.readyState.selectedEmails)
        viewModel.resetSelectedContacts()
        assertEquals(setOf("person@example.com"), viewModel.readyState.selectedEmails)
        viewModel.updateSelectedEmail(false, "person@example.com")
        viewModel.confirmSelectedContacts()
        assertEquals(emptySet<String>(), viewModel.readyState.confirmedEmails)
    }

    @Test
    fun givenMeetNow_whenAddingEmail_thenEmailIsNotSelected() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(NewMeetingType.MeetNow))
        viewModel.updateSelectedEmail(true, "person@example.com")
        viewModel.confirmSelectedContacts()
        assertEquals(emptySet<String>(), viewModel.readyState.selectedEmails)
        assertEquals(emptySet<String>(), viewModel.readyState.confirmedEmails)
    }

    @Test
    fun givenMeetNowWithPreviousTimeError_whenCreating_thenGeneratedTimesAreTreatedAsValid() = runTest(dispatcher) {
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.MeetNow)
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
        )
        enterTitle(viewModel, "Quick meeting")
        viewModel.updateEndTime(viewModel.readyState.startTime - 1.hours)
        val previousError = NewMeetingState.TimeError.EndTimeBeforeStartTimeError
        assertEquals(previousError, viewModel.readyState.endTimeError)

        viewModel.submitCreation()
        runCurrent()

        coVerify(exactly = 1) {
            arrangement.createNewMeeting(
                match { it.startTime == arrangement.currentTimeProvider() && it.endTime == it.startTime + 1.hours }
            )
        }
        assertEquals(previousError, viewModel.readyState.endTimeError)
    }

    @Test
    fun givenImmediateMeeting_whenEditing_thenEmailInviteesCannotBeAdded() = runTest(dispatcher) {
        val occurrence = MEETING_OCCURRENCE.copy(meeting = MEETING_OCCURRENCE.meeting.copy(type = Meeting.Type.IMMEDIATE))
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrence(occurrence)
        )

        viewModel.updateSelectedEmail(true, "person@example.com")
        viewModel.confirmSelectedContacts()

        assertEquals(Meeting.Type.IMMEDIATE, viewModel.readyState.meetingType)
        assertEquals(emptySet<String>(), viewModel.readyState.confirmedEmails)
        assertFalse(viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenExistingEmailInvitees_whenEditing_thenEmailsAreLoadedAndSubmitted() = runTest(dispatcher) {
        val occurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(invitedEmails = listOf("existing@example.com"))
        )
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrence(occurrence)
                .withUpdateMeetingResult(CREATED_MEETING_ID, UpdateMeetingUseCase.Result.Success)
        )
        assertTrue(viewModel.readyState.isGuestLinkSharingEnabled)
        assertEquals(setOf("existing@example.com"), viewModel.readyState.confirmedEmails)
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedEmail(false, "existing@example.com")
        viewModel.updateSelectedEmail(true, "new@example.com")
        viewModel.confirmSelectedContacts()
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.submitUpdate()
        advanceUntilIdle()
        coVerify { arrangement.updateMeeting(CREATED_MEETING_ID, match { it.invitedEmails == listOf("new@example.com") }) }
    }

    @Test
    fun givenExistingMeeting_whenOnlyParticipantsChange_thenConfirmIsEnabledAndChangesAreSubmitted() = runTest(dispatcher) {
        val existingContact = contact("existing")
        val replacement = contact("replacement")
        val existingUser = TestUser.OTHER_USER.copy(id = UserId(existingContact.id, existingContact.domain))
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrence(MEETING_OCCURRENCE)
                .withConversationMembers(CONVERSATION_ID, listOf(MemberDetails(existingUser, Conversation.Member.Role.Member)))
                .withMappedContact(existingUser, existingContact)
                .withUpdateMeetingResult(CREATED_MEETING_ID, UpdateMeetingUseCase.Result.Success)
        )

        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedContact(false, existingContact)
        viewModel.updateSelectedContact(true, replacement)
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.confirmSelectedContacts()

        assertTrue(viewModel.readyState.continueButtonEnabled)
        assertEquals(MEETING_OCCURRENCE.meeting.title, viewModel.titleTextState.text.toString())
        assertEquals(MEETING_OCCURRENCE.occurrenceStartTime, viewModel.readyState.startTime)
        assertEquals(MEETING_OCCURRENCE.occurrenceEndTime, viewModel.readyState.endTime)
        viewModel.submitUpdate()
        advanceUntilIdle()
        coVerify {
            arrangement.updateMeeting(
                CREATED_MEETING_ID,
                match { it.otherParticipants == listOf(UserId(replacement.id, replacement.domain)) }
            )
        }
    }

    @Test
    fun givenExistingMeetingWithInvalidTitle_whenParticipantsChange_thenConfirmRemainsDisabled() = runTest(dispatcher) {
        val occurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(title = "a".repeat(NewMeetingViewModel.MEETING_NAME_MAX_COUNT + 1))
        )
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrence(occurrence)
        )

        viewModel.updateSelectedContact(true, CONTACT)
        viewModel.confirmSelectedContacts()

        assertFalse(viewModel.readyState.continueButtonEnabled)
        assertEquals(NewMeetingState.TitleError.TitleExceedsLimitError, viewModel.readyState.titleError)
    }

    @Test
    fun givenExistingMeeting_whenOnlyGroupSelectionChanges_thenConfirmIsEnabled() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrence(MEETING_OCCURRENCE)
        )
        val group = SearchGroup(ConversationId("group", "wire.com"), "Group", setOf(CONTACT))

        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedGroup(true, group)
        viewModel.confirmSelectedContacts()

        assertTrue(viewModel.readyState.continueButtonEnabled)
        assertEquals(setOf(CONTACT), viewModel.readyState.confirmedContacts)
    }

    @Test
    fun givenUnchangedLoadedMeeting_whenConfirmingSelectionOrSubmitting_thenConfirmIsDisabledAndNoUpdateIsSent() = runTest(dispatcher) {
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrence(MEETING_OCCURRENCE)
        )
        advanceUntilIdle()

        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.confirmSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.submitUpdate()
        advanceUntilIdle()

        assertFalse(viewModel.readyState.continueButtonEnabled)
        coVerify(exactly = 0) { arrangement.updateMeeting(any(), any()) }
    }

    @Test
    fun givenLoadedMeeting_whenTitleTimesOrRecurrenceChangeAndRevert_thenConfirmTracksActualChanges() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrence(MEETING_OCCURRENCE)
        )
        val originalTitle = MEETING_OCCURRENCE.meeting.title
        val originalStart = MEETING_OCCURRENCE.occurrenceStartTime
        val originalEnd = MEETING_OCCURRENCE.occurrenceEndTime
        val originalRecurrence = MEETING_OCCURRENCE.meeting.recurrence?.toRepeatingInterval()

        enterTitle(viewModel, "$originalTitle changed")
        assertTrue(viewModel.readyState.continueButtonEnabled)
        enterTitle(viewModel, originalTitle)
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateStartTime(originalStart + 1.hours)
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.updateStartTime(originalStart)
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateEndTime(originalEnd + 1.hours)
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.updateEndTime(originalEnd)
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateRepeatingInterval(null)
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.updateRepeatingInterval(originalRecurrence)
        assertFalse(viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenLoadedMeeting_whenParticipantsAreChangedCancelledAndRestored_thenConfirmUsesCommittedParticipantIds() = runTest(dispatcher) {
        val existingUser = TestUser.OTHER_USER.copy(id = UserId(CONTACT.id, CONTACT.domain))
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrence(MEETING_OCCURRENCE)
                .withConversationMembers(CONVERSATION_ID, listOf(MemberDetails(existingUser, Conversation.Member.Role.Member)))
                .withMappedContact(existingUser, CONTACT)
        )
        val added = contact("added")
        val sameMembersGroup = SearchGroup(ConversationId("group", "wire.com"), "Group", setOf(CONTACT))

        viewModel.updateSelectedContact(true, added)
        viewModel.resetSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedGroup(true, sameMembersGroup)
        viewModel.confirmSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedContact(true, added)
        viewModel.confirmSelectedContacts()
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedContact(false, added)
        viewModel.confirmSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedContact(false, CONTACT)
        viewModel.confirmSelectedContacts()
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedContact(true, CONTACT.copy(name = "Updated display name"))
        viewModel.confirmSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenLoadedEmailInvitees_whenEmailsChangeAndRevert_thenConfirmIgnoresAddressOrderAndCase() = runTest(dispatcher) {
        val occurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(invitedEmails = listOf("first@example.com", "second@example.com"))
        )
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrence(occurrence)
        )

        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedEmail(true, "added@example.com")
        viewModel.resetSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedEmail(false, "first@example.com")
        viewModel.confirmSelectedContacts()
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedEmail(true, "FIRST@example.com")
        viewModel.confirmSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedEmail(true, "added@example.com")
        viewModel.confirmSelectedContacts()
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedEmail(false, "added@example.com")
        viewModel.confirmSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenParticipantOnlyChanges_whenEndTimeBecomesInvalid_thenConfirmRequiresValidTimesAndActualChanges() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
                .withNextUnfinishedMeetingOccurrence(MEETING_OCCURRENCE)
        )

        viewModel.updateSelectedContact(true, CONTACT)
        viewModel.confirmSelectedContacts()
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.updateEndTime(viewModel.readyState.startTime - 1.hours)
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateEndTime(MEETING_OCCURRENCE.occurrenceEndTime)
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedContact(false, CONTACT)
        viewModel.confirmSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenScheduledMeeting_whenCreating_thenConfirmedEmailsAreSubmitted() = runTest(dispatcher) {
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCreateMeetingResult(
                    CreateNewMeetingUseCase.Result.Success(meetingId = CREATED_MEETING_ID, conversationId = CONVERSATION_ID)
                )
        )
        viewModel.titleTextState.setTextAndPlaceCursorAtEnd("Email meeting")
        viewModel.updateSelectedEmail(true, "person@example.com")
        viewModel.confirmSelectedContacts()
        viewModel.submitCreation()
        advanceUntilIdle()
        coVerify { arrangement.createNewMeeting(match { it.invitedEmails == listOf("person@example.com") }) }
    }

    @Test
    fun givenCreationTypeAndCurrentTime_whenViewModelIsCreated_thenStateIsInitialized() = runTest(dispatcher) {
        withCreationTypes { type ->
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val (_, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(type)
                    .withCurrentTimeProvider { currentTime }
            )

            assertEquals(type, viewModel.type)
            assertEquals(
                if (type == NewMeetingType.MeetNow) Meeting.Type.IMMEDIATE else Meeting.Type.SCHEDULED,
                viewModel.readyState.meetingType
            )
            assertEquals(currentTime + 1.hours, viewModel.readyState.startTime) // next full hour
            assertEquals(currentTime + 2.hours, viewModel.readyState.endTime) // start time + 1 hour
            assertFalse(viewModel.readyState.continueButtonEnabled)
            assertNull(viewModel.readyState.titleError)
            assertNull(viewModel.readyState.startTimeError)
            assertNull(viewModel.readyState.endTimeError)
            assertEquals(MEETING_TIME_ZONE_ID, viewModel.readyState.tzid)
        }
    }

    @Test
    fun givenSelectedContacts_whenContactsAreConfirmedAndReset_thenStateKeepsConfirmedContacts() = runTest(dispatcher) {
        withCreationTypes { type ->
            val contact = contact("contact-1")
            val otherContact = contact("contact-2")
            val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(type))

            viewModel.updateSelectedContact(selected = true, contact = contact)
            viewModel.updateSelectedContact(selected = true, contact = otherContact)
            assertEquals(setOf(contact, otherContact), viewModel.readyState.selectedContacts.toSet())
            assertEquals(emptySet<Contact>(), viewModel.readyState.confirmedContacts.toSet())

            viewModel.confirmSelectedContacts()
            assertEquals(setOf(contact, otherContact), viewModel.readyState.confirmedContacts.toSet())

            viewModel.updateSelectedContact(selected = false, contact = otherContact)
            assertEquals(setOf(contact), viewModel.readyState.selectedContacts.toSet())

            viewModel.resetSelectedContacts()
            assertEquals(setOf(contact, otherContact), viewModel.readyState.selectedContacts.toSet())
        }
    }

    @Test
    fun givenOverlappingGroups_whenOneIsRemoved_thenMembersFromTheOtherRemain() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(type))
            val shared = contact("shared")
            val firstOnly = contact("first-only")
            val firstGroup = SearchGroup(ConversationId("first", "wire.com"), "First", setOf(shared, firstOnly))
            val secondGroup = SearchGroup(ConversationId("second", "wire.com"), "Second", setOf(shared))

            viewModel.updateSelectedGroup(true, firstGroup)
            viewModel.updateSelectedGroup(true, secondGroup)
            viewModel.updateSelectedGroup(false, firstGroup)

            assertEquals(setOf(shared), viewModel.readyState.selectedContacts.toSet())
            assertEquals(setOf(secondGroup), viewModel.readyState.selectedGroups.toSet())

            viewModel.confirmSelectedContacts()
            viewModel.updateSelectedGroup(false, secondGroup)
            viewModel.resetSelectedContacts()

            assertEquals(setOf(shared), viewModel.readyState.selectedContacts.toSet())
            assertEquals(setOf(secondGroup), viewModel.readyState.selectedGroups.toSet())
        }
    }

    @Test
    fun givenIndividualAndGroupSelection_whenResetAndGroupRemoved_thenIndividualSelectionRemains() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(type))
            val contact = contact("shared")
            val group = SearchGroup(ConversationId("group", "wire.com"), "Group", setOf(contact))

            viewModel.updateSelectedContact(true, contact)
            viewModel.updateSelectedGroup(true, group)
            viewModel.confirmSelectedContacts()
            viewModel.updateSelectedContact(false, contact)
            viewModel.updateSelectedGroup(false, group)
            viewModel.resetSelectedContacts()
            viewModel.updateSelectedGroup(false, group)

            assertEquals(setOf(contact), viewModel.readyState.selectedContacts.toSet())
            assertEquals(setOf(contact), viewModel.readyState.selectedIndividualContacts.toSet())
            assertEquals(emptySet<SearchGroup>(), viewModel.readyState.selectedGroups.toSet())
        }
    }

    @Test
    fun givenOverlappingSelectedGroups_whenMemberIsDeselected_thenOthersRemainAsIndividualSelections() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(type))
            val shared = contact("shared")
            val firstOnly = contact("first-only")
            val secondOnly = contact("second-only")
            val unrelated = contact("unrelated")
            val individual = contact("individual")
            val firstGroup = SearchGroup(ConversationId("first", "wire.com"), "First", setOf(shared, firstOnly))
            val secondGroup = SearchGroup(ConversationId("second", "wire.com"), "Second", setOf(shared, secondOnly))
            val unrelatedGroup = SearchGroup(ConversationId("unrelated", "wire.com"), "Unrelated", setOf(unrelated))
            viewModel.updateSelectedContact(true, individual)
            viewModel.updateSelectedContact(true, shared)
            listOf(firstGroup, secondGroup, unrelatedGroup).forEach { viewModel.updateSelectedGroup(true, it) }

            // The picker may hold newer contact details for the same person.
            viewModel.updateSelectedContact(false, shared.copy(name = "Updated name"))

            assertEquals(setOf(individual, firstOnly, secondOnly, unrelated), viewModel.readyState.selectedContacts.toSet())
            assertEquals(setOf(individual, firstOnly, secondOnly), viewModel.readyState.selectedIndividualContacts.toSet())
            assertEquals(setOf(unrelatedGroup), viewModel.readyState.selectedGroups.toSet())
        }
    }

    @Test
    fun givenGroupMemberDeselected_whenSelectionIsResetOrConfirmed_thenGroupAndContactsFollowTheConfirmation() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(type))
            val removed = contact("removed")
            val remaining = contact("remaining")
            val group = SearchGroup(ConversationId("group", "wire.com"), "Group", setOf(removed, remaining))
            viewModel.updateSelectedGroup(true, group)
            viewModel.confirmSelectedContacts()

            viewModel.updateSelectedContact(false, removed)
            viewModel.resetSelectedContacts()
            assertEquals(setOf(group), viewModel.readyState.selectedGroups.toSet())
            assertEquals(setOf(removed, remaining), viewModel.readyState.selectedContacts.toSet())

            viewModel.updateSelectedContact(false, removed)
            viewModel.confirmSelectedContacts()
            viewModel.updateSelectedGroup(true, group)
            viewModel.resetSelectedContacts()
            assertEquals(emptySet<SearchGroup>(), viewModel.readyState.selectedGroups.toSet())
            assertEquals(setOf(remaining), viewModel.readyState.selectedContacts.toSet())
            assertEquals(setOf(remaining), viewModel.readyState.confirmedContacts.toSet())
        }
    }

    @Test
    fun givenRepeatingInterval_whenIntervalIsUpdated_thenStateIsUpdated() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(NewMeetingType.Schedule))

        viewModel.updateRepeatingInterval(MeetingItem.RepeatingInterval.Supported.first())

        assertEquals(MeetingItem.RepeatingInterval.Supported.first(), viewModel.readyState.repeatingInterval)
    }

    @Test
    fun givenInitialEmptyTitle_whenViewModelIsCreated_thenTitleErrorIsNotShownAndContinueIsDisabled() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(type))

            assertNull(viewModel.readyState.titleError)
            assertFalse(viewModel.readyState.continueButtonEnabled)
        }
    }

    @Test
    fun givenValidTitle_whenTitleChanges_thenContinueIsEnabled() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(type))

            enterTitle(viewModel, "Weekly sync")

            assertNull(viewModel.readyState.titleError)
            assertEquals(true, viewModel.readyState.continueButtonEnabled)
        }
    }

    @Test
    fun givenTitleIsClearedAfterInput_whenTitleChanges_thenEmptyTitleErrorIsShown() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(type))

            enterTitle(viewModel, "Weekly sync")
            enterTitle(viewModel, "")

            assertEquals(NewMeetingState.TitleError.TitleEmptyError, viewModel.readyState.titleError)
            assertFalse(viewModel.readyState.continueButtonEnabled)
        }
    }

    @Test
    fun givenTitleExceedsLimit_whenTitleChanges_thenTitleExceedsLimitErrorIsShown() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(type))

            enterTitle(viewModel, "a".repeat(NewMeetingViewModel.MEETING_NAME_MAX_COUNT + 1))

            assertEquals(NewMeetingState.TitleError.TitleExceedsLimitError, viewModel.readyState.titleError)
            assertFalse(viewModel.readyState.continueButtonEnabled)
        }
    }

    @Test
    fun givenStartTimeInPast_whenStartTimeChanges_thenStartTimeInPastErrorIsShown() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
        )

        enterTitle(viewModel, "Weekly sync")
        viewModel.updateStartTime(currentTime - 1.hours)
        advanceUntilIdle()

        assertEquals(NewMeetingState.TimeError.StartTimeInPastError, viewModel.readyState.startTimeError)
        assertFalse(viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenStartTimeChanges_whenEndTimeFitsSameDay_thenEndTimeKeepsCurrentDuration() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val newStartTime = currentTime + 3.hours
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
        )

        viewModel.updateStartTime(newStartTime)

        assertEquals(newStartTime, viewModel.readyState.startTime)
        assertEquals(newStartTime + 1.hours, viewModel.readyState.endTime)
    }

    @Test
    fun givenStartTimeChanges_whenEndTimeWouldMoveToNextDay_thenEndTimeIsCappedAt2359() = runTest(dispatcher) {
        val timeZone = TimeZone.of(MEETING_TIME_ZONE_ID)
        val currentTime = LocalDateTime(2026, 1, 1, 12, 0).toInstant(timeZone)
        val newStartTime = LocalDateTime(2026, 1, 1, 23, 30).toInstant(timeZone)
        val latestEndTime = LocalDateTime(2026, 1, 1, 23, 59).toInstant(timeZone)
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
        )

        viewModel.updateStartTime(newStartTime)

        assertEquals(newStartTime, viewModel.readyState.startTime)
        assertEquals(latestEndTime, viewModel.readyState.endTime)
        assertNull(viewModel.readyState.endTimeError)
    }

    @Test
    fun givenEndTimeInPast_whenEndTimeChanges_thenEndTimeInPastErrorIsShown() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
        )

        enterTitle(viewModel, "Weekly sync")
        viewModel.updateEndTime(currentTime - 1.hours)
        advanceUntilIdle()

        assertEquals(NewMeetingState.TimeError.EndTimeInPastError, viewModel.readyState.endTimeError)
        assertFalse(viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenScheduleTypeWithStartAndEndTimesInPast_whenSubmitCreationIsCalled_thenPastTimeErrorsAreShownAndMeetingIsNotCreated() =
        runTest(dispatcher) {
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(NewMeetingType.Schedule)
                    .withCurrentTimeProvider { currentTime }
            )

            enterTitle(viewModel, "Weekly sync")
            viewModel.updateStartTime(currentTime - 3.hours)
            viewModel.updateEndTime(currentTime - 2.hours)

            viewModel.actions.test {
                viewModel.submitCreation()
                advanceUntilIdle()

                coVerify(exactly = 0) { arrangement.createNewMeeting(any()) }
                coVerify(exactly = 0) { arrangement.updateMeeting(any(), any()) }
                expectNoEvents()
                assertEquals(NewMeetingState.TimeError.StartTimeInPastError, viewModel.readyState.startTimeError)
                assertEquals(NewMeetingState.TimeError.EndTimeInPastError, viewModel.readyState.endTimeError)
                assertFalse(viewModel.readyState.continueButtonEnabled)
            }
        }

    @Test
    fun givenEndTimeBeforeStartTime_whenEndTimeChanges_thenEndTimeBeforeStartTimeErrorIsShown() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
        )

        enterTitle(viewModel, "Weekly sync")
        viewModel.updateStartTime(currentTime + 2.hours)
        viewModel.updateEndTime(currentTime + 1.hours)
        advanceUntilIdle()

        assertEquals(NewMeetingState.TimeError.EndTimeBeforeStartTimeError, viewModel.readyState.endTimeError)
        assertFalse(viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenInvalidTimesBecomeValid_whenTimesChange_thenErrorsAreClearedAndContinueIsEnabled() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
        )

        enterTitle(viewModel, "Weekly sync")
        viewModel.updateStartTime(currentTime + 2.hours)
        viewModel.updateEndTime(currentTime + 1.hours)
        advanceUntilIdle()
        viewModel.updateEndTime(currentTime + 3.hours)
        advanceUntilIdle()

        assertNull(viewModel.readyState.startTimeError)
        assertNull(viewModel.readyState.endTimeError)
        assertEquals(true, viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenMeetNowTypeWithValidData_whenSubmitCreationIsCalled_thenSuccessRequestsCallInCreatedConversation() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val createdConversationId = ConversationId("created-meet-now-conversation", "domain")
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.MeetNow)
                .withCurrentTimeProvider { currentTime }
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(createdConversationId, CREATED_MEETING_ID))
        )

        enterTitle(viewModel, "  Quick sync  ")

        viewModel.actions.test {
            viewModel.submitCreation()
            advanceUntilIdle()

            coVerify(exactly = 1) {
                arrangement.createNewMeeting(
                    CreateMeeting(
                        title = "Quick sync",
                        startTime = currentTime,
                        endTime = currentTime + 1.hours,
                        tzid = MEETING_TIME_ZONE_ID,
                        recurrence = null,
                        type = Meeting.Type.IMMEDIATE,
                        otherParticipants = emptyList()
                    )
                )
            }
            assertEquals(currentTime, viewModel.readyState.startTime)
            assertEquals(currentTime + 1.hours, viewModel.readyState.endTime)
            assertFalse(viewModel.readyState.isSubmitting)
            assertNull(viewModel.readyState.submitError)
            assertEquals(NewMeetingViewActions.Success(NewMeetingResult.MeetNow(CREATED_MEETING_ID, createdConversationId)), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun givenScheduleTypeWithValidData_whenSubmitCreationIsCalled_thenSuccessDoesNotRequestCall() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val createMeeting = CREATE_MEETING.copy(startTime = currentTime + 2.hours, endTime = currentTime + 3.hours)
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
        )
        enterTitle(viewModel, createMeeting.title)
        viewModel.updateStartTime(createMeeting.startTime)
        viewModel.updateEndTime(createMeeting.endTime)
        val removed = contact("removed")
        val group = SearchGroup(ConversationId("group", "wire.com"), "Group", setOf(CONTACT, removed))
        viewModel.updateSelectedGroup(true, group)
        viewModel.updateSelectedContact(false, removed)
        viewModel.confirmSelectedContacts()
        viewModel.updateRepeatingInterval(createMeeting.recurrence?.toRepeatingInterval())

        viewModel.actions.test {
            viewModel.submitCreation()
            advanceUntilIdle()
            coVerify(exactly = 1) { arrangement.createNewMeeting(createMeeting) }
            assertFalse(viewModel.readyState.isSubmitting)
            assertNull(viewModel.readyState.submitError)
            assertEquals(NewMeetingViewActions.Success(NewMeetingResult.Scheduled(CREATED_MEETING_ID)), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun givenScheduledMeeting_whenCreated_thenResultCarriesMeetingId() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
        )
        enterTitle(viewModel, "Planning")
        viewModel.actions.test {
            viewModel.submitCreation()
            advanceUntilIdle()
            assertEquals(
                NewMeetingViewActions.Success(NewMeetingResult.Scheduled(CREATED_MEETING_ID)),
                awaitItem()
            )
            expectNoEvents()
        }
    }

    @Test
    fun givenGuestLinksDisabled_whenCreated_thenConfirmationIsStillEmitted() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withMeetingsGuestsLinksEnabled(false)
                .withGuestLinkSharingEnabled(false)
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
        )
        enterTitle(viewModel, "Planning")
        viewModel.actions.test {
            viewModel.submitCreation()
            advanceUntilIdle()
            assertEquals(NewMeetingViewActions.Success(NewMeetingResult.Scheduled(CREATED_MEETING_ID)), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun givenCreationFails_whenSubmitCreationIsCalled_thenErrorIsShownAndSuccessActionIsNotSent() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(type)
                    .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Failure)
            )

            enterTitle(viewModel, "Weekly sync")

            viewModel.actions.test {
                viewModel.submitCreation()
                advanceUntilIdle()

                coVerify(exactly = 1) { arrangement.createNewMeeting(any()) }
                expectNoEvents()
                assertFalse(viewModel.readyState.isSubmitting)
                assertEquals(true, viewModel.readyState.continueButtonEnabled)
                assertEquals(NewMeetingState.SubmitError.Other, viewModel.readyState.submitError)
            }
        }
    }

    @Test
    fun givenConfirmedParticipants_whenSubmittingCreation_thenOnlyConfirmedContactsArePassedWithoutDuplicates() = runTest(dispatcher) {
        withCreationTypes { type ->
            val shared = contact("shared")
            val other = contact("other")
            val removed = contact("removed")
            val unconfirmed = contact("unconfirmed")
            val group = SearchGroup(ConversationId("group", "wire.com"), "Group", setOf(shared, other, removed))
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(type)
                    .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
            )
            enterTitle(viewModel, "  Weekly sync  ")
            viewModel.updateSelectedContact(true, shared)
            viewModel.updateSelectedGroup(true, group)
            viewModel.updateSelectedContact(false, removed)
            viewModel.confirmSelectedContacts()
            viewModel.updateSelectedContact(true, unconfirmed)

            viewModel.submitCreation()
            advanceUntilIdle()

            coVerify(exactly = 1) {
                arrangement.createNewMeeting(
                    match {
                        it.title == "Weekly sync" && it.otherParticipants.size == 2 &&
                                it.otherParticipants.toSet() == setOf(UserId(shared.id, shared.domain), UserId(other.id, other.domain))
                    }
                )
            }
        }
    }

    @Test
    fun givenEditTypeWithValidData_whenSubmitUpdateIsCalled_thenMeetingIsEditedAndSuccessActionIsSent() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val contact = contact("contact-1")
        val updateMeeting = UPDATE_MEETING.copy(startTime = currentTime + 2.hours, endTime = currentTime + 3.hours)
        val editType = NewMeetingType.Edit(CREATED_MEETING_ID)
        val nextOccurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(
                startTime = currentTime + 1.hours,
                endTime = currentTime + 2.hours,
                recurrence = Meeting.Recurrence(frequency = Meeting.Recurrence.Frequency.DAILY, interval = 1L, until = null),
            ),
            occurrenceStartTime = currentTime + 1.hours,
            occurrenceEndTime = currentTime + 2.hours,
        )
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withNextUnfinishedMeetingOccurrence(nextOccurrence)
                .withUpdateMeetingResult(nextOccurrence.meeting.meetingId, UpdateMeetingUseCase.Result.Success)
                .withCurrentTimeProvider { currentTime }
        )

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("ignored password")
        enterTitle(viewModel, updateMeeting.title)
        viewModel.updateStartTime(updateMeeting.startTime)
        viewModel.updateEndTime(updateMeeting.endTime)
        viewModel.updateSelectedContact(selected = true, contact = contact)
        viewModel.confirmSelectedContacts()
        viewModel.updateRepeatingInterval(updateMeeting.recurrence?.toRepeatingInterval())

        viewModel.actions.test {
            viewModel.submitUpdate()
            advanceUntilIdle()
            coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, updateMeeting) }
            verify(exactly = 0) { arrangement.validatePassword(any()) }
            assertFalse(viewModel.readyState.isSubmitting)
            assertNull(viewModel.readyState.submitError)
            assertEquals(NewMeetingViewActions.Success(NewMeetingResult.Updated), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenImmediateMeeting_whenSubmitUpdateIsCalled_thenTypeIsPreserved() = runTest(dispatcher) {
        val editType = NewMeetingType.Edit(MEETING_OCCURRENCE.meeting.meetingId)
        val immediateOccurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(type = Meeting.Type.IMMEDIATE)
        )
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withNextUnfinishedMeetingOccurrence(immediateOccurrence)
                .withUpdateMeetingResult(editType.id, UpdateMeetingUseCase.Result.Success)
        )

        advanceUntilIdle()
        assertEquals(Meeting.Type.IMMEDIATE, viewModel.readyState.meetingType)
        enterTitle(viewModel, "Updated immediate meeting")

        viewModel.submitUpdate()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.updateMeeting(editType.id, match { it.type == Meeting.Type.IMMEDIATE })
        }
    }

    @Test
    fun givenEditTypeWithStartAndEndTimesInPast_whenSubmitUpdateIsCalled_thenPastTimesAreAllowedAndMeetingIsEdited() =
        runTest(dispatcher) {
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val pastStartTime = currentTime - 3.hours
            val pastEndTime = currentTime - 2.hours
            val editType = NewMeetingType.Edit(CREATED_MEETING_ID)
            val nextOccurrence = MEETING_OCCURRENCE.copy(
                meeting = MEETING_OCCURRENCE.meeting.copy(
                    startTime = currentTime + 1.hours,
                    endTime = currentTime + 2.hours,
                    recurrence = null,
                ),
                occurrenceStartTime = currentTime + 1.hours,
                occurrenceEndTime = currentTime + 2.hours,
            )
            val updateMeeting = UpdateMeeting(
                title = "Retrospective sync",
                startTime = pastStartTime,
                endTime = pastEndTime,
                tzid = MEETING_TIME_ZONE_ID,
                recurrence = null,
                otherParticipants = emptyList(),
                type = Meeting.Type.SCHEDULED,
            )
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(editType)
                    .withCurrentTimeProvider { currentTime }
                    .withNextUnfinishedMeetingOccurrence(nextOccurrence)
                    .withUpdateMeetingResult(nextOccurrence.meeting.meetingId, UpdateMeetingUseCase.Result.Success)
            )

            enterTitle(viewModel, updateMeeting.title)
            viewModel.updateStartTime(pastStartTime)
            viewModel.updateEndTime(pastEndTime)

            viewModel.actions.test {
                viewModel.submitUpdate()
                advanceUntilIdle()

                coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, updateMeeting) }
                assertNull(viewModel.readyState.startTimeError)
                assertNull(viewModel.readyState.endTimeError)
                assertFalse(viewModel.readyState.isSubmitting)
                assertNull(viewModel.readyState.submitError)
                assertEquals(NewMeetingViewActions.Success(NewMeetingResult.Updated), awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun givenEditionFails_whenSubmitUpdateIsCalled_thenErrorIsShownAndSuccessActionIsNotSent() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val editType = NewMeetingType.Edit(CREATED_MEETING_ID)
        val nextOccurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(
                startTime = currentTime + 1.hours,
                endTime = currentTime + 2.hours,
                recurrence = Meeting.Recurrence(frequency = Meeting.Recurrence.Frequency.DAILY, interval = 1L, until = null),
            ),
            occurrenceStartTime = currentTime + 1.hours,
            occurrenceEndTime = currentTime + 2.hours,
        )
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withNextUnfinishedMeetingOccurrence(nextOccurrence)
                .withUpdateMeetingResult(nextOccurrence.meeting.meetingId, UpdateMeetingUseCase.Result.Failure.Other)
        )

        enterTitle(viewModel, "Weekly sync")

        viewModel.actions.test {
            viewModel.submitUpdate()
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, any()) }
            expectNoEvents()
            assertFalse(viewModel.readyState.isSubmitting)
            assertEquals(true, viewModel.readyState.continueButtonEnabled)
            assertEquals(NewMeetingState.SubmitError.Other, viewModel.readyState.submitError)
        }
    }

    @Test
    fun givenConversationNameEditionFails_whenSubmitUpdateIsCalled_thenErrorIsShownAndSuccessActionIsNotSent() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val editType = NewMeetingType.Edit(CREATED_MEETING_ID)
        val nextOccurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(
                startTime = currentTime + 1.hours,
                endTime = currentTime + 2.hours,
                recurrence = Meeting.Recurrence(frequency = Meeting.Recurrence.Frequency.DAILY, interval = 1L, until = null),
            ),
            occurrenceStartTime = currentTime + 1.hours,
            occurrenceEndTime = currentTime + 2.hours,
        )
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withNextUnfinishedMeetingOccurrence(nextOccurrence)
                .withUpdateMeetingResult(
                    nextOccurrence.meeting.meetingId,
                    UpdateMeetingUseCase.Result.Failure.UpdateConversationNameFailure(nextOccurrence.meeting.conversationId)
                )
        )

        enterTitle(viewModel, "Weekly sync")

        viewModel.actions.test {
            viewModel.submitUpdate()
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, any()) }
            expectNoEvents()
            assertFalse(viewModel.readyState.isSubmitting)
            assertEquals(true, viewModel.readyState.continueButtonEnabled)
            assertEquals(
                NewMeetingState.SubmitError.UpdateConversationNameFailure(nextOccurrence.meeting.conversationId),
                viewModel.readyState.submitError
            )
        }
    }

    @Test
    fun givenUpdateActionReturnsUpdateConversationNameFailure_whenSubmitUpdateIsCalled_thenReturnedConversationErrorIsShown() =
        runTest(dispatcher) {
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val editType = NewMeetingType.Edit(CREATED_MEETING_ID)
            val nextOccurrence = MEETING_OCCURRENCE.copy(
                meeting = MEETING_OCCURRENCE.meeting.copy(
                    startTime = currentTime + 1.hours,
                    endTime = currentTime + 2.hours,
                    recurrence = Meeting.Recurrence(frequency = Meeting.Recurrence.Frequency.DAILY, interval = 1L, until = null),
                ),
                occurrenceStartTime = currentTime + 1.hours,
                occurrenceEndTime = currentTime + 2.hours,
            )
            val failedConversationId = ConversationId("failed-conversation-id", "domain")
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(editType)
                    .withNextUnfinishedMeetingOccurrence(nextOccurrence)
                    .withUpdateMeetingResult(
                        nextOccurrence.meeting.meetingId,
                        UpdateMeetingUseCase.Result.Failure.UpdateConversationNameFailure(failedConversationId)
                    )
            )

            enterTitle(viewModel, "Weekly sync")

            viewModel.actions.test {
                viewModel.submitUpdate()
                advanceUntilIdle()

                coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, any()) }
                expectNoEvents()
                assertFalse(viewModel.readyState.isSubmitting)
                assertEquals(true, viewModel.readyState.continueButtonEnabled)
                assertEquals(
                    NewMeetingState.SubmitError.UpdateConversationNameFailure(failedConversationId),
                    viewModel.readyState.submitError
                )
            }
        }

    @Test
    fun givenRetryUpdateConversationNameSucceeds_whenRetryUpdateConversationNameIsCalled_thenSuccessActionIsSent() = runTest(dispatcher) {
        val conversationId = ConversationId("conversation-id", "domain")
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withRenameConversationResult(conversationId, "Weekly sync", RenamingResult.Success)
        )

        enterTitle(viewModel, "  Weekly sync  ")

        viewModel.actions.test {
            viewModel.retryUpdateConversationName(conversationId)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                arrangement.renameConversationUseCase(
                    conversationId = conversationId,
                    conversationName = "Weekly sync"
                )
            }
            assertNull(viewModel.readyState.submitError)
            assertEquals(NewMeetingViewActions.Success(NewMeetingResult.Updated), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenRetryUpdateConversationNameFails_whenRetryUpdateConversationNameIsCalled_thenErrorIsShownAndSuccessActionIsNotSent() =
        runTest(dispatcher) {
            val conversationId = ConversationId("conversation-id", "domain")
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withRenameConversationResult(
                        conversationId = conversationId,
                        conversationName = "Weekly sync",
                        result = RenamingResult.Failure(CoreFailure.Unknown(RuntimeException("Failed to rename conversation")))
                    )
            )

            enterTitle(viewModel, "  Weekly sync  ")

            viewModel.actions.test {
                viewModel.retryUpdateConversationName(conversationId)
                advanceUntilIdle()

                coVerify(exactly = 1) {
                    arrangement.renameConversationUseCase(
                        conversationId = conversationId,
                        conversationName = "Weekly sync"
                    )
                }
                expectNoEvents()
                assertEquals(NewMeetingState.SubmitError.UpdateConversationNameFailure(conversationId), viewModel.readyState.submitError)
            }
        }

    @Test
    fun givenInvalidTitle_whenSubmitCreationIsCalled_thenTitleErrorIsShownAndSuccessActionIsNotSent() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (arrangement, viewModel) = arrangeViewModel(Arrangement(dispatcher).withNewMeetingType(type))

            viewModel.actions.test {
                viewModel.submitCreation()
                advanceUntilIdle()

                coVerify(exactly = 0) { arrangement.createNewMeeting(any()) }
                coVerify(exactly = 0) { arrangement.updateMeeting(any(), any()) }
                expectNoEvents()
                assertEquals(NewMeetingState.TitleError.TitleEmptyError, viewModel.readyState.titleError)
                assertFalse(viewModel.readyState.continueButtonEnabled)
            }
        }
    }

    @Test
    fun givenScheduleTypeWithInvalidTimes_whenCreateMeetingIsCalled_thenTimeErrorIsShownAndMeetingIsNotCreated() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
        )

        enterTitle(viewModel, "Weekly sync")
        viewModel.updateStartTime(currentTime - 1.hours)

        viewModel.actions.test {
            viewModel.submitCreation()
            advanceUntilIdle()

            coVerify(exactly = 0) { arrangement.createNewMeeting(any()) }
            coVerify(exactly = 0) { arrangement.updateMeeting(any(), any()) }
            expectNoEvents()
            assertEquals(NewMeetingState.TimeError.StartTimeInPastError, viewModel.readyState.startTimeError)
            assertFalse(viewModel.readyState.continueButtonEnabled)
        }
    }

    @Test
    fun givenEditTypeAndNextOccurrenceExists_whenViewModelIsCreated_thenStateIsInitializedFromNextOccurrence() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val nextOccurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(
                startTime = currentTime + 1.hours,
                endTime = currentTime + 2.hours,
                recurrence = Meeting.Recurrence(frequency = Meeting.Recurrence.Frequency.DAILY, interval = 1L, until = null),
            ),
            occurrenceStartTime = currentTime + 1.hours,
            occurrenceEndTime = currentTime + 2.hours,
        )
        val editType = NewMeetingType.Edit(nextOccurrence.meeting.meetingId)
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withCurrentTimeProvider { currentTime }
                .withNextUnfinishedMeetingOccurrence(nextOccurrence)
        )

        coVerify(exactly = 1) { arrangement.getNextUnfinishedMeetingOccurrence(editType.id, currentTime) }
        assertEquals(editType, viewModel.type)
        assertEquals(nextOccurrence.meeting.title, viewModel.titleTextState.text.toString())
        assertEquals(nextOccurrence.occurrenceStartTime, viewModel.readyState.startTime)
        assertEquals(nextOccurrence.occurrenceEndTime, viewModel.readyState.endTime)
        assertEquals(nextOccurrence.meeting.recurrence?.frequency, viewModel.readyState.repeatingInterval?.frequency)
        assertEquals(nextOccurrence.meeting.recurrence?.interval?.toInt(), viewModel.readyState.repeatingInterval?.interval)
        assertTrue(viewModel.state is NewMeetingState.Ready)
    }

    @Suppress("UnusedFlow")
    @Test
    fun givenEditTypeAndNextOccurrenceExists_whenViewModelIsCreated_thenParticipantsAreInitializedFromOccurrenceConversation() =
        runTest(dispatcher) {
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val editType = NewMeetingType.Edit(MEETING_OCCURRENCE.meeting.meetingId)
            val firstUser = TestUser.OTHER_USER.copy(id = UserId("contact-1", "domain"))
            val secondUser = TestUser.OTHER_USER.copy(id = UserId("contact-2", "domain"))
            val firstContact = contact("contact-1")
            val secondContact = contact("contact-2")
            val conversationMembers = listOf(
                MemberDetails(firstUser, Conversation.Member.Role.Member),
                MemberDetails(secondUser, Conversation.Member.Role.Admin),
            )
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(editType)
                    .withCurrentTimeProvider { currentTime }
                    .withNextUnfinishedMeetingOccurrence(MEETING_OCCURRENCE)
                    .withConversationMembers(MEETING_OCCURRENCE.meeting.conversationId, conversationMembers)
                    .withMappedContact(firstUser, firstContact)
                    .withMappedContact(secondUser, secondContact)
            )

            coVerify(exactly = 1) { arrangement.observeConversationMembers(MEETING_OCCURRENCE.meeting.conversationId) }
            assertEquals(setOf(firstContact, secondContact), viewModel.readyState.selectedContacts.toSet())
            assertEquals(setOf(firstContact, secondContact), viewModel.readyState.confirmedContacts.toSet())
            assertTrue(viewModel.state is NewMeetingState.Ready)
        }

    @Test
    fun givenEditTypeAndNextOccurrenceDoesNotExist_whenViewModelIsCreated_thenStateStopsLoadingWithInitialTimes() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val editType = NewMeetingType.Edit(CREATED_MEETING_ID)
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withCurrentTimeProvider { currentTime }
                .withNextUnfinishedMeetingOccurrence(null)
        )

        coVerify(exactly = 1) { arrangement.getNextUnfinishedMeetingOccurrence(editType.id, currentTime) }
        assertEquals(editType, viewModel.type)
        assertEquals("", viewModel.titleTextState.text.toString())
        assertEquals(currentTime + 1.hours, viewModel.readyState.startTime)
        assertEquals(currentTime + 2.hours, viewModel.readyState.endTime)
        assertTrue(viewModel.state is NewMeetingState.Ready)
    }

    @Test
    fun givenEditTypeAndNextOccurrenceLoadFails_whenViewModelIsCreated_thenInitialLoadingStateIsError() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val editType = NewMeetingType.Edit(CREATED_MEETING_ID)
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withCurrentTimeProvider { currentTime }
                .withNextUnfinishedMeetingOccurrenceFailure()
        )

        coVerify(exactly = 1) { arrangement.getNextUnfinishedMeetingOccurrence(editType.id, currentTime) }
        assertEquals(NewMeetingState.Error, viewModel.state)
    }

    @Test
    fun givenLocalTimeZone_whenSubmitCreationIsCalled_thenCurrentLocalTzidIsPassed() = runTest(dispatcher) {
        withCreationTypes { type ->
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val localTimeZoneID = "Europe/Berlin"
            val createMeeting = CREATE_MEETING.copy(
                startTime = if (type == NewMeetingType.MeetNow) currentTime else currentTime + 2.hours,
                endTime = if (type == NewMeetingType.MeetNow) currentTime + 1.hours else currentTime + 3.hours,
                tzid = localTimeZoneID,
                recurrence = null,
                type = if (type == NewMeetingType.MeetNow) Meeting.Type.IMMEDIATE else Meeting.Type.SCHEDULED,
                otherParticipants = emptyList()
            )
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(type)
                    .withCurrentTimeProvider { currentTime }
                    .withCurrentTimeZone(localTimeZoneID)
                    .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
            )
            enterTitle(viewModel, createMeeting.title)
            viewModel.updateStartTime(createMeeting.startTime)
            viewModel.updateEndTime(createMeeting.endTime)

            viewModel.actions.test {
                viewModel.submitCreation()
                advanceUntilIdle()

                coVerify(exactly = 1) { arrangement.createNewMeeting(createMeeting) }
                val expectedResult = when (type) {
                    NewMeetingType.MeetNow -> NewMeetingResult.MeetNow(CREATED_MEETING_ID, CONVERSATION_ID)
                    else -> NewMeetingResult.Scheduled(CREATED_MEETING_ID)
                }
                assertEquals(NewMeetingViewActions.Success(expectedResult), awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    @Test
    fun givenEditTypeAndMeetingInDifferentTimeZone_whenViewModelIsCreated_thenStateRetainsOriginalMeetingTzid() =
        runTest(dispatcher) {
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val meetingTimeZoneId = "America/New_York"
            val localTimeZoneID = "Europe/Berlin"
            val nextOccurrence = MEETING_OCCURRENCE.copy(meeting = MEETING_OCCURRENCE.meeting.copy(tzid = meetingTimeZoneId))
            val editType = NewMeetingType.Edit(nextOccurrence.meeting.meetingId)
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(editType)
                    .withCurrentTimeProvider { currentTime }
                    .withCurrentTimeZone(localTimeZoneID)
                    .withNextUnfinishedMeetingOccurrence(nextOccurrence)
            )

            coVerify(exactly = 1) { arrangement.getNextUnfinishedMeetingOccurrence(editType.id, currentTime) }
            assertEquals(meetingTimeZoneId, viewModel.readyState.tzid)
            assertTrue(viewModel.state is NewMeetingState.Ready)
        }

    @Test
    fun givenEditTypeAndMeetingInDifferentTimeZone_whenSubmitUpdateWithoutTimeChanges_thenOriginalMeetingTzidIsPassed() =
        runTest(dispatcher) {
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val meetingTimeZoneId = "America/New_York"
            val localTimeZoneID = "Europe/Berlin"
            val nextOccurrence = MEETING_OCCURRENCE.copy(meeting = MEETING_OCCURRENCE.meeting.copy(tzid = meetingTimeZoneId))
            val editType = NewMeetingType.Edit(nextOccurrence.meeting.meetingId)
            val updateMeeting = UPDATE_MEETING.copy(
                title = "Updated meeting",
                startTime = nextOccurrence.occurrenceStartTime,
                endTime = nextOccurrence.occurrenceEndTime,
                tzid = meetingTimeZoneId, // keep the original tzid since no time changes are made
                otherParticipants = emptyList()
            )
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(editType)
                    .withCurrentTimeProvider { currentTime }
                    .withCurrentTimeZone(localTimeZoneID)
                    .withNextUnfinishedMeetingOccurrence(nextOccurrence)
                    .withUpdateMeetingResult(nextOccurrence.meeting.meetingId, UpdateMeetingUseCase.Result.Success)
            )

            // only update the title, keep the original start and end times, so no time changes are made and no tzid change is expected
            enterTitle(viewModel, updateMeeting.title)

            viewModel.actions.test {
                viewModel.submitUpdate()
                advanceUntilIdle()

                coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, updateMeeting) }
                assertEquals(NewMeetingViewActions.Success(NewMeetingResult.Updated), awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun givenEditTypeAndMeetingInDifferentTimeZone_whenSubmitUpdateWithStartTimeChanges_thenOriginalMeetingTzidIsPassed() =
        runTest(dispatcher) {
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val meetingTimeZoneId = "America/New_York"
            val localTimeZoneID = "Europe/Berlin"
            val updatedStartTime = currentTime + 3.hours
            val nextOccurrence = MEETING_OCCURRENCE.copy(meeting = MEETING_OCCURRENCE.meeting.copy(tzid = meetingTimeZoneId))
            val editType = NewMeetingType.Edit(nextOccurrence.meeting.meetingId)
            val updateMeeting = UPDATE_MEETING.copy(
                startTime = updatedStartTime,
                endTime = updatedStartTime + 1.hours, // it's recalculated based on the original duration of the meeting
                tzid = meetingTimeZoneId, // keep the original tzid
                otherParticipants = emptyList()
            )
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(editType)
                    .withCurrentTimeProvider { currentTime }
                    .withCurrentTimeZone(localTimeZoneID)
                    .withNextUnfinishedMeetingOccurrence(nextOccurrence)
                    .withUpdateMeetingResult(nextOccurrence.meeting.meetingId, UpdateMeetingUseCase.Result.Success)
            )

            viewModel.updateStartTime(updatedStartTime)

            viewModel.actions.test {
                viewModel.submitUpdate()
                advanceUntilIdle()

                coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, updateMeeting) }
                assertEquals(NewMeetingViewActions.Success(NewMeetingResult.Updated), awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun givenEditTypeAndMeetingInDifferentTimeZone_whenSubmitUpdateWithEndTimeChanges_thenOriginalMeetingTzidIsPassed() =
        runTest(dispatcher) {
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val meetingTimeZoneId = "America/New_York"
            val localTimeZoneID = "Europe/Berlin"
            val nextOccurrence = MEETING_OCCURRENCE.copy(meeting = MEETING_OCCURRENCE.meeting.copy(tzid = meetingTimeZoneId))
            val updatedEndTime = nextOccurrence.occurrenceStartTime + 2.hours
            val editType = NewMeetingType.Edit(nextOccurrence.meeting.meetingId)
            val updateMeeting = UPDATE_MEETING.copy(
                startTime = nextOccurrence.occurrenceStartTime,
                endTime = updatedEndTime,
                tzid = meetingTimeZoneId, // keep the original tzid
                otherParticipants = emptyList()
            )
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(editType)
                    .withCurrentTimeProvider { currentTime }
                    .withCurrentTimeZone(localTimeZoneID)
                    .withNextUnfinishedMeetingOccurrence(nextOccurrence)
                    .withUpdateMeetingResult(nextOccurrence.meeting.meetingId, UpdateMeetingUseCase.Result.Success)
            )

            viewModel.updateEndTime(updatedEndTime)

            viewModel.actions.test {
                viewModel.submitUpdate()
                advanceUntilIdle()

                coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, updateMeeting) }
                assertEquals(NewMeetingViewActions.Success(NewMeetingResult.Updated), awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun givenEmptyPassword_whenSubmitting_thenValidationIsSkippedAndNullIsPassed() = runTest(dispatcher) {
        withCreationTypes { type ->
            val expectedType = if (type == NewMeetingType.MeetNow) Meeting.Type.IMMEDIATE else Meeting.Type.SCHEDULED
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(type)
                    .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
            )
            enterTitle(viewModel, "Weekly sync")

            viewModel.submitCreation()
            advanceUntilIdle()

            verify(exactly = 0) { arrangement.validatePassword(any()) }
            coVerify(exactly = 1) { arrangement.createNewMeeting(match { it.password == null && it.type == expectedType }) }
            assertFalse(viewModel.readyState.hasPasswordError)
        }
    }

    @Test
    fun givenValidPasswordWithMatchingConfirmation_whenSubmitting_thenExactPasswordIsPassed() = runTest(dispatcher) {
        withCreationTypes { type ->
            val password = " ValidPassword1! "
            val expectedType = if (type == NewMeetingType.MeetNow) Meeting.Type.IMMEDIATE else Meeting.Type.SCHEDULED
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withPasswordValidationResult(password, ValidatePasswordResult.Valid)
                    .withNewMeetingType(type)
                    .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
            )
            enterTitle(viewModel, "Weekly sync")
            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
            viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd(password)
            advanceUntilIdle()

            viewModel.submitCreation()
            advanceUntilIdle()

            verify(exactly = 1) { arrangement.validatePassword(password) }
            coVerify(exactly = 1) { arrangement.createNewMeeting(match { it.password == password && it.type == expectedType }) }
            assertEquals(password, viewModel.confirmPasswordTextState.text.toString())
            assertFalse(viewModel.readyState.hasPasswordError)
        }
    }

    @Test
    fun givenInvalidPassword_whenSubmitting_thenCreationIsBlocked() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withPasswordValidationResult("invalid", ValidatePasswordResult.Invalid())
                    .withNewMeetingType(type)
            )
            enterTitle(viewModel, "Weekly sync")
            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("invalid")
            viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("invalid")
            advanceUntilIdle()

            viewModel.submitCreation()
            advanceUntilIdle()
            enterTitle(viewModel, "Updated title")
            viewModel.updateEndTime(viewModel.readyState.endTime + 1.hours)

            assertEquals(true, viewModel.readyState.invalidPassword)
            assertFalse(viewModel.readyState.passwordsDoNotMatch)
            assertFalse(viewModel.readyState.continueButtonEnabled)
            coVerify(exactly = 0) { arrangement.createNewMeeting(any()) }
        }
    }

    @Test
    fun givenInvalidPassword_whenCleared_thenErrorIsClearedAndMeetingCanBeCreatedWithoutPassword() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withPasswordValidationResult("invalid", ValidatePasswordResult.Invalid())
                    .withNewMeetingType(type)
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
            )
            enterTitle(viewModel, "Weekly sync")
            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("invalid")
            advanceUntilIdle()
            viewModel.submitCreation()
            advanceUntilIdle()

            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("")
            advanceUntilIdle()

            assertFalse(viewModel.readyState.invalidPassword)
            assertEquals(true, viewModel.readyState.continueButtonEnabled)
            viewModel.submitCreation()
            advanceUntilIdle()
            verify(exactly = 1) { arrangement.validatePassword(any()) }
            coVerify(exactly = 1) { arrangement.createNewMeeting(match { it.password == null }) }
        }
    }

    @Test
    fun givenGuestLinksEnabled_whenGeneratingPassword_thenBothFieldsUseGeneratedPasswordForCreation() = runTest(dispatcher) {
        withCreationTypes { type ->
            val password = "Abc123!@"
            val expectedType = if (type == NewMeetingType.MeetNow) Meeting.Type.IMMEDIATE else Meeting.Type.SCHEDULED
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withGeneratedPassword(password)
                    .withPasswordValidationResult(password, ValidatePasswordResult.Valid)
                    .withNewMeetingType(type)
                .withPasswordValidationResult(password, ValidatePasswordResult.Valid)
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
            )
            enterTitle(viewModel, "Weekly sync")

            viewModel.onGenerateRandomPassword()
            advanceUntilIdle()
            assertEquals(password, viewModel.passwordTextState.text.toString())
            assertEquals(password, viewModel.confirmPasswordTextState.text.toString())
            viewModel.submitCreation()
            advanceUntilIdle()

            verify(exactly = 1) { arrangement.generatePassword(minLength = 8, maxLength = 8) }
            verify(exactly = 1) { arrangement.validatePassword(password) }
            coVerify(exactly = 1) { arrangement.createNewMeeting(match { it.password == password && it.type == expectedType }) }
            assertFalse(viewModel.readyState.hasPasswordError)
        }
    }

    @Test
    fun givenInvalidPassword_whenEdited_thenErrorsClearAndPasswordIsRevalidatedOnSubmission() = runTest(dispatcher) {
        withCreationTypes { type ->
            val password = "ValidPassword1!"
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withPasswordValidationResult("invalid", ValidatePasswordResult.Invalid())
                    .withPasswordValidationResult("still invalid", ValidatePasswordResult.Invalid())
                    .withPasswordValidationResult(password, ValidatePasswordResult.Valid)
                    .withNewMeetingType(type)
                    .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
            )
            enterTitle(viewModel, "Quick sync")
            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("invalid")
            viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("invalid")
            advanceUntilIdle()

            viewModel.submitCreation()
            advanceUntilIdle()
            assertEquals(true, viewModel.readyState.invalidPassword)
            assertFalse(viewModel.readyState.continueButtonEnabled)

            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("still invalid")
            advanceUntilIdle()

            assertFalse(viewModel.readyState.hasPasswordError)
            assertEquals(true, viewModel.readyState.continueButtonEnabled)
            verify(exactly = 1) { arrangement.validatePassword(any()) }
            viewModel.submitCreation()
            advanceUntilIdle()
            assertEquals(true, viewModel.readyState.invalidPassword)
            assertFalse(viewModel.readyState.continueButtonEnabled)
            coVerify(exactly = 0) { arrangement.createNewMeeting(any()) }

            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
            viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd(password)
            advanceUntilIdle()

            assertFalse(viewModel.readyState.hasPasswordError)
            assertEquals(true, viewModel.readyState.continueButtonEnabled)
            viewModel.submitCreation()
            advanceUntilIdle()
            coVerify(exactly = 1) { arrangement.createNewMeeting(match { it.password == password }) }
        }
    }

    @Test
    fun givenEditType_whenGeneratingPassword_thenPasswordIsNotGenerated() = runTest(dispatcher) {
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher).withNewMeetingType(NewMeetingType.Edit(MeetingId("meeting", "domain")))
        )

        viewModel.onGenerateRandomPassword()

        verify(exactly = 0) { arrangement.generatePassword(any(), any()) }
        assertEquals("", viewModel.passwordTextState.text.toString())
    }

    @Test
    fun givenGuestLinksDisabled_whenSubmittingNewMeeting_thenPasswordIsIgnoredAndNotGenerated() = runTest(dispatcher) {
        withCreationTypes { type ->
            listOf(false to true, true to false).forEach { (buildFlag, guestLinkSupport) ->
                val (arrangement, viewModel) = arrangeViewModel(
                    Arrangement(dispatcher)
                        .withNewMeetingType(type)
                        .withMeetingsGuestsLinksEnabled(buildFlag)
                        .withGuestLinkSharingEnabled(guestLinkSupport)
                        .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
                )
                enterTitle(viewModel, "Weekly sync")
                viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("hidden password")
                advanceUntilIdle()

                viewModel.onGenerateRandomPassword()
                viewModel.submitCreation()
                advanceUntilIdle()

                assertFalse(viewModel.readyState.isGuestLinkSharingEnabled)
                assertFalse(viewModel.readyState.invalidPassword)
                verify(exactly = 0) { arrangement.generatePassword(any(), any()) }
                verify(exactly = 0) { arrangement.validatePassword(any()) }
                coVerify(exactly = 1) { arrangement.createNewMeeting(match { it.password == null }) }
            }
        }
    }

    @Test
    fun givenGuestLinksEnabledInitially_whenSubmitting_thenAvailabilityIsNotCheckedAgain() = runTest(dispatcher) {
        withCreationTypes { type ->
            val password = "ValidPassword1!"
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withPasswordValidationResult(password, ValidatePasswordResult.Valid)
                    .withNewMeetingType(type)
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
            )
            assertEquals(true, viewModel.readyState.isGuestLinkSharingEnabled)
            assertTrue(viewModel.state is NewMeetingState.Ready)
            arrangement.withGuestLinkSharingEnabled(false)
            enterTitle(viewModel, "Weekly sync")
            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
            viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd(password)
            advanceUntilIdle()

            viewModel.submitCreation()
            advanceUntilIdle()

            assertEquals(true, viewModel.readyState.isGuestLinkSharingEnabled)
            coVerify(exactly = 1) { arrangement.isMeetingsEnabledUseCase.isGuestLinkSharingEnabled() }
            coVerify(exactly = 1) { arrangement.createNewMeeting(match { it.password == password }) }
        }
    }

    @Test
    fun givenMissingOrMismatchedConfirmation_whenSubmitting_thenCreationIsBlocked() = runTest(dispatcher) {
        withCreationTypes { type ->
            val password = "ValidPassword1!"
            listOf("", "DifferentPassword1!").forEach { confirmation ->
                val (arrangement, viewModel) = arrangeViewModel(
                    Arrangement(dispatcher)
                        .withPasswordValidationResult(password, ValidatePasswordResult.Valid)
                        .withNewMeetingType(type)
                )
                enterTitle(viewModel, "Weekly sync")
                viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
                viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd(confirmation)
                advanceUntilIdle()

                viewModel.submitCreation()
                advanceUntilIdle()
                enterTitle(viewModel, "Updated title")
                viewModel.updateEndTime(viewModel.readyState.endTime + 1.hours)

                assertEquals(true, viewModel.readyState.passwordsDoNotMatch)
                assertFalse(viewModel.readyState.invalidPassword)
                assertFalse(viewModel.readyState.continueButtonEnabled)
                coVerify(exactly = 0) { arrangement.createNewMeeting(any()) }
            }
        }
    }

    @Test
    fun givenMismatchedConfirmation_whenCorrected_thenErrorClearsAndCreationSucceeds() = runTest(dispatcher) {
        withCreationTypes { type ->
            val password = "ValidPassword1!"
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withPasswordValidationResult(password, ValidatePasswordResult.Valid)
                    .withNewMeetingType(type)
                    .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
            )
            enterTitle(viewModel, "Weekly sync")
            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
            advanceUntilIdle()
            viewModel.submitCreation()
            advanceUntilIdle()
            assertEquals(true, viewModel.readyState.passwordsDoNotMatch)

            viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd(password)
            advanceUntilIdle()

            assertFalse(viewModel.readyState.passwordsDoNotMatch)
            assertEquals(true, viewModel.readyState.continueButtonEnabled)
            viewModel.submitCreation()
            advanceUntilIdle()
            coVerify(exactly = 1) { arrangement.createNewMeeting(match { it.password == password }) }
        }
    }

    @Test
    fun givenOnlyConfirmationEntered_whenSubmitting_thenPasswordRemainsOptional() = runTest(dispatcher) {
        withCreationTypes { type ->
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(type)
                    .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success(CONVERSATION_ID, CREATED_MEETING_ID))
            )
            enterTitle(viewModel, "Weekly sync")
            viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("IgnoredPassword1!")
            advanceUntilIdle()

            viewModel.submitCreation()
            advanceUntilIdle()

            assertFalse(viewModel.readyState.hasPasswordError)
            verify(exactly = 0) { arrangement.validatePassword(any()) }
            coVerify(exactly = 1) { arrangement.createNewMeeting(match { it.password == null }) }
        }
    }

    @Test
    fun givenEditInProgress_whenParticipantsChange_thenConfirmStaysDisabledUntilRequestFinishes() = runTest(dispatcher) {
        val updateResult = CompletableDeferred<UpdateMeetingUseCase.Result>()
        val arrangement = Arrangement(dispatcher)
            .withNewMeetingType(NewMeetingType.Edit(CREATED_MEETING_ID))
            .withNextUnfinishedMeetingOccurrence(MEETING_OCCURRENCE)
        coEvery { arrangement.updateMeeting(CREATED_MEETING_ID, any()) } coAnswers { updateResult.await() }
        val (_, viewModel) = arrangeViewModel(arrangement)
        viewModel.updateSelectedEmail(true, "added@example.com")
        viewModel.confirmSelectedContacts()

        viewModel.submitUpdate()
        runCurrent()
        assertTrue(viewModel.readyState.isSubmitting)
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedContact(true, CONTACT)
        viewModel.confirmSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
        viewModel.submitUpdate()
        runCurrent()
        coVerify(exactly = 1) { arrangement.updateMeeting(CREATED_MEETING_ID, any()) }

        updateResult.complete(UpdateMeetingUseCase.Result.Failure.Other)
        runCurrent()
        assertFalse(viewModel.readyState.isSubmitting)
        assertTrue(viewModel.readyState.continueButtonEnabled)
        viewModel.updateSelectedEmail(false, "added@example.com")
        viewModel.updateSelectedContact(false, CONTACT)
        viewModel.confirmSelectedContacts()
        assertFalse(viewModel.readyState.continueButtonEnabled)
    }

    @Test
    fun givenCreationInProgress_whenFieldsChange_thenContinueStaysDisabledUntilRequestFinishes() = runTest(dispatcher) {
        withCreationTypes { type ->
            val creationResult = CompletableDeferred<CreateNewMeetingUseCase.Result>()
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withPendingCreateMeetingResult(creationResult)
                    .withNewMeetingType(type)
            )
            enterTitle(viewModel, "Quick sync")

            viewModel.submitCreation()
            runCurrent()

            assertEquals(true, viewModel.readyState.isSubmitting)
            assertFalse(viewModel.readyState.continueButtonEnabled)
            enterTitle(viewModel, "Updated title")
            assertFalse(viewModel.readyState.continueButtonEnabled)
            viewModel.updateStartTime(viewModel.readyState.startTime + 1.hours)
            assertFalse(viewModel.readyState.continueButtonEnabled)
            viewModel.updateEndTime(viewModel.readyState.endTime + 1.hours)
            assertFalse(viewModel.readyState.continueButtonEnabled)
            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("ValidPassword1!")
            runCurrent()
            assertFalse(viewModel.readyState.continueButtonEnabled)
            viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("ValidPassword1!")
            runCurrent()
            assertFalse(viewModel.readyState.continueButtonEnabled)

            viewModel.submitCreation()
            runCurrent()

            coVerify(exactly = 1) { arrangement.createNewMeeting(any()) }
            creationResult.complete(CreateNewMeetingUseCase.Result.Failure)
            runCurrent()

            assertFalse(viewModel.readyState.isSubmitting)
            assertEquals(NewMeetingState.SubmitError.Other, viewModel.readyState.submitError)
            assertEquals(true, viewModel.readyState.continueButtonEnabled)
        }
    }

    @Test
    fun givenCreationInProgress_whenUnrelatedFieldsChange_thenTitleAndTimeErrorsSurviveFailureUntilCorrected() = runTest(dispatcher) {
        withCreationTypes { type ->
            val creationResult = CompletableDeferred<CreateNewMeetingUseCase.Result>()
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withPendingCreateMeetingResult(creationResult)
                    .withNewMeetingType(type)
            )
            enterTitle(viewModel, "Weekly sync")
            viewModel.submitCreation()
            runCurrent()

            assertEquals(true, viewModel.readyState.isSubmitting)
            viewModel.updateStartTime(viewModel.readyState.startTime + 1.hours)

            enterTitle(viewModel, "x".repeat(NewMeetingViewModel.MEETING_NAME_MAX_COUNT + 1))
            viewModel.updateEndTime(viewModel.readyState.startTime + 2.hours)
            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("ValidPassword1!")
            viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("ValidPassword1!")
            runCurrent()

            assertEquals(NewMeetingState.TitleError.TitleExceedsLimitError, viewModel.readyState.titleError)
            assertFalse(viewModel.readyState.continueButtonEnabled)
            viewModel.updateEndTime(viewModel.readyState.startTime - 1.minutes)
            enterTitle(viewModel, "Updated title")
            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("DifferentPassword1!")
            viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("DifferentPassword1!")
            runCurrent()

            assertNull(viewModel.readyState.titleError)
            assertEquals(NewMeetingState.TimeError.EndTimeBeforeStartTimeError, viewModel.readyState.endTimeError)
            assertFalse(viewModel.readyState.continueButtonEnabled)
            creationResult.complete(CreateNewMeetingUseCase.Result.Failure)
            runCurrent()

            assertFalse(viewModel.readyState.isSubmitting)
            assertEquals(NewMeetingState.SubmitError.Other, viewModel.readyState.submitError)
            assertEquals(NewMeetingState.TimeError.EndTimeBeforeStartTimeError, viewModel.readyState.endTimeError)
            assertFalse(viewModel.readyState.continueButtonEnabled)
            viewModel.updateEndTime(viewModel.readyState.startTime + 1.hours)

            assertNull(viewModel.readyState.endTimeError)
            assertEquals(true, viewModel.readyState.continueButtonEnabled)
        }
    }

    private val NewMeetingViewModel.readyState: NewMeetingState.Ready
        get() = state as NewMeetingState.Ready

    private inline fun withCreationTypes(block: (NewMeetingType) -> Unit) =
        listOf(NewMeetingType.MeetNow, NewMeetingType.Schedule).forEach(block)

    private fun TestScope.arrangeViewModel(
        arrangement: Arrangement = Arrangement(dispatcher)
    ): Pair<Arrangement, NewMeetingViewModelImpl> =
        arrangement.arrange().also { runCurrent() }

    private fun TestScope.enterTitle(viewModel: NewMeetingViewModel, title: String) {
        viewModel.titleTextState.setTextAndPlaceCursorAtEnd(title)
        advanceUntilIdle()
    }

    private fun contact(id: String) = Contact(
        id = id,
        domain = "wire.com",
        name = "Contact $id",
        handle = id,
        membership = Membership.Standard,
        connectionState = ConnectionState.ACCEPTED,
    )

    private inner class Arrangement(private val dispatcher: TestDispatcher) {
        var currentTimeProvider = CurrentTimeProvider {
            Instant.fromEpochMilliseconds(dispatcher.scheduler.currentTime)
        }

        @MockK
        lateinit var currentTimeZoneProvider: CurrentTimeZoneProvider

        @MockK
        lateinit var createNewMeeting: CreateNewMeetingUseCase

        @MockK
        lateinit var updateMeeting: UpdateMeetingUseCase

        @MockK
        lateinit var getNextUnfinishedMeetingOccurrence: GetNextUnfinishedMeetingOccurrenceUseCase

        @MockK
        lateinit var observeConversationMembers: ObserveConversationMembersUseCase

        @MockK
        lateinit var renameConversationUseCase: RenameConversationUseCase

        @MockK
        lateinit var contactMapper: ContactMapper

        @MockK
        lateinit var validatePassword: ValidatePasswordUseCase

        @MockK
        lateinit var validateEmail: ValidateEmailUseCase

        @MockK
        lateinit var generatePassword: RandomPassword

        @MockK
        lateinit var isMeetingsEnabledUseCase: IsMeetingsEnabledUseCase

        private var meetingsGuestsLinksEnabled = true

        private var newMeetingType: NewMeetingType = NewMeetingType.MeetNow

        init {
            MockKAnnotations.init(this)
            every { validateEmail(any()) } returns true
            every { validateEmail("invalid") } returns false
            coEvery { isMeetingsEnabledUseCase.isGuestLinkSharingEnabled() } returns true
            every { currentTimeZoneProvider.invoke() } returns TimeZone.of(MEETING_OCCURRENCE.meeting.tzid)
            coEvery { getNextUnfinishedMeetingOccurrence(any(), any()) } returns null
            coEvery { observeConversationMembers(any()) } returns flowOf(emptyList())
        }

        fun withGuestLinkSharingEnabled(enabled: Boolean) = apply {
            coEvery { isMeetingsEnabledUseCase.isGuestLinkSharingEnabled() } returns enabled
        }

        fun withMeetingsGuestsLinksEnabled(enabled: Boolean) = apply {
            meetingsGuestsLinksEnabled = enabled
        }

        fun withNewMeetingType(type: NewMeetingType) = apply {
            newMeetingType = type
        }

        fun withGeneratedPassword(password: String) = apply {
            every { generatePassword(minLength = any(), maxLength = any()) } returns password
        }

        fun withPasswordValidationResult(password: String, result: ValidatePasswordResult) = apply {
            every { validatePassword(password) } returns result
        }

        fun withCurrentTimeProvider(currentTime: () -> Instant) = apply {
            currentTimeProvider = CurrentTimeProvider(currentTime)
        }

        fun withCurrentTimeZone(timeZoneId: String) = apply {
            every { currentTimeZoneProvider.invoke() } returns TimeZone.of(timeZoneId)
        }

        fun withCreateMeetingResult(result: CreateNewMeetingUseCase.Result) = apply {
            coEvery { createNewMeeting(any()) } returns result
        }

        fun withPendingCreateMeetingResult(result: CompletableDeferred<CreateNewMeetingUseCase.Result>) = apply {
            coEvery { createNewMeeting(any()) } coAnswers { result.await() }
        }

        fun withUpdateMeetingResult(meetingId: MeetingId, result: UpdateMeetingUseCase.Result) = apply {
            coEvery { updateMeeting(meetingId, any()) } returns result
        }

        fun withRenameConversationResult(conversationId: ConversationId, conversationName: String, result: RenamingResult) = apply {
            coEvery { renameConversationUseCase(conversationId, conversationName) } returns result
        }

        fun withNextUnfinishedMeetingOccurrence(nextMeetingOccurrence: MeetingOccurrence?) = apply {
            coEvery { getNextUnfinishedMeetingOccurrence(any(), any()) } returns nextMeetingOccurrence
        }

        fun withNextUnfinishedMeetingOccurrenceFailure() = apply {
            coEvery {
                getNextUnfinishedMeetingOccurrence(any(), any())
            } throws IllegalStateException("Failed to load meeting occurrence")
        }

        fun withConversationMembers(conversationId: ConversationId, members: List<MemberDetails>) = apply {
            coEvery { observeConversationMembers(conversationId) } returns flowOf(members)
        }

        fun withMappedContact(otherUser: OtherUser, contact: Contact) = apply {
            every { contactMapper.fromOtherUser(otherUser) } returns contact
        }

        fun arrange() = this to NewMeetingViewModelImpl(
            navArgs = NewMeetingNavArgs(type = newMeetingType),
            currentTimeProvider = currentTimeProvider,
            currentTimeZoneProvider = currentTimeZoneProvider,
            createNewMeeting = createNewMeeting,
            updateMeeting = updateMeeting,
            getNextUnfinishedMeetingOccurrence = getNextUnfinishedMeetingOccurrence,
            observeConversationMembers = observeConversationMembers,
            renameConversationUseCase = renameConversationUseCase,
            contactMapper = contactMapper,
            validatePassword = validatePassword,
            validateEmail = validateEmail,
            generatePassword = generatePassword,
            isMeetingsEnabledUseCase = isMeetingsEnabledUseCase,
            meetingsGuestsLinksEnabled = meetingsGuestsLinksEnabled,
        )
    }

    private val CREATED_MEETING_ID = MeetingId("meeting-id", "domain")
    val CONVERSATION_ID = ConversationId("conversation-id", "domain")
    private val MEETING_TIME_ZONE_ID = "Europe/Berlin"
    private val MEETING_OCCURRENCE = MeetingOccurrence(
        participants = emptyList(),
        meeting = Meeting(
            meetingId = CREATED_MEETING_ID,
            conversationId = CONVERSATION_ID,
            creatorId = UserId("creator-id", "domain"),
            title = "Repeating meeting",
            startTime = Instant.parse("2026-01-01T09:00:00Z"),
            endTime = Instant.parse("2026-01-01T10:00:00Z"),
            tzid = MEETING_TIME_ZONE_ID,
            recurrence = Meeting.Recurrence(
                frequency = MeetingItem.RepeatingInterval.Supported.first().frequency,
                interval = MeetingItem.RepeatingInterval.Supported.first().interval.toLong(),
                until = null
            ),
        ),
        selfRole = MeetingOccurrence.SelfRole.Creator,
        conversationName = "Daily",
        conversationType = MeetingOccurrence.ConversationType.Group,
        occurrenceId = "occurrence-id",
        occurrenceStartTime = Instant.parse("2026-01-02T09:00:00Z"),
        occurrenceEndTime = Instant.parse("2026-01-02T10:00:00Z"),
    )
    private val CONTACT = contact("contact-1")
    private val CREATE_MEETING = CreateMeeting(
        title = MEETING_OCCURRENCE.meeting.title,
        startTime = MEETING_OCCURRENCE.meeting.startTime,
        endTime = MEETING_OCCURRENCE.meeting.endTime,
        tzid = MEETING_TIME_ZONE_ID,
        recurrence = MEETING_OCCURRENCE.meeting.recurrence,
        otherParticipants = listOf(UserId(CONTACT.id, CONTACT.domain))
    )
    private val UPDATE_MEETING = UpdateMeeting(
        title = CREATE_MEETING.title,
        startTime = CREATE_MEETING.startTime,
        endTime = CREATE_MEETING.endTime,
        tzid = CREATE_MEETING.tzid,
        recurrence = CREATE_MEETING.recurrence,
        otherParticipants = CREATE_MEETING.otherParticipants,
        type = Meeting.Type.SCHEDULED,
    )
}
