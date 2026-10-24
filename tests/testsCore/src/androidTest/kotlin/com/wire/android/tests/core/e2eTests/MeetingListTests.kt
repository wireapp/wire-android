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
import androidx.test.filters.SdkSuppress
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.meeting.deleteMeeting
import backendUtils.meeting.getMeeting
import backendUtils.meeting.updateMeeting
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Suppress("TooManyFunctions", "MagicNumber")
class MeetingListTests : BaseMeetingUiTest() {
    @TestCaseId("TC-12041")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenNoMeetings_whenOpeningMeetings_thenEmptyStateIsResponsive() {
        step("Open Meetings and verify the empty state contains no cards") {
            pages.meetingsPage.open()
            pages.meetingsPage.assertEmpty()
        }
        step("Navigate away and back to prove the app remains responsive") {
            pages.conversationListPage.clickConversationsMenuEntry()
            pages.conversationListPage.clickConversationsButtonOnMenuEntry()
            pages.conversationListPage.assertConversationListVisible()
            pages.meetingsPage.open()
            pages.meetingsPage.assertEmpty()
        }
    }

    @TestCaseId("TC-12042", "TC-12050")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenInvitedScheduledAndRecurringMeetings_whenOpeningList_thenLocalGroupingOrderAndDetailsAreCorrect() {
        val today = LocalDate.now(zone).plusDays(1)
        val dates = listOf(
            today.atTime(0, 30),
            today.atTime(2, 0),
            today.plusDays(1).atTime(10, 0),
            today.plusDays(3).atTime(8, 0),
            today.plusDays(3).atTime(10, 0)
        )
        val meetings = stepWithResult("Host creates five invited meetings, including a bounded daily recurrence") {
            dates.mapIndexed { index, date ->
                invitedMeeting("ML-Grouping-$index", date.atZone(zone).toInstant(), recurring = index == 4)
            }
        }
        step("Verify the invitee has the same occurrence identifiers and absolute times as the host") {
            meetings.forEach { meeting ->
                val inviteeCopy = backendClient.getMeeting(meeting, member)
                val hostCopy = backendClient.getMeeting(meeting, host)
                assertEquals(meeting.id.id, inviteeCopy.getJSONObject("qualified_id").getString("id"))
                assertEquals(Instant.parse(hostCopy.getString("start_time")), Instant.parse(inviteeCopy.getString("start_time")))
                assertEquals(Instant.parse(hostCopy.getString("end_time")), Instant.parse(inviteeCopy.getString("end_time")))
            }
        }
        step("Open the list at local midnight; the first meeting is on the preceding UTC date") {
            settings.setTime(today.atStartOfDay(zone).toInstant())
            pages.meetingsPage.open()
            pages.meetingsPage.assertMeeting(meetings.first(), zone)
        }
        step("Verify Today, Tomorrow, future day, chronological order, recurrence, participants and no empty or duplicate sections") {
            pages.meetingsPage.assertCompleteList(
                meetings = meetings,
                zone = zone,
                today = today,
                participants = meetings.associate { it.title to 2 }
            )
        }
    }

    @TestCaseId("TC-12044")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenCompletedMeetingToday_whenLocalMidnightPasses_thenOnlyTheOldMeetingDisappears() {
        val today = LocalDate.now(zone).plusDays(1)
        val completed = createMeeting("ML-Completed", today.atTime(20, 0).atZone(zone).toInstant())
        val future = createMeeting("ML-Future", today.plusDays(1).atTime(10, 0).atZone(zone).toInstant())
        step("Open Meetings shortly before local midnight and verify the completed meeting remains under Today") {
            settings.setTime(today.plusDays(1).atStartOfDay(zone).minusSeconds(45).toInstant())
            pages.meetingsPage.open()
            pages.meetingsPage.assertMeeting(completed, zone)
            pages.meetingsPage.assertHeader(completed, zone, today)
            pages.meetingsPage.assertCompleteList(listOf(completed, future), zone, today)
            pages.meetingsPage.returnToTopAndAssertFirst(completed.title)
        }
        step("Advance to five seconds before midnight, then verify the natural rollover without restarting or refreshing") {
            settings.setTime(today.plusDays(1).atStartOfDay(zone).minusSeconds(5).toInstant())
            pages.meetingsPage.assertGone(completed.title, timeout = 20.seconds)
            pages.meetingsPage.assertMeeting(future, zone)
            pages.meetingsPage.returnToTopAndAssertFirst(future.title)
            pages.meetingsPage.assertHeader(future, zone, today.plusDays(1))
            pages.meetingsPage.assertCompleteList(listOf(future), zone, today.plusDays(1))
        }
    }

    @TestCaseId("TC-12045")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenMoreThanInitialPageOfMeetings_whenScrolling_thenAllMeetingsLoadInOrderWithoutDuplicatesOrJumps() {
        val firstStart = LocalDate.now(zone).plusDays(1).atTime(9, 0).atZone(zone).toInstant()
        val meetings = stepWithResult("Create 65 distinct meetings, exceeding the initial 40-entry page") {
            (0 until 65).map { index ->
                createMeeting("ML-Paging-${index.toString().padStart(2, '0')}", firstStart.plusSeconds(index * 900L))
            }
        }
        step("Open Meetings and verify only the initial viewport is visible") {
            pages.meetingsPage.open()
            pages.meetingsPage.assertMeeting(meetings.first(), zone)
            pages.meetingsPage.assertNotVisible(meetings.last().title)
        }
        step("Scroll through every page and verify the full header/card sequence and meeting details") {
            pages.meetingsPage.assertCompleteList(meetings, zone, LocalDate.now(zone))
        }
        step("Scroll back and verify previously loaded meetings remain available") {
            pages.meetingsPage.returnToTopAndAssertFirst(meetings.first().title)
            pages.meetingsPage.assertMeeting(meetings.first(), zone)
        }
    }

    @TestCaseId("TC-12047")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenMeetingListIsActive_whenHostCreatesUpdatesDeletesAndChangesWhileBackgrounded_thenListSynchronizes() {
        pages.meetingsPage.open()
        pages.meetingsPage.assertEmpty()
        val start = Instant.now().plusSeconds(7200).truncatedTo(ChronoUnit.MINUTES)
        val original = stepWithResult("Host creates and invites the Android member while Meetings remains open") {
            invitedMeeting("ML-Live-created", start)
        }
        pages.meetingsPage.assertMeeting(original, zone)
        val updated = stepWithResult("Host changes title and time without the invitee refreshing") {
            backendClient.updateMeeting(original, "ML-Live-updated", start.plusSeconds(3600), start.plusSeconds(5400))
        }
        step("Verify the updated meeting replaces the original card") {
            pages.meetingsPage.assertGone(original.title)
            pages.meetingsPage.assertMeeting(updated, zone)
            pages.meetingsPage.assertCompleteList(listOf(updated), zone, LocalDate.now(zone))
        }
        step("Host deletes the meeting and the active list returns to its empty state") {
            backendClient.deleteMeeting(updated)
            createdMeetings.removeAll { it.id == updated.id }
            pages.meetingsPage.assertGone(updated.title)
            pages.meetingsPage.assertEmpty()
        }
        val latest = stepWithResult("Background Wire, then create and update another invited meeting") {
            device.pressHome()
            val background = invitedMeeting("ML-Background-created", start)
            backendClient.updateMeeting(background, "ML-Background-latest", start.plusSeconds(7200), start.plusSeconds(9000))
        }
        step("Return to the existing app task and verify only the latest meeting information is shown") {
            pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
            pages.meetingsPage.assertMeeting(latest, zone)
            pages.meetingsPage.assertGone("ML-Background-created")
            pages.meetingsPage.assertCompleteList(listOf(latest), zone, LocalDate.now(zone))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @TestCaseId("TC-12051")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenOneMeeting_whenAppLocaleChangesFromGermanToUs_thenFormattingChangesButOccurrenceDoesNot() {
        val start = LocalDate.now(zone).plusDays(3).atTime(13, 5).atZone(zone).toInstant()
        val meeting = createMeeting("ML-Locale", start)
        step("Set German app locale and verify the localized day heading and 24-hour time") {
            settings.setAppLocale(UiAutomatorSetup.appPackage, Locale.GERMANY)
            pages.meetingsPage.open(Locale.GERMANY)
            pages.meetingsPage.assertMeeting(meeting, zone, Locale.GERMANY)
            pages.meetingsPage.assertHeader(meeting, zone, LocalDate.now(zone), Locale.GERMANY)
        }
        step("Change only app locale to US English and verify the same meeting uses localized time and date") {
            settings.setAppLocale(UiAutomatorSetup.appPackage, Locale.US)
            pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
            pages.meetingsPage.assertMeeting(meeting, zone, Locale.US)
            pages.meetingsPage.assertHeader(meeting, zone, LocalDate.now(zone), Locale.US)
            val unchanged = backendClient.getMeeting(meeting, member)
            assertEquals(meeting.start, Instant.parse(unchanged.getString("start_time")))
            assertEquals(meeting.end, Instant.parse(unchanged.getString("end_time")))
            assertEquals(meeting.id.id, unchanged.getJSONObject("qualified_id").getString("id"))
        }
    }

    @TestCaseId("TC-12053", "TC-12055")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenUpcomingMeetings_whenCountdownReachesStartAndAppReturnsFromBackground_thenJoinStateIsCurrent() {
        val start = LocalDate.now(zone).plusDays(1).atTime(12, 0).atZone(zone).toInstant()
        val foreground = createMeeting("ML-Countdown", start)
        val background = createMeeting("ML-Resume", start.plusSeconds(120))
        step("Open Meetings more than five minutes before start and verify countdown is absent") {
            settings.setTime(start.minusSeconds(330))
            pages.meetingsPage.open()
            pages.meetingsPage.assertMeeting(foreground, zone)
            pages.meetingsPage.assertNoCountdown(foreground.title)
        }
        step("Advance to five seconds before the countdown boundary and observe successive updates without refreshing") {
            settings.setTime(start.minusSeconds(305))
            // The UI floors remaining seconds, so the first full second below the boundary displays 4:59.
            pages.meetingsPage.awaitCountdown(foreground.title, "4:59", 10.seconds)
            pages.meetingsPage.awaitCountdown(foreground.title, "4:58", 3.seconds)
            pages.meetingsPage.awaitCountdown(foreground.title, "4:57", 3.seconds)
        }
        step("Advance the clock to just before start and verify the open list automatically shows Join") {
            settings.setTime(start.minusSeconds(2))
            pages.meetingsPage.awaitJoin(foreground.title, 15.seconds)
            pages.meetingsPage.assertMeeting(foreground, zone)
        }
        step("Background Wire before the second meeting, advance the device clock past its start, then return") {
            pages.meetingsPage.assertMeeting(background, zone)
            pages.meetingsPage.assertHasCountdown(background.title)
            device.pressHome()
            settings.setTime(background.start.plusSeconds(1))
            pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
            pages.meetingsPage.awaitJoin(background.title)
            pages.meetingsPage.assertMeeting(background, zone)
        }
    }
}
