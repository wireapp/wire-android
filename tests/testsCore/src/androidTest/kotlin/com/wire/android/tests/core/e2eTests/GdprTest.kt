/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
import user.utils.ClientUser

/*
This test works on the following conditions:
1) The dev/staging app is installed on the device/emulator.
*/
@RunWith(AndroidJUnit4::class)
class GdprTest : BaseUiTest() {
    override val deletePersonalUsersAfterTest = true

    private lateinit var personalUser: ClientUser
    private lateinit var teamMember: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8704", "TC-8132")
    @Category("regression", "RC", "gdpr")
    @Test
    fun givenTeamMember_whenAcceptingAnonymousDataSharing_thenAnalyticsInitializedIsSetToTrueAndIdentifierIsVisibleInDebugSettings() {
        step("Given There is a team owner user1Name with team GDPR and member user2Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GDPR",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "GDPR",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(teamMember.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and accept share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickAgreeShareDataAlert()
            }
        }

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When I open Privacy Settings from the main navigation menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.clickPrivacySettingsButtonOnSettingsPage()
        }

        // TC-8132 - I want to accept sending anonymous data as a team user
        step("Then I see send anonymous usage data switch is turned on") {
            pages.settingsPage.assertSendAnonymousUsageDataToggleIsOn()
        }

        step("When I return to Settings and open the debug menu") {
            pages.settingsPage.apply {
                clickBackButtonOnPrivacySettingsPage()
                clickDebugSettingsButton()
            }
        }

        step("Then I see analytics is initialized and my tracking identifier is visible") {
            pages.settingsPage.apply {
                assertAnalyticsInitializedIsSetToTrue()
                assertAnalyticsTrackingIdentifierIsDispayed()
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8705", "TC-8134")
    @Category("regression", "RC", "gdpr")
    @Test
    fun givenPersonalUser_whenAcceptingAnonymousDataSharing_thenAnalyticsInitializedIsSetToTrueAndIdentifierIsVisibleInDebugSettings() {
        step("Given There is a personal user user1Name and User user1Name is me") {
            clientUserManager.createPersonalUsersByAliases(listOf("user1Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(personalUser)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(personalUser.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and accept share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickAgreeShareDataAlert()
            }
        }

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When I open Privacy Settings from the main navigation menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.clickPrivacySettingsButtonOnSettingsPage()
        }

        // TC-8134 - I want to accept sending anonymous data as a personal user
        step("Then I see send anonymous usage data switch is turned on") {
            pages.settingsPage.assertSendAnonymousUsageDataToggleIsOn()
        }

        step("When I return to Settings and open the debug menu") {
            pages.settingsPage.apply {
                clickBackButtonOnPrivacySettingsPage()
                clickDebugSettingsButton()
            }
        }

        step("Then I see analytics is initialized and my tracking identifier is visible") {
            pages.settingsPage.apply {
                assertAnalyticsInitializedIsSetToTrue()
                assertAnalyticsTrackingIdentifierIsDispayed()
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8706", "TC-8133")
    @Category("regression", "RC", "gdpr")
    @Test
    fun givenTeamMember_whenDecliningAnonymousDataSharing_thenAnalyticsInitializedIsSetToFalseAndIdentifierIsVisibleInDebugSettings() {
        step("Given There is a team owner user1Name with team GDPR and member user2Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GDPR",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "GDPR",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(teamMember.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When I open Privacy Settings from the main navigation menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.clickPrivacySettingsButtonOnSettingsPage()
        }

        // TC-8133 - I want to decline sending anonymous data as a team user
        step("Then I see send anonymous usage data switch is turned off") {
            pages.settingsPage.assertSendAnonymousUsageDataToggleIsOff()
        }

        step("When I return to Settings and open the debug menu") {
            pages.settingsPage.apply {
                clickBackButtonOnPrivacySettingsPage()
                clickDebugSettingsButton()
            }
        }

        step("Then I see analytics initialized is set to false and my tracking identifier is visible") {
            pages.settingsPage.apply {
                assertAnalyticsInitializedIsSetToFalse()
                assertAnalyticsTrackingIdentifierIsDispayed()
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8707", "TC-8135")
    @Category("regression", "RC", "gdpr")
    @Test
    fun givenPersonalUser_whenDecliningAnonymousDataSharing_thenAnalyticsInitializedIsSetToFalseAndIdentifierIsVisibleInDebugSettings() {
        step("Given There is a personal user user1Name and User user1Name is me") {
            clientUserManager.createPersonalUsersByAliases(listOf("user1Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(personalUser)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(personalUser.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When I open Privacy Settings from the main navigation menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.clickPrivacySettingsButtonOnSettingsPage()
        }

        // TC-8135 - I want to decline sending anonymous data as a personal user
        step("Then I see send anonymous usage data switch is turned off") {
            pages.settingsPage.assertSendAnonymousUsageDataToggleIsOff()
        }

        step("When I return to Settings and open the debug menu") {
            pages.settingsPage.apply {
                clickBackButtonOnPrivacySettingsPage()
                clickDebugSettingsButton()
            }
        }

        step("Then I see analytics initialized is set to false and my tracking identifier is visible") {
            pages.settingsPage.apply {
                assertAnalyticsInitializedIsSetToFalse()
                assertAnalyticsTrackingIdentifierIsDispayed()
            }
        }
    }
}
