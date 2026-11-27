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

import SSOServiceHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.Tag
import com.wire.android.tests.support.tags.TestCaseId
import keycloak.KeycloakApiClient
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import user.utils.component.getUserId
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class ApplockTest : BaseUiTest() {
    private lateinit var appPackage: String
    private var teamOwner: ClientUser? = null
    private var member1: ClientUser? = null
    private var keycloakApiClient: KeycloakApiClient? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        appPackage = UiAutomatorSetup.appPackage
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12672")
    @Category("regression", "RC", "applock")
    @Test
    fun givenUserEnablesAppLock_whenAppIsBackgroundedForOneMinute_thenAppRequiresUnlockOnReturn() {
        step("Given there is TeamOwner with team AppLock and members on Staging backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AppLock",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "AppLock", backendClient)
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name,user5Name",
                "AppLock",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
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
                clickContinueButtonOnBackendConfigSuccess()
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
    @TestCaseId("TC-12673")
    @Category("regression", "RC", "applock")
    @Test
    fun givenUserEnablesAppLock_whenWrongPasscodeIsEntered_thenAppStaysLockedUntilCorrectPasscodeIsEntered() {
        step("Given there is TeamOwner with team AppLockInvalidPassphrase and Member1 on Staging backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AppLockInvalidPassphrase",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "AppLockInvalidPassphrase", backendClient)

            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "AppLockInvalidPassphrase",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "AppLockInvalidPassphrase"
            )

            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
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
                clickContinueButtonOnBackendConfigSuccess()
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
    @TestCaseId("TC-12768")
    @Category("regression", "RC", "applock", "security")
    @Tag(key = "feature", value = "appLockColdStart")
    @Test
    fun givenAppLockIsEnabled_whenAppIsColdStarted_thenUnlockGateIsShownBeforeConversationList() {
        val teamName = "AppLockColdStart"
        val appLockPasscode = "A1a!A1a!"

        step("Given there is a team owner on the Staging backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                teamName,
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", teamName, backendClient)
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("When I open the staging deep link and log in as the team owner") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
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

        step("And I enable app lock with a passcode") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                UiWaitUtils.waitFor(2.seconds)
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOff()
                turnOnLockWithPasscodeToggle()
                assertSetUpAppLockPageVisible()
                enterPasscode(appLockPasscode)
                tapSetPasscodeButton()
                assertLockWithPasswordToggleIsOn()
                clickBackButtonOnSettingsPage()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        repeat(COLD_START_REPETITIONS) { attempt ->
            step("When I force-stop and cold-start Wire, attempt ${attempt + 1}") {
                UiAutomatorSetup.stopApp()
                device.executeShellCommand(
                    "monkey -p $appPackage -c android.intent.category.LAUNCHER 1"
                )
            }

            step("Then the unlock gate is visible before conversation content, attempt ${attempt + 1}") {
                pages.appLockPage.assertAppLockGateVisibleBefore(
                    otherScreen = pages.conversationListPage.conversationListHeadingSelector(),
                    otherScreenName = "Conversation list"
                )
                pages.conversationListPage.assertConversationListNotVisible()
            }

            step("When I unlock Wire with the app lock passcode, attempt ${attempt + 1}") {
                pages.appLockPage.apply {
                    tapUsePasscodeOnBiometricPromptIfVisible()
                    assertAppLockPageVisible()
                    enterPasscode(appLockPasscode)
                    tapUnlockButtonOnAppLockPage()
                }
            }

            step("Then I see the conversation list, attempt ${attempt + 1}") {
                pages.conversationListPage.assertConversationListVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12674", "TC-12675")
    @Category("regression", "RC", "applock")
    @Test
    fun givenTeamAppLockIsEnforced_whenTeamOwnerSetsPasscode_thenAppLockCannotBeChanged() {
        step("Given there is TeamOwner with team AppLockEnforced and Member1 on Staging backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AppLockEnforced",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "AppLockEnforced", backendClient)

            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "AppLockEnforced",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "AppLockEnforced"
            )

            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
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
                clickContinueButtonOnBackendConfigSuccess()
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
            backendSetupHelper.enableForceAppLockFeature(
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

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12813")
    @Category("regression", "RC", "applock")
    @Test
    fun givenUserEnabledAppLock_whenForgottenPasscodeIsRecoveredWithPasswordLogin_thenHistoryIsRetained() {

        step("Given TeamOwner creates team AppLockRecovery on Staging") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AppLockRecovery",
                "en_US",
                true,
                backendClient,
                context
            )
        }
        step("And TeamOwner configures MLS for AppLockRecovery") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "AppLockRecovery", backendClient)
        }
        step("And TeamOwner adds Member1 to AppLockRecovery with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "AppLockRecovery",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And TeamOwner creates MLS group Conversation AppLockRecovery with Member1") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "AppLockRecovery",
                deviceName = "Device1"
            )
        }
        step("And TeamOwner is the current user") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(requireNotNull(teamOwner))
        }
        step("And TeamOwner logs in with a password and completes the login prompts") {
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(requireNotNull(teamOwner))
            pages.registrationPage.clickDeclineShareDataAlert()
            pages.conversationListPage.assertConversationListVisible()
        }
        step("And TeamOwner sees Conversation AppLockRecovery in the conversation list and opens it") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("AppLockRecovery")
                tapConversationNameInConversationList("AppLockRecovery")
            }
        }
        step("And TeamOwner sends a message in Conversation AppLockRecovery and verifies it is visible") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("TeamOwner history before app-lock recovery")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
            }
        }
        step("And Member1 sends a reply that TeamOwner sees in Conversation AppLockRecovery") {
            testServiceHelper.userSendsGenericMessageToConversation(
                "user2Name",
                "AppLockRecovery",
                "Device1",
                "Member1 history before app-lock recovery"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
        }
        step("And TeamOwner returns to the conversation list and opens Settings from the conversation list menu") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }
        step("And TeamOwner verifies app lock is OFF, opens app-lock setup and sets the original passcode") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOff()
                turnOnLockWithPasscodeToggle()
                assertSetUpAppLockPageVisible()
                enterPasscode(requireNotNull(teamOwner?.password))
                tapSetPasscodeButton()
            }
        }
        step("And TeamOwner verifies app lock is ON and returns to the conversation list") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOn()
                clickBackButtonOnSettingsPage()
            }
        }
        step("When TeamOwner cold-starts Wire and selects Forgot your passcode on the unlock screen") {
            UiAutomatorSetup.stopApp()
            device.executeShellCommand("monkey -p $appPackage -c android.intent.category.LAUNCHER 1")
            pages.appLockPage.apply {
                assertAppLockGateVisible()
                tapUsePasscodeOnBiometricPromptIfVisible()
                assertAppLockPageVisible()
                tapForgotPasscodeButton()
            }
        }
        step("And TeamOwner logs out from recovery with clear data unchecked") {
            pages.appLockPage.apply {
                tapLogoutButtonOnRecoveryScreen()
                confirmLogoutWithoutClearingData()
            }
        }
        step("And TeamOwner logs back in with the same account password") {
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(requireNotNull(teamOwner))
        }
        step("Then the conversation list opens without mandatory app-lock setup and both original messages remain") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("AppLockRecovery")
                tapConversationNameInConversationList("AppLockRecovery")
            }
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
                assertReceivedMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
                tapBackButtonToCloseConversationViewPage()
            }
        }
        step("When TeamOwner opens Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }
        step("And TeamOwner verifies app lock is OFF, sets a new passcode, verifies it is ON and returns to the conversation list") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOff()
                turnOnLockWithPasscodeToggle()
                assertSetUpAppLockPageVisible()
                enterPasscode("${requireNotNull(teamOwner?.password)}!New")
                tapSetPasscodeButton()
                assertLockWithPasswordToggleIsOn()
                clickBackButtonOnSettingsPage()
            }
        }
        step("And TeamOwner cold-starts Wire and verifies the app-lock gate is visible") {
            UiAutomatorSetup.stopApp()
            device.executeShellCommand("monkey -p $appPackage -c android.intent.category.LAUNCHER 1")
            pages.appLockPage.apply {
                assertAppLockGateVisible()
            }
        }
        step("And TeamOwner opens the passcode screen, enters the original passcode and taps Unlock") {
            pages.appLockPage.apply {
                tapUsePasscodeOnBiometricPromptIfVisible()
                assertAppLockPageVisible()
                enterPasscode(requireNotNull(teamOwner?.password))
                tapUnlockButtonOnAppLockPage()
            }
        }
        step("Then the original passcode is rejected and conversation content stays hidden") {
            pages.appLockPage.assertWrongPasscodeErrorMessageVisible()
            pages.conversationListPage.assertConversationListNotVisible()
        }
        step("When TeamOwner enters the new passcode") {
            pages.appLockPage.apply {
                clearPasscodeField()
                enterPasscode("${requireNotNull(teamOwner?.password)}!New")
                tapUnlockButtonOnAppLockPage()
            }
        }
        step("Then Wire unlocks and both original messages remain in Conversation AppLockRecovery") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                tapConversationNameInConversationList("AppLockRecovery")
            }
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
                assertReceivedMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-13073")
    @Category("regression", "RC", "applock")
    @Test
    fun givenTeamEnforcedAppLock_whenForgottenPasscodeIsRecoveredWithPasswordLogin_thenNewPasscodeIsRequiredAndHistoryIsRetained() {

        step("Given TeamOwner creates team EnforcedAppLockRecovery on Staging") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "EnforcedAppLockRecovery",
                "en_US",
                true,
                backendClient,
                context
            )
        }
        step("And TeamOwner configures MLS for EnforcedAppLockRecovery") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "EnforcedAppLockRecovery", backendClient)
        }
        step("And TeamOwner adds Member1 to EnforcedAppLockRecovery with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "EnforcedAppLockRecovery",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And TeamOwner creates MLS group Conversation EnforcedAppLockRecovery with Member1") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "EnforcedAppLockRecovery",
                deviceName = "Device1"
            )
        }
        step("And TeamOwner enforces app lock for team EnforcedAppLockRecovery with a 30-second timeout") {
            backendSetupHelper.enableForceAppLockFeature(
                "user1Name",
                "EnforcedAppLockRecovery",
                30,
                backendClient
            )
        }
        step("And TeamOwner is the current user") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(requireNotNull(teamOwner))
        }
        step("And TeamOwner logs in with a password to the app-lock-enforced team") {
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(requireNotNull(teamOwner))
            pages.registrationPage.clickDeclineShareDataAlert()
        }
        step("And TeamOwner sees the mandatory app-lock policy alert and taps OK") {
            pages.commonAppPage.apply {
                assertTeamSettingsChangedAlertVisible()
                assertTeamSettingsChangedAlertSubtextVisible(
                    "App lock is now mandatory. Wire will lock itself after a certain time of inactivity"
                )
                tapOkButtonOnAlert()
            }
        }
        step("Then app-lock setup is mandatory before TeamOwner can see the conversation list") {
            pages.settingsPage.assertSetUpAppLockPageVisible()
            pages.conversationListPage.assertConversationListNotVisible()
        }
        step("When TeamOwner sets the original passcode in the mandatory app-lock setup") {
            pages.settingsPage.apply {
                enterPasscode(requireNotNull(teamOwner?.password))
                tapSetPasscodeButton()
            }
        }
        step("And TeamOwner sees the conversation list after setting the mandatory passcode") {
            pages.conversationListPage.assertConversationListVisible()
        }
        step("And TeamOwner sees Conversation EnforcedAppLockRecovery in the conversation list and opens it") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("EnforcedAppLockRecovery")
                tapConversationNameInConversationList("EnforcedAppLockRecovery")
            }
        }
        step("And TeamOwner sends a message in Conversation EnforcedAppLockRecovery and verifies it is visible") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("TeamOwner history before app-lock recovery")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
            }
        }
        step("And Member1 sends a reply that TeamOwner sees in Conversation EnforcedAppLockRecovery") {
            testServiceHelper.userSendsGenericMessageToConversation(
                "user2Name",
                "EnforcedAppLockRecovery",
                "Device1",
                "Member1 history before app-lock recovery"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
        }
        step("And TeamOwner returns to the conversation list and opens Settings from the conversation list menu") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }
        step("And TeamOwner verifies app lock is ON and cannot be changed, then returns to the conversation list") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOn()
                assertLockWithPasscodeToggleCannotBeChanged()
                clickBackButtonOnSettingsPage()
            }
        }
        step("When TeamOwner cold-starts Wire and selects Forgot your passcode on the unlock screen") {
            UiAutomatorSetup.stopApp()
            device.executeShellCommand("monkey -p $appPackage -c android.intent.category.LAUNCHER 1")
            pages.appLockPage.apply {
                assertAppLockGateVisible()
                tapUsePasscodeOnBiometricPromptIfVisible()
                assertAppLockPageVisible()
                tapForgotPasscodeButton()
            }
        }
        step("And TeamOwner logs out from recovery with clear data unchecked") {
            pages.appLockPage.apply {
                tapLogoutButtonOnRecoveryScreen()
                confirmLogoutWithoutClearingData()
            }
        }
        step("And TeamOwner logs back in with the same account password") {
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(requireNotNull(teamOwner))
        }
        step("And TeamOwner sees the mandatory app-lock policy alert after recovery login and taps OK") {
            pages.commonAppPage.apply {
                assertTeamSettingsChangedAlertVisible()
                assertTeamSettingsChangedAlertSubtextVisible(
                    "App lock is now mandatory. Wire will lock itself after a certain time of inactivity"
                )
                tapOkButtonOnAlert()
            }
        }
        step("Then app-lock setup is mandatory again and the conversation list is not visible") {
            pages.settingsPage.assertSetUpAppLockPageVisible()
            pages.conversationListPage.assertConversationListNotVisible()
        }
        step("When TeamOwner sets a different passcode in the mandatory app-lock setup") {
            pages.settingsPage.apply {
                enterPasscode("${requireNotNull(teamOwner?.password)}!New")
                tapSetPasscodeButton()
            }
        }
        step("Then TeamOwner opens Conversation EnforcedAppLockRecovery and sees both original messages") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("EnforcedAppLockRecovery")
                tapConversationNameInConversationList("EnforcedAppLockRecovery")
            }
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
                assertReceivedMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
                tapBackButtonToCloseConversationViewPage()
            }
        }
        step("And TeamOwner cold-starts Wire and verifies the app-lock gate is visible") {
            UiAutomatorSetup.stopApp()
            device.executeShellCommand("monkey -p $appPackage -c android.intent.category.LAUNCHER 1")
            pages.appLockPage.apply {
                assertAppLockGateVisible()
            }
        }
        step("And TeamOwner opens the passcode screen, enters the original passcode and taps Unlock") {
            pages.appLockPage.apply {
                tapUsePasscodeOnBiometricPromptIfVisible()
                assertAppLockPageVisible()
                enterPasscode(requireNotNull(teamOwner?.password))
                tapUnlockButtonOnAppLockPage()
            }
        }
        step("Then the original passcode is rejected and conversation content stays hidden") {
            pages.appLockPage.assertWrongPasscodeErrorMessageVisible()
            pages.conversationListPage.assertConversationListNotVisible()
        }
        step("When TeamOwner enters the new passcode") {
            pages.appLockPage.apply {
                clearPasscodeField()
                enterPasscode("${requireNotNull(teamOwner?.password)}!New")
                tapUnlockButtonOnAppLockPage()
            }
        }
        step("Then Wire unlocks and both original messages remain in Conversation EnforcedAppLockRecovery") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                tapConversationNameInConversationList("EnforcedAppLockRecovery")
            }
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
                assertReceivedMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-13074")
    @Category("regression", "RC", "applock")
    @Test
    fun givenTeamEnforcedAppLock_whenForgottenPasscodeIsRecoveredWithSsoLogin_thenNewPasscodeIsRequiredAndHistoryIsRetained() {

        step("Given TeamOwner creates Keycloak SSO team EnforcedSsoAppLockRecovery on Staging") {
            SSOServiceHelper.initialize(clientUserManager)
            keycloakApiClient = KeycloakApiClient(backendClient)
            runBlocking {
                SSOServiceHelper.createKeycloakSsoTeamOwner(
                    context,
                    "user1Name",
                    "EnforcedSsoAppLockRecovery",
                    requireNotNull(keycloakApiClient)
                )
            }
        }
        step("And TeamOwner configures MLS for EnforcedSsoAppLockRecovery") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "EnforcedSsoAppLockRecovery", backendClient)
        }
        step("And TeamOwner provisions Member1 in Keycloak for SSO login") {
            runBlocking {
                SSOServiceHelper.addKeycloakSsoUsers("user1Name", "user2Name", requireNotNull(keycloakApiClient))
            }
        }
        step("And TeamOwner enforces app lock for team EnforcedSsoAppLockRecovery with a 30-second timeout") {
            backendSetupHelper.enableForceAppLockFeature(
                "user1Name",
                "EnforcedSsoAppLockRecovery",
                30,
                backendClient
            )
        }
        step("And Member1 is the current SSO user") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            SSOServiceHelper.setCurrentSsoUser("user2Name")
        }
        step("When Member1 opens Staging SSO login and signs in through Keycloak") {
            pages.registrationPage.assertEmailWelcomePage()
            openStagingSsoLogin()
            pages.ssoPage.apply {
                waitUntilKeycloakPageLoaded()
                enterKeycloakEmail(requireNotNull(member1?.email))
                closeKeyboardIfOpened()
                enterKeycloakPassword(requireNotNull(member1?.password))
                closeKeyboardIfOpened()
                tapKeycloakSignIn()
            }
        }
        step("And Member1 sees the mandatory app-lock policy alert and taps OK") {
            pages.commonAppPage.apply {
                assertTeamSettingsChangedAlertVisible()
                assertTeamSettingsChangedAlertSubtextVisible(
                    "App lock is now mandatory. Wire will lock itself after a certain time of inactivity"
                )
                tapOkButtonOnAlert()
            }
        }
        step("Then app-lock setup is mandatory before Member1 can see the conversation list") {
            pages.settingsPage.assertSetUpAppLockPageVisible()
            pages.conversationListPage.assertConversationListNotVisible()
        }
        step("When Member1 sets the original passcode in the mandatory app-lock setup") {
            pages.settingsPage.apply {
                enterPasscode(requireNotNull(member1?.password))
                tapSetPasscodeButton()
            }
        }
        step("And Member1 completes username setup and declines analytics consent") {
            pages.registrationPage.apply {
                assertEnterYourUserNameInfoText()
                setUserName(requireNotNull(member1?.uniqueUsername))
                clickConfirmButton()
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }
        step("And Member1 sees the conversation list after setting the mandatory passcode") {
            pages.conversationListPage.assertConversationListVisible()
        }
        step("And TeamOwner resolves Member1's Wire user ID after the first SSO login") {
            requireNotNull(requireNotNull(member1).getUserId())
        }
        step("And TeamOwner registers a remote MLS device to create the Conversation and send replies") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }
        step("And TeamOwner creates MLS group Conversation EnforcedSsoAppLockRecovery with Member1") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "EnforcedSsoAppLockRecovery",
                deviceName = "Device1"
            )
        }
        step("And Member1 sees Conversation EnforcedSsoAppLockRecovery in the conversation list and opens it") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("EnforcedSsoAppLockRecovery")
                tapConversationNameInConversationList("EnforcedSsoAppLockRecovery")
            }
        }
        step("And Member1 sends a message in Conversation EnforcedSsoAppLockRecovery and verifies it is visible") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Member1 history before app-lock recovery")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
            }
        }
        step("And TeamOwner sends a reply that Member1 sees in Conversation EnforcedSsoAppLockRecovery") {
            testServiceHelper.userSendsGenericMessageToConversation(
                "user1Name",
                "EnforcedSsoAppLockRecovery",
                "Device1",
                "TeamOwner history before app-lock recovery"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
        }
        step("And Member1 returns to the conversation list and opens Settings from the conversation list menu") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }
        step("And Member1 verifies app lock is ON and cannot be changed, then returns to the conversation list") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOn()
                assertLockWithPasscodeToggleCannotBeChanged()
                clickBackButtonOnSettingsPage()
            }
        }
        step("When Member1 cold-starts Wire and selects Forgot your passcode on the unlock screen") {
            UiAutomatorSetup.stopApp()
            device.executeShellCommand("monkey -p $appPackage -c android.intent.category.LAUNCHER 1")
            pages.appLockPage.apply {
                assertAppLockGateVisible()
                tapUsePasscodeOnBiometricPromptIfVisible()
                assertAppLockPageVisible()
                tapForgotPasscodeButton()
            }
        }
        step("And Member1 logs out from recovery with clear data unchecked") {
            pages.appLockPage.apply {
                tapLogoutButtonOnRecoveryScreen()
                confirmLogoutWithoutClearingData()
            }
        }
        step("And Member1 logs back in through the same Keycloak SSO provider") {
            pages.registrationPage.assertEmailWelcomePage()
            openStagingSsoLogin()
            pages.ssoPage.signInToKeycloakIfRequested(
                requireNotNull(member1?.email),
                requireNotNull(member1?.password)
            )
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }
        step("And Member1 sees the mandatory app-lock policy alert after recovery login and taps OK") {
            pages.commonAppPage.apply {
                assertTeamSettingsChangedAlertVisible()
                assertTeamSettingsChangedAlertSubtextVisible(
                    "App lock is now mandatory. Wire will lock itself after a certain time of inactivity"
                )
                tapOkButtonOnAlert()
            }
        }
        step("Then app-lock setup is mandatory again and the conversation list is not visible") {
            pages.settingsPage.assertSetUpAppLockPageVisible()
            pages.conversationListPage.assertConversationListNotVisible()
        }
        step("When Member1 sets a different passcode in the mandatory app-lock setup") {
            pages.settingsPage.apply {
                enterPasscode("${requireNotNull(member1?.password)}!New")
                tapSetPasscodeButton()
            }
        }
        step("Then Member1 opens Conversation EnforcedSsoAppLockRecovery and sees both original messages") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("EnforcedSsoAppLockRecovery")
                tapConversationNameInConversationList("EnforcedSsoAppLockRecovery")
            }
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
                assertReceivedMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
                tapBackButtonToCloseConversationViewPage()
            }
        }
        step("And Member1 cold-starts Wire and verifies the app-lock gate is visible") {
            UiAutomatorSetup.stopApp()
            device.executeShellCommand("monkey -p $appPackage -c android.intent.category.LAUNCHER 1")
            pages.appLockPage.apply {
                assertAppLockGateVisible()
            }
        }
        step("And Member1 opens the passcode screen, enters the original passcode and taps Unlock") {
            pages.appLockPage.apply {
                tapUsePasscodeOnBiometricPromptIfVisible()
                assertAppLockPageVisible()
                enterPasscode(requireNotNull(member1?.password))
                tapUnlockButtonOnAppLockPage()
            }
        }
        step("Then the original passcode is rejected and conversation content stays hidden") {
            pages.appLockPage.assertWrongPasscodeErrorMessageVisible()
            pages.conversationListPage.assertConversationListNotVisible()
        }
        step("When Member1 enters the new passcode") {
            pages.appLockPage.apply {
                clearPasscodeField()
                enterPasscode("${requireNotNull(member1?.password)}!New")
                tapUnlockButtonOnAppLockPage()
            }
        }
        step("Then Wire unlocks and both original messages remain in Conversation EnforcedSsoAppLockRecovery") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                tapConversationNameInConversationList("EnforcedSsoAppLockRecovery")
            }
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
                assertReceivedMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-13075")
    @Category("regression", "RC", "applock")
    @Test
    fun givenUserEnabledAppLock_whenForgottenPasscodeIsRecoveredWithSsoLogin_thenHistoryIsRetained() {

        step("Given TeamOwner creates Keycloak SSO team SsoAppLockRecovery on Staging") {
            SSOServiceHelper.initialize(clientUserManager)
            keycloakApiClient = KeycloakApiClient(backendClient)
            runBlocking {
                SSOServiceHelper.createKeycloakSsoTeamOwner(
                    context,
                    "user1Name",
                    "SsoAppLockRecovery",
                    requireNotNull(keycloakApiClient)
                )
            }
        }
        step("And TeamOwner configures MLS for SsoAppLockRecovery") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "SsoAppLockRecovery", backendClient)
        }
        step("And TeamOwner provisions Member1 in Keycloak for SSO login") {
            runBlocking {
                SSOServiceHelper.addKeycloakSsoUsers("user1Name", "user2Name", requireNotNull(keycloakApiClient))
            }
        }
        step("And Member1 is the current SSO user") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            SSOServiceHelper.setCurrentSsoUser("user2Name")
        }
        step("When Member1 opens Staging SSO login and signs in through Keycloak") {
            pages.registrationPage.assertEmailWelcomePage()
            openStagingSsoLogin()
            pages.ssoPage.apply {
                waitUntilKeycloakPageLoaded()
                enterKeycloakEmail(requireNotNull(member1?.email))
                closeKeyboardIfOpened()
                enterKeycloakPassword(requireNotNull(member1?.password))
                closeKeyboardIfOpened()
                tapKeycloakSignIn()
            }
        }
        step("And Member1 completes username setup and declines analytics consent") {
            pages.registrationPage.apply {
                assertEnterYourUserNameInfoText()
                setUserName(requireNotNull(member1?.uniqueUsername))
                clickConfirmButton()
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }
        step("Then Member1 reaches the conversation list without mandatory app-lock setup") {
            pages.conversationListPage.assertConversationListVisible()
        }
        step("And TeamOwner resolves Member1's Wire user ID after the first SSO login") {
            requireNotNull(requireNotNull(member1).getUserId())
        }
        step("And TeamOwner registers a remote MLS device to create the Conversation and send replies") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }
        step("And TeamOwner creates MLS group Conversation SsoAppLockRecovery with Member1") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "SsoAppLockRecovery",
                deviceName = "Device1"
            )
        }
        step("And Member1 sees Conversation SsoAppLockRecovery in the conversation list and opens it") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SsoAppLockRecovery")
                tapConversationNameInConversationList("SsoAppLockRecovery")
            }
        }
        step("And Member1 sends a message in Conversation SsoAppLockRecovery and verifies it is visible") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Member1 history before app-lock recovery")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
            }
        }
        step("And TeamOwner sends a reply that Member1 sees in Conversation SsoAppLockRecovery") {
            testServiceHelper.userSendsGenericMessageToConversation(
                "user1Name",
                "SsoAppLockRecovery",
                "Device1",
                "TeamOwner history before app-lock recovery"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
        }
        step("And Member1 returns to the conversation list and opens Settings from the conversation list menu") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }
        step("And Member1 verifies app lock is OFF, opens app-lock setup and sets the original passcode") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOff()
                turnOnLockWithPasscodeToggle()
                assertSetUpAppLockPageVisible()
                enterPasscode(requireNotNull(member1?.password))
                tapSetPasscodeButton()
            }
        }
        step("And Member1 verifies app lock is ON and returns to the conversation list") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOn()
                clickBackButtonOnSettingsPage()
            }
        }
        step("When Member1 cold-starts Wire and selects Forgot your passcode on the unlock screen") {
            UiAutomatorSetup.stopApp()
            device.executeShellCommand("monkey -p $appPackage -c android.intent.category.LAUNCHER 1")
            pages.appLockPage.apply {
                assertAppLockGateVisible()
                tapUsePasscodeOnBiometricPromptIfVisible()
                assertAppLockPageVisible()
                tapForgotPasscodeButton()
            }
        }
        step("And Member1 logs out from recovery with clear data unchecked") {
            pages.appLockPage.apply {
                tapLogoutButtonOnRecoveryScreen()
                confirmLogoutWithoutClearingData()
            }
        }
        step("And Member1 logs back in through the same Keycloak SSO provider") {
            pages.registrationPage.assertEmailWelcomePage()
            openStagingSsoLogin()
            pages.ssoPage.signInToKeycloakIfRequested(
                requireNotNull(member1?.email),
                requireNotNull(member1?.password)
            )
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }
        step("Then Member1 reaches the conversation list without mandatory setup and sees both original messages") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("SsoAppLockRecovery")
                tapConversationNameInConversationList("SsoAppLockRecovery")
            }
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
                assertReceivedMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
                tapBackButtonToCloseConversationViewPage()
            }
        }
        step("When Member1 opens Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }
        step("And Member1 verifies app lock is OFF, sets a new passcode, verifies it is ON and returns to the conversation list") {
            pages.settingsPage.apply {
                assertLockWithPasswordToggleIsOff()
                turnOnLockWithPasscodeToggle()
                assertSetUpAppLockPageVisible()
                enterPasscode("${requireNotNull(member1?.password)}!New")
                tapSetPasscodeButton()
                assertLockWithPasswordToggleIsOn()
                clickBackButtonOnSettingsPage()
            }
        }
        step("And Member1 cold-starts Wire and verifies the app-lock gate is visible") {
            UiAutomatorSetup.stopApp()
            device.executeShellCommand("monkey -p $appPackage -c android.intent.category.LAUNCHER 1")
            pages.appLockPage.apply {
                assertAppLockGateVisible()
            }
        }
        step("And Member1 opens the passcode screen, enters the original passcode and taps Unlock") {
            pages.appLockPage.apply {
                tapUsePasscodeOnBiometricPromptIfVisible()
                assertAppLockPageVisible()
                enterPasscode(requireNotNull(member1?.password))
                tapUnlockButtonOnAppLockPage()
            }
        }
        step("Then the original passcode is rejected and conversation content stays hidden") {
            pages.appLockPage.assertWrongPasscodeErrorMessageVisible()
            pages.conversationListPage.assertConversationListNotVisible()
        }
        step("When Member1 enters the new passcode") {
            pages.appLockPage.apply {
                clearPasscodeField()
                enterPasscode("${requireNotNull(member1?.password)}!New")
                tapUnlockButtonOnAppLockPage()
            }
        }
        step("Then Wire unlocks and both original messages remain in Conversation SsoAppLockRecovery") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                tapConversationNameInConversationList("SsoAppLockRecovery")
            }
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Member1 history before app-lock recovery")
                assertReceivedMessageIsVisibleInCurrentConversation("TeamOwner history before app-lock recovery")
            }
        }
    }

    @After
    fun cleanUpSsoFixtures() {
        keycloakApiClient?.cleanUp()
    }

    // Open the configured IdP; first login and recovery handle their own post-login screens.
    private fun openStagingSsoLogin() {
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
            clickContinueButtonOnBackendConfigSuccess()
            enterSSOCodeOnSSOLoginTab(SSOServiceHelper.getSsoCode())
            clickLoginButton()
        }
        pages.chromePage.dismissFirstRunIfVisible()
    }

    // Shared password login flow; analytics consent is handled only on the first login.
    private fun loginToStagingAs(user: ClientUser) {
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
            clickContinueButtonOnBackendConfigSuccess()
            enterUserIdentifier(user.email.orEmpty())
            clickLoginButton()
            enterUserPassword(user.password.orEmpty())
            clickLoginButton()
        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsCompleted()
            clickAllowNotificationButton()
        }
        pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
    }

    private companion object {
        const val COLD_START_REPETITIONS = 3
    }
}
