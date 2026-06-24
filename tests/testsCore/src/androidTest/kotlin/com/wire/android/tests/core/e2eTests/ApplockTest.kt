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
import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ApplockTest : BaseUiTest() {
    private lateinit var appPackage: String
    private var teamOwner: ClientUser? = null
    private var member1: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        appPackage = UiAutomatorSetup.appPackage
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8143")
    @Category("regression", "RC", "applock")
    @Test
    fun givenUserEnablesAppLock_whenAppIsBackgroundedForOneMinute_thenAppRequiresUnlockOnReturn() {
        step("Given there is TeamOwner with team AppLock and members on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "AppLock",
                "en_US",
                true,
                backendClient,
                context
            )

            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name,user5Name",
                "AppLock",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            teamOwner = teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("When I open staging deep link and login as TeamOwner") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamOwnerLoggingEmail(teamOwner?.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner?.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickAgreeShareDataAlert()
            }
        }

        step("Then I see conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
            }
        }

        step("When I open Settings from conversation list") {
            pages.conversationListPage.apply {
                UiWaitUtils.waitFor(2.seconds) // wait for websocket notification to disappear
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("When I confirm lock with passcode toggle is off and turn it on") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOff()
                turnOnLockWithPasscodeToggle()
            }
        }

        step("Then I see set up app lock page with inactivity description") {
            pages.settingsPage.apply {
                assertSetUpAppLockPageVisible()
                assertAppLockDescriptionText()
            }
        }

        step("When I enter my passcode for app lock and tap set passcode button") {
            val appLockPasscode = "A1a!".repeat(2)
            pages.settingsPage.apply {
                enterPasscode(appLockPasscode)
                tapSetPasscodeButton()
            }
        }

        step("Then I see lock with passcode toggle is turned on") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOn()
            }
        }

        step("When I go back to conversation list") {
            pages.settingsPage.apply {
                clickBackButtonOnSettingsPage()
            }
        }

        step("And I see conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
            }
        }

        step("And I minimise Wire for 63 seconds and restart it") {
            device.pressHome()
            UiWaitUtils.waitFor(63.seconds)
            device.executeShellCommand(
                "monkey -p $appPackage -c android.intent.category.LAUNCHER 1"
            )
        }

        step("And I unlock Wire from app lock page") {
            val appLockPasscode = "A1a!".repeat(2)
            pages.appLockPage.apply {
                assertAppLockPageVisible()
                enterPasscode(appLockPasscode)
                tapUnlockButtonOnAppLockPage()
            }
        }

        step("Then I see conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8144")
    @Category("regression", "RC", "applock")
    @Test
    fun givenUserEnablesAppLock_whenWrongPasscodeIsEntered_thenAppStaysLockedUntilCorrectPasscodeIsEntered() {
        step("Given there is TeamOwner with team AppLockInvalidPassphrase and Member1 on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "AppLockInvalidPassphrase",
                "en_US",
                true,
                backendClient,
                context
            )

            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "AppLockInvalidPassphrase",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            testServiceHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "AppLockInvalidPassphrase"
            )

            member1 = teamHelper.usersManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("When I open staging deep link and login as Member1") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamMemberLoggingEmail(member1?.email ?: "")
                clickLoginButton()
                enterTeamMemberLoggingPassword(member1?.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
            }
        }

        step("When I open Settings from conversation list") {
            pages.conversationListPage.apply {
                UiWaitUtils.waitFor(2.seconds) // wait for websocket notification to disappear
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("When I confirm lock with passcode toggle is off and turn it on") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOff()
                turnOnLockWithPasscodeToggle()
            }
        }

        step("Then I see set up app lock page with inactivity description") {
            pages.settingsPage.apply {
                assertSetUpAppLockPageVisible()
                assertAppLockDescriptionText()
            }
        }

        step("When I enter my passcode for app lock and tap set passcode button") {
            val appLockPasscode = "A1a!".repeat(2)
            pages.settingsPage.apply {
                enterPasscode(appLockPasscode)
                tapSetPasscodeButton()
            }
        }

        step("Then I see lock with passcode toggle is turned on") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOn()
            }
        }

        step("When I go back to conversation list") {
            pages.settingsPage.apply {
                clickBackButtonOnSettingsPage()
            }
        }

        step("And I see conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
            }
        }

        step("And I minimise Wire for 63 seconds and restart it") {
            device.pressHome()
            UiWaitUtils.waitFor(63.seconds)
            device.executeShellCommand(
                "monkey -p $appPackage -c android.intent.category.LAUNCHER 1"
            )
        }

        step("And I enter wrong passcode and tap unlock button on app lock page") {
            val wrongAppLockPasscode = "B2b@".repeat(2)
            pages.appLockPage.apply {
                assertAppLockPageVisible()
                enterPasscode(wrongAppLockPasscode)
                tapUnlockButtonOnAppLockPage()
            }
        }

        step("Then I see error message on app lock page") {
            pages.appLockPage.apply {
                assertWrongPasscodeErrorMessageVisible()
            }
        }

        step("And I do not see conversation list") {
            pages.conversationListPage.apply {
                assertConversationListNotVisible()
            }
        }

        step("When I clear the password field, enter correct passcode, and tap unlock button") {
            val appLockPasscode = "A1a!".repeat(2)
            pages.appLockPage.apply {
                clearPasscodeField()
                enterPasscode(appLockPasscode)
                tapUnlockButtonOnAppLockPage()
            }
        }

        step("Then I see conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8145", "TC-8146")
    @Category("regression", "RC", "applock")
    @Test
    fun givenTeamAppLockIsEnforced_whenTeamOwnerSetsPasscode_thenAppLockCannotBeChanged() {
        step("Given there is TeamOwner with team AppLockEnforced and Member1 on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "AppLockEnforced",
                "en_US",
                true,
                backendClient,
                context
            )

            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "AppLockEnforced",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            testServiceHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "AppLockEnforced"
            )

            teamOwner = teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("When I open staging deep link and login as TeamOwner") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamOwnerLoggingEmail(teamOwner?.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner?.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("When TeamOwner enables force app lock feature for team AppLockEnforced with timeout of 30 seconds") {
            teamHelper.enableForceAppLockFeature(
                "user1Name",
                "AppLockEnforced",
                30,
                backendClient
            )
        }

        step("Then I see alert informing me that my Team settings have changed") {
            pages.commonAppPage.apply {
                assertTeamSettingsChangedAlertVisible()
            }
        }

        step("And I see app lock mandatory subtext in the Team settings change alert") {
            pages.commonAppPage.apply {
                assertTeamSettingsChangedAlertSubtextVisible(
                    "App lock is now mandatory. Wire will lock itself after a certain time of inactivity"
                )
            }
        }

        step("And I tap OK button on the alert") {
            pages.commonAppPage.apply {
                tapOkButtonOnAlert()
            }
        }

        step("Then I see set up app lock page") {
            pages.settingsPage.apply {
                assertSetUpAppLockPageVisible()
            }
        }

        step("When I enter my passcode for app lock and tap set passcode button") {
            val appLockPasscode = "A1a!".repeat(2)
            pages.settingsPage.apply {
                enterPasscode(appLockPasscode)
                tapSetPasscodeButton()
            }
        }

        step("And I see conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
            }
        }

        // TC-8146 - I should not be able to switch off app lock if it is enforced for the team

        step("When I open Settings from conversation list") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                closeKeyboardIfOpened()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("Then I see lock with passcode toggle is turned on") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOn()
            }
        }

        step("And I see lock with passcode toggle can not be changed") {
            pages.settingsPage.apply {
                assertLockWithPasscodeToggleCannotBeChanged()
            }
        }
    }
}
