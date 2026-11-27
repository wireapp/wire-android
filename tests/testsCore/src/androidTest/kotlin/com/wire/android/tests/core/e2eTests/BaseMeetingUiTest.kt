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

import backendUtils.meeting.deleteMeeting
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import org.junit.After
import org.junit.Before
import service.models.Meeting
import uiautomatorutils.DeviceSettingsHelper
import user.utils.ClientUser
import java.time.Instant
import java.time.ZoneId

@Suppress("TooManyFunctions", "MagicNumber")
abstract class BaseMeetingUiTest : BaseUiTest() {
    protected lateinit var settings: DeviceSettingsHelper
    protected val createdMeetings = mutableListOf<Meeting>()
    protected lateinit var host: ClientUser
    protected lateinit var member: ClientUser
    private var hostServiceStarted = false
    private var meetingServiceStarted = false
    protected val zone = ZoneId.of("Europe/Berlin")

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        settings = DeviceSettingsHelper(device)
        // Interrupted clock-changing tests must not leave the next MLS login in the future.
        settings.synchronizeTime(backendClient)
        settings.setZone(zone)
        settings.useLocaleTimeFormat()
        // English selectors also support the English regional locales used by the CI phones.
        check(context.resources.configuration.locales[0].language == "en") {
            "Meeting UI automation requires English UI; meeting locale coverage changes and restores the app locale"
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
                stopMeetingServiceDevices()
            }
        }
    }

    // Release finished fixture devices before long UI checks outlive the service instance limit.
    protected fun stopMeetingServiceDevices() {
        if (meetingServiceStarted) {
            testServiceHelper.testServiceClient.cleanUp()
            meetingServiceStarted = false
            hostServiceStarted = false
        }
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

    @Suppress("LongMethod") // Keep setup and login actions in order with explicit Allure step boundaries.
    private fun givenTeamMemberIsLoggedIn() {
        step("Given Host creates the team and configures MLS") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                TEAM_NAME,
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", TEAM_NAME, backendClient)
        }
        step("And Host adds Member to the team") {
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
        }
        step("And Host enables Meetings for the team") {
            backendSetupHelper.userEnablesMeetingsForTeam("user1Name")
        }
        step("And Member is selected as the logged-in user") {
            clientUserManager.setSelfUser(member)
        }
        step("And Member opens the login screen on Staging") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }
        step("When Member logs in with their credentials") {
            pages.loginPage.apply {
                enterUserIdentifier(requireNotNull(member.email))
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterUserPassword(requireNotNull(member.password))
                clickLoginButton()
            }
        }
        step("And Member completes the post-login prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }
        step("Then Member sees the conversation list and the service notification disappears") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                waitUntilWireServiceNotificationDisappears()
            }
        }
    }

    protected fun <T> stepWithResult(name: String, action: () -> T): T {
        var result: T? = null
        step(name) { result = action() }
        return requireNotNull(result)
    }

    protected companion object {
        const val TEAM_NAME = "Meeting automation"
        const val HOST_DEVICE = "MeetingsHost"
    }

    @Suppress("LongParameterList")
    protected fun createMeeting(
        title: String,
        start: Instant,
        end: Instant = start.plusSeconds(1800),
        zone: ZoneId = this.zone,
        recurring: Boolean = false,
    ): Meeting {
        if (!hostServiceStarted) {
            hostServiceStarted = true
            startMeetingServiceDevice("user1Name", HOST_DEVICE)
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

    protected fun invitedMeeting(title: String, start: Instant, recurring: Boolean = false): Meeting = createMeeting(
        title,
        start,
        zone = ZoneId.of("America/Los_Angeles"),
        recurring = recurring
    )

    protected fun startMeetingServiceDevice(userAlias: String, deviceName: String) {
        meetingServiceStarted = true
        testServiceHelper.addDevice(userAlias, null, deviceName, retryOnFailure = false)
    }
}
