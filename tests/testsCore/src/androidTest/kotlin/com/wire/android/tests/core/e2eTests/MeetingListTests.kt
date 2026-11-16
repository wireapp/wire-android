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
import backendUtils.meeting.updateMeeting
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import service.models.Meeting
import uiautomatorutils.DeviceSettingsHelper
import user.utils.ClientUser
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Suppress("TooManyFunctions", "MagicNumber")
class MeetingListTests : BaseUiTest() {
    private lateinit var settings: DeviceSettingsHelper
    private val createdMeetings = mutableListOf<Meeting>()
    private lateinit var host: ClientUser
    private lateinit var member: ClientUser
    private var hostServiceStarted = false
    private val zone = ZoneId.of("Europe/Berlin")

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        settings = DeviceSettingsHelper(device)
        settings.setZone(zone)
        settings.useLocaleTimeFormat()
        // English selectors also support the English regional locales used by the CI phones.
        check(context.resources.configuration.locales[0].language == "en") {
            "Meeting UI automation requires English UI; TC-12051 changes and restores the app locale"
        }
        givenTeamMemberIsLoggedIn()
    }

    @After
    fun restoreDeviceAndRemoveMeetings() {
        try {
            if (::settings.isInitialized) settings.close()
        } finally {
            try {
                removeCreatedMeetings()
            } finally {
                if (hostServiceStarted) testServiceHelper.testServiceClient.cleanUp()
            }
        }
    }

    @TestCaseId("TC-12041")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenNoMeetings_whenOpeningMeetings_thenEmptyStateIsResponsive() {
        step("When I open Meetings") {
            pages.meetingsPage.open()
        }
        step("Then I see the empty state without meeting cards") {
            pages.meetingsPage.assertEmpty()
        }
        step("When I open Conversations and see the conversation list") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickConversationsButtonOnMenuEntry()
                assertConversationListVisible()
            }
        }
        step("And I return to Meetings") {
            pages.meetingsPage.open()
        }
        step("Then I see the empty state without meeting cards again") {
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
        val meetings = stepWithResult("Given Host creates five meetings and invites Member, including a bounded daily recurrence") {
            dates.mapIndexed { index, date ->
                invitedMeeting("ML-Grouping-$index", date.atZone(zone).toInstant(), recurring = index == 4)
            }
        }
        step("Then Member and Host have the same meeting occurrence IDs, start times and end times") {
            meetings.forEach { meeting ->
                val inviteeCopy = backendClient.getMeeting(meeting, member)
                val hostCopy = backendClient.getMeeting(meeting, host)
                assertEquals(meeting.id.id, inviteeCopy.getJSONObject("qualified_id").getString("id"))
                assertEquals(Instant.parse(hostCopy.getString("start_time")), Instant.parse(inviteeCopy.getString("start_time")))
                assertEquals(Instant.parse(hostCopy.getString("end_time")), Instant.parse(inviteeCopy.getString("end_time")))
            }
        }
        step("When I set local midnight, open Meetings and see the first meeting from the preceding UTC date") {
            settings.setTime(today.atStartOfDay(zone).toInstant())
            pages.meetingsPage.apply {
                open()
                assertMeeting(meetings.first(), zone)
            }
        }
        step("Then I see all meetings in date order with correct headers, recurrence and participants, without duplicates") {
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
        val completed = stepWithResult("Given Host creates a meeting that will be completed before local midnight") {
            createMeeting("ML-Completed", today.atTime(20, 0).atZone(zone).toInstant())
        }
        val future = stepWithResult("And Host creates a future meeting for the following morning") {
            createMeeting("ML-Future", today.plusDays(1).atTime(10, 0).atZone(zone).toInstant())
        }
        step("When I open Meetings before midnight and see the completed meeting under Today alongside the future meeting") {
            settings.setTime(today.plusDays(1).atStartOfDay(zone).minusSeconds(45).toInstant())
            pages.meetingsPage.apply {
                dismissSamsungSoftwareUpdatePromptIfShown()
                open()
                assertMeeting(completed, zone)
                assertHeader(completed, zone, today)
                assertCompleteList(listOf(completed, future), zone, today)
                returnToTopAndAssertFirst(completed.title)
            }
        }
        step("When I advance the device clock to five seconds before local midnight") {
            settings.setTime(today.plusDays(1).atStartOfDay(zone).minusSeconds(5).toInstant())
            pages.meetingsPage.dismissSamsungSoftwareUpdatePromptIfShown()
        }
        step("Then after local midnight the completed meeting disappears and only the future meeting remains under Today") {
            pages.meetingsPage.apply {
                assertGone(completed.title, timeout = 20.seconds)
                assertMeeting(future, zone)
                returnToTopAndAssertFirst(future.title)
                assertHeader(future, zone, today.plusDays(1))
                assertCompleteList(listOf(future), zone, today.plusDays(1))
            }
        }
    }

    @TestCaseId("TC-12045")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenMoreThanInitialPageOfMeetings_whenScrolling_thenAllMeetingsLoadInOrderWithoutDuplicatesOrJumps() {
        val firstStart = LocalDate.now(zone).plusDays(1).atTime(9, 0).atZone(zone).toInstant()
        val meetings = stepWithResult("Given Host creates 65 distinct meetings, exceeding the initial 40-entry page") {
            (0 until 65).map { index ->
                createMeeting("ML-Paging-${index.toString().padStart(2, '0')}", firstStart.plusSeconds(index * 900L))
            }
        }
        step("When I open Meetings and see the first meeting while the last meeting is not visible") {
            pages.meetingsPage.apply {
                open()
                assertMeeting(meetings.first(), zone)
                assertNotVisible(meetings.last().title)
            }
        }
        step("Then I scroll through all meetings and see the complete header and card sequence with correct details") {
            pages.meetingsPage.assertCompleteList(meetings, zone, LocalDate.now(zone))
        }
        step("And I scroll to the top and see the first meeting again") {
            pages.meetingsPage.apply {
                returnToTopAndAssertFirst(meetings.first().title)
                assertMeeting(meetings.first(), zone)
            }
        }
    }

    @TestCaseId("TC-12047")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenMeetingListIsActive_whenHostCreatesUpdatesDeletesAndChangesWhileBackgrounded_thenListSynchronizes() {
        step("Given I open Meetings and see no upcoming meetings") {
            pages.meetingsPage.apply {
                open()
                assertEmpty()
            }
        }
        val start = Instant.now().plusSeconds(7200).truncatedTo(ChronoUnit.MINUTES)
        val original = stepWithResult("When Host creates a meeting and invites Member while Meetings stays open") {
            invitedMeeting("ML-Live-created", start)
        }
        step("Then I see the new meeting on the open Meetings screen") {
            pages.meetingsPage.assertMeeting(original, zone)
        }
        val updated = stepWithResult("When Host changes the meeting title, start time and end time while Member stays on Meetings") {
            backendClient.updateMeeting(original, "ML-Live-updated", start.plusSeconds(3600), start.plusSeconds(5400))
        }
        step("Then I see the updated meeting and the original meeting card is absent") {
            pages.meetingsPage.apply {
                assertGone(original.title)
                assertMeeting(updated, zone)
                assertCompleteList(listOf(updated), zone, LocalDate.now(zone))
            }
        }
        step("When Host deletes the meeting, its card disappears and I see the empty Meetings screen") {
            backendClient.deleteMeeting(updated)
            createdMeetings.removeAll { it.id == updated.id }
            pages.meetingsPage.apply {
                assertGone(updated.title)
                assertEmpty()
            }
        }
        val latest = stepWithResult("When I background Wire and Host creates and updates another meeting with Member invited") {
            device.pressHome()
            val background = invitedMeeting("ML-Background-created", start)
            backendClient.updateMeeting(background, "ML-Background-latest", start.plusSeconds(7200), start.plusSeconds(9000))
        }
        step("Then I return to Wire and see only the latest meeting title, time and details") {
            pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
            pages.meetingsPage.apply {
                assertMeeting(latest, zone)
                assertGone("ML-Background-created")
                assertCompleteList(listOf(latest), zone, LocalDate.now(zone))
            }
        }
    }

    // Android 13 introduced the per-app locale command. The Android 11 and 12 CI phones skip this case.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @TestCaseId("TC-12051")
    @Category("regression", "RC", "meetings")
    @Test
    fun givenOneMeeting_whenAppLocaleChangesFromGermanToUs_thenFormattingChangesButOccurrenceDoesNot() {
        val start = LocalDate.now(zone).plusDays(3).atTime(13, 5).atZone(zone).toInstant()
        val meeting = stepWithResult("Given Host creates a meeting for Member to check in German and US English") {
            createMeeting("ML-Locale", start)
        }
        step("When I set Wire to German, open Meetings and see the German date header and 24-hour meeting time") {
            settings.setAppLocale(UiAutomatorSetup.APP_ALPHA, Locale.GERMANY)
            pages.meetingsPage.apply {
                open(Locale.GERMANY)
                assertMeeting(meeting, zone, Locale.GERMANY)
                assertHeader(meeting, zone, LocalDate.now(zone), Locale.GERMANY)
            }
        }
        step("Then after I switch Wire to US English, the same meeting has English formatting and unchanged occurrence details") {
            settings.setAppLocale(UiAutomatorSetup.APP_ALPHA, Locale.US)
            pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
            pages.meetingsPage.apply {
                assertMeeting(meeting, zone, Locale.US)
                assertHeader(meeting, zone, LocalDate.now(zone), Locale.US)
            }
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
        val foreground = stepWithResult("Given Host creates the first meeting for the foreground countdown") {
            createMeeting("ML-Countdown", start)
        }
        val background = stepWithResult("And Host creates a second meeting thirty seconds later for the background check") {
            createMeeting("ML-Resume", start.plusSeconds(30))
        }
        step("When I open Meetings more than five minutes before the first meeting and see no countdown") {
            settings.setTime(start.minusSeconds(330))
            pages.meetingsPage.apply {
                dismissSamsungSoftwareUpdatePromptIfShown()
                open()
                assertMeeting(foreground, zone)
                assertNoCountdown(foreground.title)
            }
        }
        step("Then the open meeting card counts down from 4:59 to 4:57 as the five-minute boundary passes") {
            val untilCountdown = (start.minusSeconds(300).epochSecond - Instant.now().epochSecond).coerceAtLeast(0).seconds
            pages.meetingsPage.apply {
                awaitCountdown(foreground.title, "4:59", untilCountdown + 10.seconds)
                awaitCountdown(foreground.title, "4:58", 3.seconds)
                awaitCountdown(foreground.title, "4:57", 3.seconds)
            }
        }
        step("And I see the second meeting countdown before advancing to the first meeting start") {
            val untilCountdown =
                (background.start.minusSeconds(300).epochSecond - Instant.now().epochSecond).coerceAtLeast(0).seconds
            pages.meetingsPage.assertHasCountdown(background.title, untilCountdown + 10.seconds)
        }
        step("Then after I advance to the meeting start, the open meeting card automatically shows Join") {
            settings.setTime(start.minusSeconds(2))
            pages.meetingsPage.apply {
                dismissSamsungSoftwareUpdatePromptIfShown()
                awaitJoin(foreground.title, 15.seconds)
                assertMeeting(foreground, zone)
            }
        }
        step("And I see the second countdown, background Wire, advance past its start and return to see Join") {
            pages.meetingsPage.apply {
                assertMeeting(background, zone)
                assertHasCountdown(background.title)
            }
            device.pressHome()
            settings.setTime(background.start.plusSeconds(1))
            pages.meetingsPage.dismissSamsungSoftwareUpdatePromptIfShown()
            pages.commonAppPage.restartWireApp().assertWireAppIsInForeground()
            pages.meetingsPage.apply {
                awaitJoin(background.title)
                assertMeeting(background, zone)
            }
        }
    }

    @Suppress("LongParameterList")
    private fun createMeeting(
        title: String,
        start: Instant,
        end: Instant = start.plusSeconds(1800),
        zone: ZoneId = this.zone,
        recurring: Boolean = false,
    ): Meeting {
        if (!hostServiceStarted) {
            hostServiceStarted = true
            testServiceHelper.addDevice("user1Name", null, HOST_DEVICE, retryOnFailure = false)
        }
        return testServiceHelper.userCreatesMeeting(
            ownerAlias = "user1Name",
            participantAliases = "user2Name",
            title = title,
            start = start,
            end = end,
            zone = zone,
            recurring = recurring,
            deviceName = HOST_DEVICE
        ).also { createdMeetings.add(it) }
    }

    private fun removeCreatedMeetings() {
        var failure: Throwable? = null
        createdMeetings.toList().forEach { meeting ->
            try {
                backendClient.deleteMeeting(meeting)
                createdMeetings.remove(meeting)
            } catch (error: Exception) {
                if (failure == null) failure = error else failure?.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }

    private fun invitedMeeting(title: String, start: Instant, recurring: Boolean = false): Meeting = createMeeting(
        title,
        start,
        zone = ZoneId.of("America/Los_Angeles"),
        recurring = recurring
    )

    private fun givenTeamMemberIsLoggedIn() {
        step("Given Host creates an MLS team, adds Member, enables Meetings and Member is me") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                TEAM_NAME,
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", TEAM_NAME, backendClient)
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            host = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member = clientUserManager.findUserByNameOrNameAlias("user2Name")
            backendSetupHelper.userEnablesMeetingsForTeam("user1Name")
            clientUserManager.setSelfUser(member)
        }
        step("And I log in as Member on Staging, complete the login prompts and see the conversation list") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
                enterTeamMemberLoggingEmail(requireNotNull(member.email))
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(requireNotNull(member.password))
                clickLoginButton()
            }
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.apply {
                assertConversationListVisible()
                waitUntilWireServiceNotificationDisappears()
            }
        }
    }

    private fun <T> stepWithResult(name: String, action: () -> T): T {
        var result: T? = null
        step(name) { result = action() }
        return requireNotNull(result)
    }

    private companion object {
        const val TEAM_NAME = "Meeting automation"
        const val HOST_DEVICE = "MeetingsHost"
    }
}
