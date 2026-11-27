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

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.meeting.deleteMeeting
import backendUtils.meeting.getMeeting
import backendUtils.meeting.getMeetingConversation
import backendUtils.meeting.listMeetings
import backendUtils.team.TeamRoles
import backendUtils.team.updateUniqueUsername
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.Tag
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
        step("Then I schedule the meeting, close its link screen if shown and see one saved series and one correctly grouped card") {
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
        step("Given I open an empty draft and Schedule is disabled") {
            form.apply {
                open()
                assertScheduleEnabled(false)
            }
        }
        step("When I enter exactly 64 characters, the complete title is accepted and Schedule is enabled") {
            form.apply {
                enterTitle(maximumTitle)
                assertTitle(maximumTitle)
                assertScheduleEnabled(true)
            }
        }
        step("Then entering 65 characters keeps the accepted title at 64 characters and Schedule enabled") {
            form.apply {
                enterTitle(maximumTitle + "b")
                assertTitle(maximumTitle)
                assertScheduleEnabled(true)
            }
        }
        step("When I clear the title, Schedule becomes disabled again") {
            form.apply {
                enterTitle("")
                assertScheduleEnabled(false)
            }
        }
        step("When I restore the 64-character title and schedule, the meeting is saved with the default times") {
            form.enterTitle(maximumTitle)
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
            step("Given I open a draft and enter its title before exiting using ${if (useBack) "Android Back" else "Close"}") {
                form.apply {
                    open()
                    enterTitle("ML-Schedule-discard")
                }
            }
            step("When I exit the modified draft using ${if (useBack) "Android Back" else "Close"}, I see the discard confirmation") {
                form.apply {
                    close(useBack)
                    assertDiscardConfirmation()
                }
            }
            step("When I cancel discarding, the draft title and default times remain unchanged") {
                form.apply {
                    cancelDiscard()
                    assertTitle("ML-Schedule-discard")
                    assertDateTimes(defaultStart, defaultStart.plusHours(1))
                }
            }
            step("When I exit again using ${if (useBack) "Android Back" else "Close"} and confirm discarding") {
                form.apply {
                    close(useBack)
                    assertDiscardConfirmation()
                    confirmDiscard()
                    awaitClosed()
                }
            }
            step("Then the draft is closed and no meeting was saved") {
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
        step("And I disconnect Wi-Fi and cellular connectivity") {
            settings.disconnectNetwork()
        }
        step("When I submit Schedule Meeting while the device is offline") {
            form.schedule()
        }
        step("Then the scheduling error is shown and the title and default times remain in the draft") {
            form.apply {
                assertSchedulingError()
                assertTitle("ML-Schedule-retry")
                assertDateTimes(defaultStart, defaultStart.plusHours(1))
            }
        }
        step("Then restoring connectivity leaves no saved meeting from the failed attempt") {
            settings.restoreNetwork()
            assertNoSavedMeetings()
        }
        val meeting = stepWithResult("When I retry scheduling online, one meeting is saved with the expected details") {
            submitAndVerify("ML-Schedule-retry", defaultStart, defaultStart.plusHours(1))
        }
        step("When I relaunch Wire after the successful retry") {
            pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
        }
        step("Then the saved meeting remains and no duplicate appears during synchronization") {
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
        step("Given I open a titled draft and set a 90-minute duration") {
            form.apply {
                open()
                enterTitle("ML-Schedule-changed")
                selectEndTime(LocalTime.of(11, 30))
            }
        }
        step("When I change the start date, the end date follows and the 90-minute duration remains") {
            form.apply {
                selectDate(selectedDate)
                assertDateTimes(selectedDate.atTime(10, 0), selectedDate.atTime(11, 30))
            }
        }
        step("When I change the start time to 13:15") {
            form.selectStartTime(LocalTime.of(13, 15))
        }
        val start = selectedDate.atTime(13, 15)
        val end = selectedDate.atTime(14, 45)
        step("Then the selected date and 90-minute duration remain") {
            form.assertDateTimes(start, end)
        }
        step("When I schedule the meeting, it is saved with the selected start and end") {
            submitAndVerify("ML-Schedule-changed", start, end)
        }
    }

    @TestCaseId("TC-12841")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenSameDayMeeting_whenStartingLate_thenEndIsClampedTo2359AndZeroDurationIsRejected() {
        step("When I open a titled draft starting at 22:00, the one-hour duration is preserved") {
            form.apply {
                open()
                enterTitle("ML-Schedule-late")
                selectStartTime(LocalTime.of(22, 0))
                assertDateTimes(today.atTime(22, 0), today.atTime(23, 0))
            }
        }
        step("When I move the start to 23:30, the end is clamped to 23:59") {
            form.apply {
                selectStartTime(LocalTime.of(23, 30))
                assertDateTimes(today.atTime(23, 30), today.atTime(23, 59))
            }
        }
        step("Then I can save the meeting ending at 23:59") {
            submitAndVerify("ML-Schedule-late", today.atTime(23, 30), today.atTime(23, 59))
        }
        step("When I prepare a separate draft starting at 23:59, its end also remains at 23:59") {
            form.apply {
                open()
                enterTitle("ML-Schedule-zero")
                selectStartTime(LocalTime.of(23, 59))
                assertDateTimes(today.atTime(23, 59), today.atTime(23, 59))
            }
        }
        step("Then the zero-duration draft cannot create another saved meeting") {
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
            val meeting = stepWithResult("When I schedule a meeting with the $label repeat option and close its link screen if shown") {
                form.open()
                val title = "ML-Schedule-repeat-$index"
                form.apply {
                    enterTitle(title)
                    selectRepeat(label)
                    assertTitle(title)
                    schedule()
                    awaitClosed()
                    closeMeetingLinkIfShown()
                }
                awaitSavedMeeting(title)
            }
            step("Then the saved $label rule and displayed occurrence sequence match the selected repeat option") {
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
            }
            step("And Member deletes the $label meeting and the current Meetings list becomes empty") {
                backendClient.deleteMeeting(meeting)
                pages.meetingsPage.assertEmpty()
            }
        }
    }

    @TestCaseId("TC-12843")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Suppress("LongMethod") // Keep the scenario inline with separate Allure steps for its actions and outcomes.
    @Test
    fun givenEligibleAndExternalUsers_whenSelectingAndRemovingParticipants_thenOnlyFinalEligibleSelectionIsScheduled() {
        val external = stepWithResult("Given Host and a second team member have MLS devices and an external account exists") {
            givenParticipantAccountsExist()
        }
        val second = clientUserManager.findUserByNameOrNameAlias("user3Name")
        step("Given I open a meeting draft, enter its title and open the participant picker") {
            form.apply {
                open()
                enterTitle("ML-Schedule-participants")
                openParticipants()
            }
        }
        step("When I search for Host by name and select Host") {
            form.apply {
                searchParticipant(requireNotNull(host.name))
                setParticipant(requireNotNull(host.name), true)
            }
        }
        step("And I search for the second member by username and select that member") {
            form.apply {
                searchParticipant(requireNotNull(second.uniqueUsername))
                setParticipant(requireNotNull(second.name), true)
            }
        }
        step("Then Host remains selected when I search by username and select Host again") {
            form.apply {
                searchParticipant(requireNotNull(host.uniqueUsername))
                assertParticipantSelected(requireNotNull(host.name))
                setParticipant(requireNotNull(host.name), true)
            }
        }
        step("When I confirm Host and the second member as participants") {
            form.confirmParticipants()
        }
        step("Then the draft contains Host and the second member once each") {
            form.assertParticipants(listOf(requireNotNull(host.name), requireNotNull(second.name)))
        }
        step("When I reopen participants and search by the external account name and username, neither search has results") {
            form.openParticipants()
            listOf(requireNotNull(external.name), requireNotNull(external.uniqueUsername)).forEach { query ->
                form.searchParticipant(query)
                pages.searchPage.assertNoSearchResultsVisible()
            }
        }
        step("When I find Host, verify Host is selected and deselect Host") {
            form.apply {
                searchParticipant(requireNotNull(host.name))
                assertParticipantSelected(requireNotNull(host.name))
                setParticipant(requireNotNull(host.name), false)
            }
        }
        step("Then the second member remains selected when I search by their username") {
            form.apply {
                searchParticipant(requireNotNull(second.uniqueUsername))
                assertParticipantSelected(requireNotNull(second.name))
            }
        }
        step("When I confirm the participant selection") {
            form.confirmParticipants()
        }
        step("Then the draft contains only the second member") {
            form.assertParticipants(listOf(requireNotNull(second.name)))
        }
        val meeting = stepWithResult("When I schedule the meeting, it is saved with the expected details") {
            submitAndVerify("ML-Schedule-participants", defaultStart, defaultStart.plusHours(1))
        }
        step("Then the meeting conversation contains only the final invitee alongside Member") {
            val synchronized = UiWaitUtils.retryUntilTimeout(30.seconds) {
                val others = backendClient.getMeetingConversation(meeting).getJSONObject("members").getJSONArray("others")
                val ids = (0 until others.length()).map { others.getJSONObject(it).getJSONObject("qualified_id").getString("id") }
                ids == listOf(second.id)
            }
            assertTrue("Final meeting membership did not synchronize", synchronized)
        }
    }

    // Per-app locale switching requires Android 13; CI assigns this test to a compatible phone.
    @Tag(key = "minSdk", value = "33")
    @TestCaseId("TC-12844")
    @Category("regression", "RC", "meetings", "meetingSchedule")
    @Test
    fun givenGermanAndUsAppLocales_whenOpeningScheduleMeeting_thenDateUsesAppLocale() {
        listOf(Locale.GERMANY, Locale.US).forEach { locale ->
            step("When I set Wire to $locale, relaunch it and open a fresh Schedule Meeting draft") {
                settings.setAppLocale(UiAutomatorSetup.appPackage, locale)
                pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
                form.apply {
                    open(locale)
                }
            }
            step("Then the draft date uses $locale, and I close the unchanged draft") {
                form.apply {
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
            step("When I select Android ${if (is24Hour) 24 else 12}-hour time, relaunch Wire and open Schedule Meeting") {
                settings.set24HourFormat(is24Hour)
                pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
                form.apply {
                    open()
                }
            }
            step("Then the draft uses the Android ${if (is24Hour) 24 else 12}-hour format, and I close it") {
                form.apply {
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
            closeMeetingLinkIfShown()
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
