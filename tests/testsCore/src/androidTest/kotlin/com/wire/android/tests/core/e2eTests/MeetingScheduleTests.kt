/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.tests.core.e2eTests

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import backendUtils.meeting.deleteMeeting
import backendUtils.meeting.getMeeting
import backendUtils.meeting.getMeetingConversation
import backendUtils.meeting.listMeetings
import backendUtils.team.TeamRoles
import backendUtils.team.updateUniqueUsername
import kotlinx.coroutines.runBlocking
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import service.models.Meeting
import uiautomatorutils.UiWaitUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Suppress("TooManyFunctions", "MagicNumber")
class MeetingScheduleTests : BaseMeetingUiTest() {
    override val deletePersonalUsersAfterTest = true
    private val form get() = pages.scheduleMeetingPage
    private var fixtureReady = false
    private lateinit var today: LocalDate
    private lateinit var defaultStart: LocalDateTime

    @Before
    fun openMeetingsForScheduling() {
        fixtureReady = true
        // A fixed local morning avoids midnight and next-hour races in the default-value assertions.
        today = LocalDate.now(zone).plusDays(1)
        settings.setTime(today.atTime(9, 17).atZone(zone).toInstant())
        settings.set24HourFormat(true)
        defaultStart = today.atTime(10, 0)
        pages.meetingsPage.open()
    }

    @After
    fun removeMeetingsCreatedThroughUi() {
        if (!fixtureReady) return
        // Restore connectivity before backend cleanup even when the offline assertion failed.
        settings.restoreNetwork()
        backendClient.listMeetings(member).forEach { backendClient.deleteMeeting(it) }
    }

    @TestCaseId("TC-12056", "TC-12064")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenLocalMorning_whenSchedulingValidMeeting_thenDefaultsAndSavedDetailsAreCorrect() {
        step("Open the form, check every field and next-full-hour default with a one-hour duration") {
            form.open()
            form.assertFields()
            form.assertDateTimes(defaultStart, defaultStart.plusHours(1))
            form.assertScheduleEnabled(false)
        }
        step("Schedule a meeting and verify one saved series and one correctly grouped card") {
            form.enterTitle("ML-Schedule-valid")
            val meeting = submitAndVerify("ML-Schedule-valid", defaultStart, defaultStart.plusHours(1))
            pages.meetingsPage.assertCompleteList(listOf(meeting), zone, today)
        }
    }

    @TestCaseId("TC-12058")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenRequiredTitle_whenEnteringBoundaryValues_thenOnlyUpTo64CharactersAreAccepted() {
        val maximumTitle = "ML-" + "a".repeat(61)
        step("Empty title cannot be scheduled; exactly 64 characters are accepted") {
            form.open()
            form.assertScheduleEnabled(false)
            form.enterTitle(maximumTitle)
            form.assertTitle(maximumTitle)
            form.assertScheduleEnabled(true)
        }
        step("Attempt 65 characters and verify the accepted title remains within the limit") {
            form.enterTitle(maximumTitle + "b")
            form.assertTitle(maximumTitle)
            form.assertScheduleEnabled(true)
            form.enterTitle("")
            form.assertScheduleEnabled(false)
            form.enterTitle(maximumTitle)
            submitAndVerify(maximumTitle, defaultStart, defaultStart.plusHours(1))
        }
    }

    @TestCaseId("TC-12060")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenEmptyAndModifiedDrafts_whenClosingOrPressingBack_thenDiscardIsConfirmedOnlyForChanges() {
        step("Close an unchanged draft immediately without a discard dialog or saved meeting") {
            form.open()
            form.close()
            form.awaitClosed()
            assertNoSavedMeetings()
            pages.meetingsPage.assertEmpty()
        }
        listOf(false, true).forEach { useBack ->
            step("Modify a draft, cancel discard, then confirm discard using ${if (useBack) "Android Back" else "Close"}") {
                form.open()
                form.enterTitle("ML-Schedule-discard")
                form.close(useBack)
                form.assertDiscardConfirmation()
                form.cancelDiscard()
                form.assertTitle("ML-Schedule-discard")
                form.assertDateTimes(defaultStart, defaultStart.plusHours(1))
                form.close(useBack)
                form.assertDiscardConfirmation()
                form.confirmDiscard()
                form.awaitClosed()
                assertNoSavedMeetings()
                pages.meetingsPage.assertEmpty()
            }
        }
    }

    @TestCaseId("TC-12063")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenValidDraftOffline_whenSchedulingThenRetryingOnline_thenErrorPreservesDraftAndCreatesOnlyOneMeeting() {
        form.open()
        form.enterTitle("ML-Schedule-retry")
        step("Disconnect Wi-Fi and cellular connectivity and attempt scheduling") {
            settings.disconnectNetwork()
            form.schedule()
            form.assertSchedulingError()
            form.assertTitle("ML-Schedule-retry")
            form.assertDateTimes(defaultStart, defaultStart.plusHours(1))
        }
        step("Restore connectivity and verify the failed attempt did not create a meeting") {
            settings.restoreNetwork()
            assertNoSavedMeetings()
        }
        step("Retry and verify a single saved meeting remains after relaunch and synchronization") {
            val meeting = submitAndVerify("ML-Schedule-retry", defaultStart, defaultStart.plusHours(1))
            pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
            pages.meetingsPage.assertCompleteList(listOf(meeting), zone, today)
            val duplicateAppeared = UiWaitUtils.retryUntilTimeout(10.seconds, 1.seconds) {
                backendClient.listMeetings(member).count { it.title == meeting.title } > 1
            }
            assertFalse("A failed attempt created a duplicate after connectivity returned", duplicateAppeared)
            assertEquals(listOf(meeting.id), backendClient.listMeetings(member).map { it.id })
        }
    }

    @TestCaseId("TC-12065")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenCustomDuration_whenChangingDateAndStartTime_thenEndDateTracksStartAndDurationIsPreserved() {
        val selectedDate = today.plusDays(1)
        step("Set a 90-minute duration and change the start date") {
            form.open()
            form.enterTitle("ML-Schedule-changed")
            form.selectEndTime(LocalTime.of(11, 30))
            form.selectDate(selectedDate)
            form.assertDateTimes(selectedDate.atTime(10, 0), selectedDate.atTime(11, 30))
        }
        step("Move the start time, preserve 90 minutes and save the selected values") {
            form.selectStartTime(LocalTime.of(13, 15))
            val start = selectedDate.atTime(13, 15)
            val end = selectedDate.atTime(14, 45)
            form.assertDateTimes(start, end)
            submitAndVerify("ML-Schedule-changed", start, end)
        }
    }

    @TestCaseId("TC-12067")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenSameDayMeeting_whenStartingLate_thenEndIsClampedTo2359AndZeroDurationIsRejected() {
        step("Preserve the normal duration when it fits; clamp 23:30 to a 23:59 end") {
            form.open()
            form.enterTitle("ML-Schedule-late")
            form.selectStartTime(LocalTime.of(22, 0))
            form.assertDateTimes(today.atTime(22, 0), today.atTime(23, 0))
            form.selectStartTime(LocalTime.of(23, 30))
            form.assertDateTimes(today.atTime(23, 30), today.atTime(23, 59))
            submitAndVerify("ML-Schedule-late", today.atTime(23, 30), today.atTime(23, 59))
        }
        step("A separate draft starting at 23:59 has zero duration and cannot be scheduled") {
            form.open()
            form.enterTitle("ML-Schedule-zero")
            form.selectStartTime(LocalTime.of(23, 59))
            form.assertDateTimes(today.atTime(23, 59), today.atTime(23, 59))
            if (form.isScheduleEnabled()) {
                form.schedule()
                form.assertSchedulingError()
            }
            assertEquals(listOf("ML-Schedule-late"), backendClient.listMeetings(member).map { it.title })
        }
    }

    @TestCaseId("TC-12068")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenSupportedRepeatOptions_whenSchedulingEach_thenSavedRecurrenceAndOccurrenceIntervalsAreCorrect() {
        val options = listOf(
            Triple("Never", "", 0),
            Triple("Daily", "daily", 1),
            Triple("Weekly", "weekly", 1),
            Triple("Every two weeks", "weekly", 2),
            Triple("Every four weeks", "weekly", 4)
        )
        options.forEachIndexed { index, (label, frequency, interval) ->
            step("Schedule using $label and verify the saved rule and occurrence sequence") {
                form.open()
                val title = "ML-Schedule-repeat-$index"
                form.enterTitle(title)
                form.selectRepeat(label)
                form.assertTitle(title)
                form.schedule()
                form.awaitClosed()
                val meeting = awaitSavedMeeting(title)
                val saved = backendClient.getMeeting(meeting, member)
                if (interval == 0) {
                    assertTrue("Never must save no recurrence", saved.isNull("recurrence"))
                    pages.meetingsPage.assertCompleteList(listOf(meeting), zone, today)
                } else {
                    val recurrence = saved.getJSONObject("recurrence")
                    assertEquals(frequency, recurrence.getString("frequency"))
                    assertEquals(interval, recurrence.getInt("interval"))
                    pages.meetingsPage.assertOccurrencePrefix(
                        meeting,
                        zone,
                        today,
                        label,
                        if (frequency == "daily") 1 else 7 * interval
                    )
                }
                backendClient.deleteMeeting(meeting)
                // Reopen the tab so the next independent series starts at the beginning of the list.
                pages.conversationListPage.clickConversationsMenuEntry()
                pages.conversationListPage.clickConversationsButtonOnMenuEntry()
                pages.meetingsPage.open()
                pages.meetingsPage.assertEmpty()
            }
        }
    }

    @TestCaseId("TC-12069")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenEligibleAndExternalUsers_whenSelectingAndRemovingParticipants_thenOnlyFinalEligibleSelectionIsScheduled() {
        backendSetupHelper.userXAddsUsersToTeam("user1Name", "user3Name", TEAM_NAME, TeamRoles.Member, backendClient, context, true)
        val second = clientUserManager.findUserByNameOrNameAlias("user3Name")
        startMeetingServiceDevice("user3Name", "ScheduleInvitee")
        val external = backendSetupHelper.createPersonalUser(backendClient = backendClient)
        runBlocking { backendClient.updateUniqueUsername(external, requireNotNull(external.uniqueUsername)) }
        step("Select two eligible members by name and username, including searching the same selected member twice") {
            form.open()
            form.enterTitle("ML-Schedule-participants")
            form.openParticipants()
            form.searchParticipant(requireNotNull(host.name))
            form.setParticipant(requireNotNull(host.name), true)
            form.searchParticipant(requireNotNull(second.uniqueUsername))
            form.setParticipant(requireNotNull(second.name), true)
            form.searchParticipant(requireNotNull(host.uniqueUsername))
            form.assertParticipantSelected(requireNotNull(host.name))
            form.setParticipant(requireNotNull(host.name), true)
            form.confirmParticipants()
            form.assertParticipants(listOf(requireNotNull(host.name), requireNotNull(second.name)))
        }
        step("Reopen selection, exclude an external account by full name and username, and remove only one member") {
            form.openParticipants()
            listOf(requireNotNull(external.name), requireNotNull(external.uniqueUsername)).forEach { query ->
                form.searchParticipant(query)
                pages.searchPage.assertNoSearchResultsVisible()
            }
            form.searchParticipant(requireNotNull(host.name))
            form.assertParticipantSelected(requireNotNull(host.name))
            form.setParticipant(requireNotNull(host.name), false)
            form.searchParticipant(requireNotNull(second.uniqueUsername))
            form.assertParticipantSelected(requireNotNull(second.name))
            form.confirmParticipants()
            form.assertParticipants(listOf(requireNotNull(second.name)))
        }
        step("Schedule and verify the saved conversation contains only the final invitee") {
            val meeting = submitAndVerify("ML-Schedule-participants", defaultStart, defaultStart.plusHours(1))
            val synchronized = UiWaitUtils.retryUntilTimeout(30.seconds) {
                val others = backendClient.getMeetingConversation(meeting).getJSONObject("members").getJSONArray("others")
                val ids = (0 until others.length()).map { others.getJSONObject(it).getJSONObject("qualified_id").getString("id") }
                ids == listOf(second.id)
            }
            assertTrue("Final meeting membership did not synchronize", synchronized)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @TestCaseId("TC-12072")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenGermanAndUsAppLocales_whenOpeningScheduleMeeting_thenDateUsesAppLocale() {
        listOf(Locale.GERMANY, Locale.US).forEach { locale ->
            step("Set app locale to $locale, open a fresh draft and verify its date format") {
                settings.setAppLocale(UiAutomatorSetup.appPackage, locale)
                pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
                form.open(locale)
                form.assertDateUsesAppLocale(today, locale)
                form.close(locale = locale)
                form.awaitClosed(locale)
            }
        }
    }

    @TestCaseId("TC-12072")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenExplicitAndroidTimePreferences_whenReopeningDraft_thenTimesFollow12And24HourSettings() {
        listOf(false, true).forEach { is24Hour ->
            step("Reopen Schedule Meeting with Android ${if (is24Hour) 24 else 12}-hour time enabled") {
                settings.set24HourFormat(is24Hour)
                pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
                form.open()
                form.assertTimePreference(defaultStart.toLocalTime(), is24Hour)
                form.close()
                form.awaitClosed()
            }
        }
    }

    private fun assertNoSavedMeetings() = assertTrue(
        "An abandoned or failed draft created a meeting",
        backendClient.listMeetings(member).isEmpty()
    )

    private fun awaitSavedMeeting(title: String): Meeting {
        var matches = emptyList<Meeting>()
        val saved = UiWaitUtils.retryUntilTimeout(30.seconds) {
            matches = backendClient.listMeetings(member).filter { it.title == title }
            matches.isNotEmpty()
        }
        assertTrue("Meeting $title was not saved", saved)
        assertEquals("Duplicate saved meeting $title", 1, matches.size)
        return matches.single()
    }

    private fun submitAndVerify(title: String, start: LocalDateTime, end: LocalDateTime): Meeting {
        form.schedule()
        form.awaitClosed()
        return awaitSavedMeeting(title).also { meeting ->
            assertEquals(start.atZone(zone).toInstant(), meeting.start)
            assertEquals(end.atZone(zone).toInstant(), meeting.end)
            assertEquals(zone, meeting.zone)
            assertFalse("Unexpected recurrence", meeting.recurring)
            pages.meetingsPage.assertMeeting(meeting, zone)
            pages.meetingsPage.assertHeader(meeting, zone, today)
        }
    }
}
