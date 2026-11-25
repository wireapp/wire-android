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
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
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
        step("Given I open Meetings at 09:17 local time with 24-hour setting") {
            // A fixed local morning avoids midnight and next-hour races in the default-value assertions.
            today = LocalDate.now(zone).plusDays(1)
            settings.setTime(today.atTime(9, 17).atZone(zone).toInstant())
            settings.set24HourFormat(true)
            defaultStart = today.atTime(10, 0)
            pages.meetingsPage.apply {
                dismissSamsungSoftwareUpdatePromptIfShown()
                open()
            }
        }
    }

    @After
    fun removeMeetingsCreatedThroughUi() {
        if (!fixtureReady) return
        // Restore connectivity before backend cleanup even when the offline assertion failed.
        settings.restoreNetwork()
        backendClient.listMeetings(member).forEach { backendClient.deleteMeeting(it) }
    }

    @TestCaseId("TC-12835", "TC-12839")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenLocalMorning_whenSchedulingValidMeeting_thenDefaultsAndSavedDetailsAreCorrect() {
        step("When I open Schedule Meeting, I see every field and the next-full-hour default with a one-hour duration") {
            form.apply {
                open()
                assertFields()
                assertDateTimes(defaultStart, defaultStart.plusHours(1))
                assertScheduleEnabled(false)
            }
        }
        step("Then I can schedule a meeting and see one saved series and one correctly grouped card") {
            form.enterTitle("ML-Schedule-valid")
            val meeting = submitAndVerify("ML-Schedule-valid", defaultStart, defaultStart.plusHours(1))
            pages.meetingsPage.assertCompleteList(listOf(meeting), zone, today)
        }
    }

    @TestCaseId("TC-12836")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenRequiredTitle_whenEnteringBoundaryValues_thenOnlyUpTo64CharactersAreAccepted() {
        val maximumTitle = "ML-" + "a".repeat(61)
        step("When I open a draft, an empty title cannot be scheduled and exactly 64 characters are accepted") {
            form.apply {
                open()
                assertScheduleEnabled(false)
                enterTitle(maximumTitle)
                assertTitle(maximumTitle)
                assertScheduleEnabled(true)
            }
        }
        step("Then entering 65 characters keeps the title within the limit and I can schedule the 64-character title") {
            form.apply {
                enterTitle(maximumTitle + "b")
                assertTitle(maximumTitle)
                assertScheduleEnabled(true)
                enterTitle("")
                assertScheduleEnabled(false)
                enterTitle(maximumTitle)
            }
            submitAndVerify(maximumTitle, defaultStart, defaultStart.plusHours(1))
        }
    }

    @TestCaseId("TC-12837")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenEmptyAndModifiedDrafts_whenClosingOrPressingBack_thenDiscardIsConfirmedOnlyForChanges() {
        step("When I close an unchanged draft, it closes without a discard dialog or saved meeting") {
            form.apply {
                open()
                close()
                awaitClosed()
            }
            assertNoSavedMeetings()
            pages.meetingsPage.assertEmpty()
        }
        listOf(false, true).forEach { useBack ->
            step("Then I can cancel discard, keep my changes and confirm discard using ${if (useBack) "Android Back" else "Close"}") {
                form.apply {
                    open()
                    enterTitle("ML-Schedule-discard")
                    close(useBack)
                    assertDiscardConfirmation()
                    cancelDiscard()
                    assertTitle("ML-Schedule-discard")
                    assertDateTimes(defaultStart, defaultStart.plusHours(1))
                    close(useBack)
                    assertDiscardConfirmation()
                    confirmDiscard()
                    awaitClosed()
                }
                assertNoSavedMeetings()
                pages.meetingsPage.assertEmpty()
            }
        }
    }

    @TestCaseId("TC-12838")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenValidDraftOffline_whenSchedulingThenRetryingOnline_thenErrorPreservesDraftAndCreatesOnlyOneMeeting() {
        step("Given I have a valid meeting draft") {
            form.apply {
                open()
                enterTitle("ML-Schedule-retry")
            }
        }
        step("When I disconnect Wi-Fi and cellular connectivity and schedule, I see an error and my draft is preserved") {
            settings.disconnectNetwork()
            form.apply {
                schedule()
                assertSchedulingError()
                assertTitle("ML-Schedule-retry")
                assertDateTimes(defaultStart, defaultStart.plusHours(1))
            }
        }
        step("Then restoring connectivity leaves no saved meeting from the failed attempt") {
            settings.restoreNetwork()
            assertNoSavedMeetings()
        }
        step("And retrying saves a single meeting that remains after relaunch and synchronization") {
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

    @TestCaseId("TC-12840")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenCustomDuration_whenChangingDateAndStartTime_thenEndDateTracksStartAndDurationIsPreserved() {
        val selectedDate = today.plusDays(1)
        step("When I set a 90-minute duration and change the start date, the end date follows the start date") {
            form.apply {
                open()
                enterTitle("ML-Schedule-changed")
                selectEndTime(LocalTime.of(11, 30))
                selectDate(selectedDate)
                assertDateTimes(selectedDate.atTime(10, 0), selectedDate.atTime(11, 30))
            }
        }
        step("Then changing the start time preserves 90 minutes and I can save the selected values") {
            form.selectStartTime(LocalTime.of(13, 15))
            val start = selectedDate.atTime(13, 15)
            val end = selectedDate.atTime(14, 45)
            form.assertDateTimes(start, end)
            submitAndVerify("ML-Schedule-changed", start, end)
        }
    }

    @TestCaseId("TC-12841")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenSameDayMeeting_whenStartingLate_thenEndIsClampedTo2359AndZeroDurationIsRejected() {
        step("When I select late start times, the normal duration is preserved when it fits and 23:30 ends at 23:59") {
            form.apply {
                open()
                enterTitle("ML-Schedule-late")
                selectStartTime(LocalTime.of(22, 0))
                assertDateTimes(today.atTime(22, 0), today.atTime(23, 0))
                selectStartTime(LocalTime.of(23, 30))
                assertDateTimes(today.atTime(23, 30), today.atTime(23, 59))
            }
            submitAndVerify("ML-Schedule-late", today.atTime(23, 30), today.atTime(23, 59))
        }
        step("Then a separate draft starting at 23:59 has zero duration and cannot be scheduled") {
            form.apply {
                open()
                enterTitle("ML-Schedule-zero")
                selectStartTime(LocalTime.of(23, 59))
                assertDateTimes(today.atTime(23, 59), today.atTime(23, 59))
            }
            if (form.isScheduleEnabled()) {
                form.apply {
                    schedule()
                    assertSchedulingError()
                }
            }
            assertEquals(listOf("ML-Schedule-late"), backendClient.listMeetings(member).map { it.title })
        }
    }

    @TestCaseId("TC-12842")
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
            step("When I schedule using $label, the saved rule and occurrence sequence match my selection") {
                form.open()
                val title = "ML-Schedule-repeat-$index"
                form.apply {
                    enterTitle(title)
                    selectRepeat(label)
                    assertTitle(title)
                    schedule()
                    awaitClosed()
                }
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
                pages.conversationListPage.apply {
                    clickConversationsMenuEntry()
                    clickConversationsButtonOnMenuEntry()
                }
                pages.meetingsPage.apply {
                    open()
                    assertEmpty()
                }
            }
        }
    }

    @TestCaseId("TC-12843")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenEligibleAndExternalUsers_whenSelectingAndRemovingParticipants_thenOnlyFinalEligibleSelectionIsScheduled() {
        val external = stepWithResult("Given Host and a second team member have MLS devices and an external account exists") {
            givenParticipantAccountsExist()
        }
        val second = clientUserManager.findUserByNameOrNameAlias("user3Name")
        step("When I select Host and a second team member by name and username, searching Host again keeps one selection") {
            form.apply {
                open()
                enterTitle("ML-Schedule-participants")
                openParticipants()
                searchParticipant(requireNotNull(host.name))
                setParticipant(requireNotNull(host.name), true)
                searchParticipant(requireNotNull(second.uniqueUsername))
                setParticipant(requireNotNull(second.name), true)
                searchParticipant(requireNotNull(host.uniqueUsername))
                assertParticipantSelected(requireNotNull(host.name))
                setParticipant(requireNotNull(host.name), true)
                confirmParticipants()
                assertParticipants(listOf(requireNotNull(host.name), requireNotNull(second.name)))
            }
        }
        step("Then reopening selection excludes the external account by name and username and lets me remove only Host") {
            form.openParticipants()
            listOf(requireNotNull(external.name), requireNotNull(external.uniqueUsername)).forEach { query ->
                form.searchParticipant(query)
                pages.searchPage.assertNoSearchResultsVisible()
            }
            form.apply {
                searchParticipant(requireNotNull(host.name))
                assertParticipantSelected(requireNotNull(host.name))
                setParticipant(requireNotNull(host.name), false)
                searchParticipant(requireNotNull(second.uniqueUsername))
                assertParticipantSelected(requireNotNull(second.name))
                confirmParticipants()
                assertParticipants(listOf(requireNotNull(second.name)))
            }
        }
        step("And scheduling creates a conversation containing only the final invitee") {
            val meeting = submitAndVerify("ML-Schedule-participants", defaultStart, defaultStart.plusHours(1))
            val synchronized = UiWaitUtils.retryUntilTimeout(30.seconds) {
                val others = backendClient.getMeetingConversation(meeting).getJSONObject("members").getJSONArray("others")
                val ids = (0 until others.length()).map { others.getJSONObject(it).getJSONObject("qualified_id").getString("id") }
                ids == listOf(second.id)
            }
            assertTrue("Final meeting membership did not synchronize", synchronized)
        }
    }

    // Android 13 introduced the per-app locale command; Android 11 and 12 skip this case.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @TestCaseId("TC-12844")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenGermanAndUsAppLocales_whenOpeningScheduleMeeting_thenDateUsesAppLocale() {
        listOf(Locale.GERMANY, Locale.US).forEach { locale ->
            step("When I set the app locale to $locale and open a fresh draft, its dates use that locale") {
                settings.setAppLocale(UiAutomatorSetup.appPackage, locale)
                pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
                form.apply {
                    open(locale)
                    assertDateUsesAppLocale(today, locale)
                    close(locale = locale)
                    awaitClosed(locale)
                }
            }
        }
    }

    @TestCaseId("TC-12844")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenExplicitAndroidTimePreferences_whenReopeningDraft_thenTimesFollow12And24HourSettings() {
        listOf(false, true).forEach { is24Hour ->
            step("When I reopen Schedule Meeting, its times follow the Android ${if (is24Hour) 24 else 12}-hour setting") {
                settings.set24HourFormat(is24Hour)
                pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
                form.apply {
                    open()
                    assertTimePreference(defaultStart.toLocalTime(), is24Hour)
                    close()
                    awaitClosed()
                }
            }
        }
    }

    private fun givenParticipantAccountsExist() = backendSetupHelper.run {
        userXAddsUsersToTeam(
            "user1Name",
            "user3Name",
            TEAM_NAME,
            TeamRoles.Member,
            backendClient,
            context,
            true
        )
        startMeetingServiceDevice("user1Name", HOST_DEVICE)
        startMeetingServiceDevice("user3Name", "ScheduleInvitee")
        createPersonalUser(backendClient = backendClient).also { external ->
            runBlocking { backendClient.updateUniqueUsername(external, requireNotNull(external.uniqueUsername)) }
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
        form.apply {
            schedule()
            awaitClosed()
        }
        return awaitSavedMeeting(title).also { meeting ->
            assertEquals(start.atZone(zone).toInstant(), meeting.start)
            assertEquals(end.atZone(zone).toInstant(), meeting.end)
            assertEquals(zone, meeting.zone)
            assertFalse("Unexpected recurrence", meeting.recurring)
            pages.meetingsPage.apply {
                assertMeeting(meeting, zone)
                assertHeader(meeting, zone, today)
            }
        }
    }
}
