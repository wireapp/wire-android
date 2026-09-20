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
package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class SettingsTests : BaseUiTest() {
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4545")
    @Category("regression", "RC", "settings")
    @Test
    fun givenLoggedInTeamMember_whenIOpenAccountDetails_thenUserDetailsAreDisplayed() {
        step("Given There is a team owner user1Name with team MyAmazingTeam") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "MyAmazingTeam",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team MyAmazingTeam with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "MyAmazingTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And User user2Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        loginToStagingAs(member1)

        step("And I open Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("When I open my account details menu") {
            pages.settingsPage.tapAccountDetailsButton()
        }

        step("Then I see my profile name, username, email address, team name and domain") {
            pages.settingsPage.apply {
                verifyDisplayedProfileName(member1.name.orEmpty())
                verifyDisplayedUserName(member1.uniqueUsername.orEmpty())
                verifyDisplayedEmailAddress(member1.email.orEmpty())
                verifyDisplayedTeamName("MyAmazingTeam")
                verifyDisplayedDomain("staging.zinfra.io")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4543")
    @Category("regression", "RC", "settings")
    @Test
    fun givenLoggedInTeamMember_whenITapResetPassword_thenAccountRecoveryPageIsDisplayed() {
        step("Given There is a team owner user1Name with team ResetPassword") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ResetPassword",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team ResetPassword with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ResetPassword",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("When I open Account Details from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.tapAccountDetailsButton()
        }

        step("Then I see reset password button") {
            pages.settingsPage.assertResetPasswordButtonIsDisplayed()
        }

        step("When I tap reset password button") {
            pages.settingsPage.tapResetPasswordButton()
        }

        step("Then Wire is not in foreground and I see the account recovery webpage") {
            pages.commonAppPage.assertWireAppIsNotInForeground()
            pages.chromePage.apply {
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
            }
            pages.settingsPage.assertChromeUrlIsDisplayed("wire-account-staging.zinfra.io")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4544")
    @Category("regression", "RC", "settings")
    @Test
    fun givenLoggedInPersonalUser_whenITapResetPassword_thenAccountRecoveryPageIsDisplayed() {
        step("Given There is a personal user user1Name") {
            clientUserManager.createPersonalUsersByAliases(listOf("user1Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
        }

        step("And User user1Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("When I open Account Details from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.tapAccountDetailsButton()
        }

        step("Then I see reset password button") {
            pages.settingsPage.assertResetPasswordButtonIsDisplayed()
        }

        step("When I tap reset password button") {
            pages.settingsPage.tapResetPasswordButton()
        }

        step("Then Wire is not in foreground and I see the account recovery webpage") {
            pages.commonAppPage.assertWireAppIsNotInForeground()
            pages.chromePage.apply {
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
            }
            pages.settingsPage.assertChromeUrlIsDisplayed("wire-account-staging.zinfra.io")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4546")
    @Category("regression", "RC", "settings")
    @Test
    fun givenLoggedInTeamMember_whenIChangeMyProfileName_thenNewProfileNameIsDisplayed() {
        step("Given There is a team owner user1Name with team ChangeUserName") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ChangeUserName",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team ChangeUserName with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ChangeUserName",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I open Account Details from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.tapAccountDetailsButton()
        }

        step("And I see my profile name user2Name is displayed") {
            pages.settingsPage.verifyDisplayedProfileName(member1.name.orEmpty())
        }

        step("When I tap on my profile name user2Name in Account Details") {
            pages.settingsPage.tapDisplayedProfileName(member1.name.orEmpty())
        }

        step("Then I see edit profile name page") {
            pages.settingsPage.assertEditProfileNamePageIsDisplayed()
        }

        step("When I edit my profile name to ThisIsMyNewName and save it") {
            pages.settingsPage.apply {
                editProfileName("ThisIsMyNewName")
                clickSaveButton()
            }
        }

        step("Then I see the success message and my new profile name") {
            waitUntilToastIsDisplayed("Your profile name changed")
            pages.settingsPage.verifyDisplayedProfileName("ThisIsMyNewName")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4540")
    @Category("regression", "RC", "settings")
    @Test
    fun givenLoggedInTeamMember_whenIOpenReportBug_thenAndroidShareSheetIsDisplayed() {
        step("Given There is a team owner user1Name with team ResetPassword") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ResetPassword",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team ResetPassword with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ResetPassword",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I open Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("When I open report a bug menu") {
            pages.settingsPage.openReportBugMenu()
        }

        step("Then I see the app drawer where I can share my bug report") {
            pages.settingsPage.assertShareSheetIsDisplayed()
        }

        step("And I tap back button") {
            device.pressBack()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4541", "TC-4542")
    @Category("regression", "RC", "settings")
    @Test
    fun givenLoggedInTeamMember_whenIToggleWebsocketConnection_thenSwitchChangesFromOnToOff() {
        step("Given There is a team owner user1Name with team NetworkSettings") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "NetworkSettings",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team NetworkSettings with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "NetworkSettings",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I open Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("When I tap Network Settings menu") {
            pages.settingsPage.tapNetworkSettingsButton()
        }

        step("Then I see Websocket switch is at ON state") {
            pages.settingsPage.assertWebsocketSwitchState("ON")
        }

        step("When I minimise Wire and open the notification center") {
            device.pressHome()
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I see the message that my Websocket connection is running") {
            pages.notificationsPage.assertWebsocketServiceNotificationVisible()
        }

        step("When I close the notification center and restart Wire") {
            pages.notificationsPage.closeNotificationCenter()
            pages.commonAppPage.restartWireApp()
        }

        step("And I see Websocket switch is still at ON state") {
            pages.settingsPage.assertWebsocketSwitchState("ON")
        }

        step("When I tap Websocket Connection button") {
            pages.settingsPage.tapWebsocketConnectionButton()
        }

        step("Then I see Websocket switch is at OFF state") {
            pages.settingsPage.assertWebsocketSwitchState("OFF")
        }
    }

    // Keeps the repeated staging login flow consistent across settings scenarios.
    private fun loginToStagingAs(user: ClientUser) {
        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link and proceed to login") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterUserIdentifier(user.email.orEmpty())
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterUserPassword(user.password.orEmpty())
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.waitUntilConversationPageVisibleDismissingPostLoginPrompts()
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }
    }
}
