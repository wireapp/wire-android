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
import androidx.test.platform.app.InstrumentationRegistry
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import deleteDownloadedFilesContaining
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class BackupTest : BaseUiTest() {
    private var teamOwner: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @After
    fun tearDown() {
        deleteDownloadedFilesContaining("Wire")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4243")
    @Category("regression", "RC", "backup")
    @Test
    fun givenTeamOwnerCreatesBackupWithoutPassword_whenLoggingBackInAndRestoringBackup_thenMessagesAreImported() {
        step("Given There is a team owner user1Name with team Messaging") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Messaging",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("And User user1Name adds user user2Name to team Messaging with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Messaging",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation BackingUp with user2Name in team Messaging") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "BackingUp",
                "user2Name",
                "Messaging"
            )
        }

        testServiceHelper.apply {
            addDevice("user2Name", null, "Device1")
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("When I open staging backend deep link and log in as user1Name") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterUserIdentifier(teamOwner?.email.orEmpty())
                clickLoginButton()
                enterUserPassword(teamOwner?.password.orEmpty())
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

        step("Then I see conversation BackingUp in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("BackingUp")
        }

        step("And I tap on conversation name BackingUp in conversation list") {
            pages.conversationListPage.clickGroupConversation("BackingUp")
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And User user2Name sends message Hello to you, too! to group conversation BackingUp") {
            testServiceHelper.apply {
                userSendMessageToConversation(
                    "user2Name",
                    "Hello to you, too!",
                    "Device1",
                    "BackingUp"
                )
            }
        }

        step("And I see the message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("And I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I open the Back up & Restore Conversations menu from Settings") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.apply {
                openBackupAndRestoreConversationsMenu()
            }
            pages.backupPage.apply {
                iSeeBackupPageHeading()
            }
        }

        step("And I create and save a backup without password") {
            pages.backupPage.apply {
                clickCreateBackupButton()
                clickBackUpNowButton()
                iSeeBackupConfirmation("Conversations successfully saved")
                iTapSaveFileButton()
                iTapSaveInOSMenuButton()
                iSeeBackupPageHeading()
            }
        }

        step("And I return to conversation list and open User Profile Page") {
            pages.settingsPage.apply {
                clickBackButtonOnSettingsPage()
                clickBackButtonOnSettingsPage()
            }
            pages.conversationListPage.apply {
                assertConversationListVisible()
                clickUserProfileButton()
            }
        }

        step("And I log out with clear data selected") {
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                tapInfoTextCheckbox()
                tapLogoutButton()
            }
        }

        step("And I see email verification Welcome Page after logout") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("When I open staging backend deep link and log in as user1Name again") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterUserIdentifier(teamOwner?.email.orEmpty())
                clickLoginButton()
                enterUserPassword(teamOwner?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in again and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation BackingUp in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("BackingUp")
        }

        step("And I open conversation BackingUp and verify local message is absent before restore") {
            pages.conversationListPage.clickGroupConversation("BackingUp")
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("BackingUp")
                assertMessageNotVisible("Hello!")
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("When I open Settings from conversation list") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("And I open Restore from Backup") {
            pages.settingsPage.apply {
                openBackupAndRestoreConversationsMenu()
            }
            pages.backupPage.apply {
                clickRestoreBackupButton()
            }
        }

        step("And I choose the saved backup file") {
            pages.backupPage.apply {
                clickChooseBackupFileButton()
                selectBackupFileInDocumentsUI(clientUserManager, "user1Name")
            }
        }

        step("Then I wait until conversations have been restored") {
            pages.backupPage.apply {
                waitUntilThisTextIsDisplayedOnBackupAlert("Conversations have been restored")
                clickOkButtonOnBackupAlert()
            }
        }

        step("And I see conversation list with BackingUp") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("BackingUp")
            }
        }

        step("And I open conversation BackingUp and see both restored messages") {
            pages.conversationListPage.clickGroupConversation("BackingUp")
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("BackingUp")
                assertRestoredBackupMessageIsVisibleInCurrentConversation("Hello!")
                assertRestoredBackupMessageIsVisibleInCurrentConversation("Hello to you, too!")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4244")
    @Category("regression", "RC", "backup")
    @Test
    fun givenIWantToCreateABackupWithPassword_whenIImportIt_thenMessagesAreRestored() {
        val backupPassword = "A1a!".repeat(2)

        step("Given There is a team owner user1Name with team Messaging") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Messaging",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("And User user1Name adds user user2Name to team Messaging with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Messaging",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation BackingUp with user2Name in team Messaging") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "BackingUp",
                "user2Name",
                "Messaging"
            )
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("When I open staging backend deep link and log in as user1Name") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterUserIdentifier(teamOwner?.email.orEmpty())
                clickLoginButton()
                enterUserPassword(teamOwner?.password.orEmpty())
                clickLoginButton()
            }
        }

        testServiceHelper.apply {
            addDevice("user2Name", null, "Device1")
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation BackingUp in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("BackingUp")
        }

        step("And I tap on conversation name BackingUp in conversation list") {
            pages.conversationListPage.clickGroupConversation("BackingUp")
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And User user2Name sends message Hello to you, too! to group conversation BackingUp") {
            testServiceHelper.apply {
                userSendMessageToConversation(
                    "user2Name",
                    "Hello to you, too!",
                    "Device1",
                    "BackingUp"
                )
            }
        }

        step("And I see the message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("And I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I open the Back up & Restore Conversations menu from Settings") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.apply {
                openBackupAndRestoreConversationsMenu()
            }
            pages.backupPage.apply {
                iSeeBackupPageHeading()
            }
        }

        step("And I create and save a backup with password") {
            pages.backupPage.apply {
                clickCreateBackupButton()
                typeBackupPassword(backupPassword)
                clickBackUpNowButton()
                iSeeBackupConfirmation("Conversations successfully saved")
                iTapSaveFileButton()
                iTapSaveInOSMenuButton()
                iSeeBackupPageHeading()
            }
        }

        step("And I return to conversation list and open User Profile Page") {
            pages.settingsPage.apply {
                clickBackButtonOnSettingsPage()
                clickBackButtonOnSettingsPage()
            }
            pages.conversationListPage.apply {
                assertConversationListVisible()
                clickUserProfileButton()
            }
        }

        step("And I log out with clear data selected") {
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                tapInfoTextCheckbox()
                tapLogoutButton()
            }
        }

        step("And I see email verification Welcome Page after logout") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("When I open staging backend deep link and log in as user1Name again") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterUserIdentifier(teamOwner?.email.orEmpty())
                clickLoginButton()
                enterUserPassword(teamOwner?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in again and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation BackingUp in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("BackingUp")
        }

        step("And I open conversation BackingUp and verify local message is absent before restore") {
            pages.conversationListPage.clickGroupConversation("BackingUp")
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("BackingUp")
                assertMessageNotVisible("Hello!")
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("When I open Settings from conversation list") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("And I open Restore from Backup") {
            pages.settingsPage.apply {
                openBackupAndRestoreConversationsMenu()
            }
            pages.backupPage.apply {
                clickRestoreBackupButton()
            }
        }

        step("And I choose the saved backup file") {
            pages.backupPage.apply {
                clickChooseBackupFileButton()
                selectBackupFileInDocumentsUI(clientUserManager, "user1Name")
            }
        }

        step("And I type my password to restore my backup and continue") {
            pages.backupPage.apply {
                typeBackupPassword(backupPassword)
                tapContinueButtonOnBackupPage()
            }
        }

        step("Then I wait until conversations have been restored") {
            pages.backupPage.apply {
                waitUntilThisTextIsDisplayedOnBackupAlert("Conversations have been restored")
                clickOkButtonOnBackupAlert()
            }
        }

        step("And I see conversation list with BackingUp") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("BackingUp")
            }
        }

        step("And I open conversation BackingUp and see both restored messages") {
            pages.conversationListPage.clickGroupConversation("BackingUp")
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("BackingUp")
                assertRestoredBackupMessageIsVisibleInCurrentConversation("Hello!")
                assertRestoredBackupMessageIsVisibleInCurrentConversation("Hello to you, too!")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4245")
    @Category("regression", "RC", "backup")
    @Test
    fun givenIWantToImportABackupCreatedOnPreviousVersion_whenIImportIt_thenMessagesAreRestored() {
        val backupPassword = "A1a!".repeat(2)

        step("Given There is a team owner user1Name with team Messaging") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Messaging",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("And User user1Name adds user user2Name to team Messaging with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Messaging",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation BackingUp with user2Name in team Messaging") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "BackingUp",
                "user2Name",
                "Messaging"
            )
        }

        testServiceHelper.apply {
            addDevice("user2Name", null, "Device1")
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("When I open staging backend deep link and log in as user1Name") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterUserIdentifier(teamOwner?.email.orEmpty())
                clickLoginButton()
                enterUserPassword(teamOwner?.password.orEmpty())
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

        step("Then I see conversation BackingUp in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("BackingUp")
        }

        step("And I tap on conversation name BackingUp in conversation list") {
            pages.conversationListPage.clickGroupConversation("BackingUp")
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And User user2Name sends message Hello to you, too! to group conversation BackingUp") {
            testServiceHelper.apply {
                userSendMessageToConversation(
                    "user2Name",
                    "Hello to you, too!",
                    "Device1",
                    "BackingUp"
                )
            }
        }

        step("And I see the message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("And I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I open the Back up & Restore Conversations menu from Settings") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.apply {
                openBackupAndRestoreConversationsMenu()
            }
            pages.backupPage.apply {
                iSeeBackupPageHeading()
            }
        }

        step("And I create and save a backup with password") {
            pages.backupPage.apply {
                clickCreateBackupButton()
                typeBackupPassword(backupPassword)
                clickBackUpNowButton()
                iSeeBackupConfirmation("Conversations successfully saved")
                iTapSaveFileButton()
                iTapSaveInOSMenuButton()
                iSeeBackupPageHeading()
            }
        }

        step("And I return to conversation list and open User Profile Page") {
            pages.settingsPage.apply {
                clickBackButtonOnSettingsPage()
                clickBackButtonOnSettingsPage()
            }
            pages.conversationListPage.apply {
                assertConversationListVisible()
                clickUserProfileButton()
            }
        }

        step("And I log out with clear data selected") {
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                tapInfoTextCheckbox()
                tapLogoutButton()
            }
        }

        step("And I wait for logout to complete") {
            UiWaitUtils.waitFor(1.seconds)
        }

        step("When I upgrade Wire to the recent version") {
            val recentWireApkPath = InstrumentationRegistry.getArguments()
                .getString("newApkPath") ?: "/data/local/tmp/Wire.new.apk"
            UiAutomatorSetup.upgradeWireToRecentVersion(recentWireApkPath)
        }

        step("And I see email verification Welcome Page after upgrade") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("When I open staging backend deep link and log in as user1Name again") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterUserIdentifier(teamOwner?.email.orEmpty())
                clickLoginButton()
                enterUserPassword(teamOwner?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in again and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation BackingUp in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("BackingUp")
        }

        step("And I open conversation BackingUp and verify local message is absent before restore") {
            pages.conversationListPage.clickGroupConversation("BackingUp")
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("BackingUp")
                assertMessageNotVisible("Hello!")
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("When I open Settings from conversation list") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("And I open Restore from Backup") {
            pages.settingsPage.apply {
                openBackupAndRestoreConversationsMenu()
            }
            pages.backupPage.apply {
                clickRestoreBackupButton()
            }
        }

        step("And I choose the saved backup file") {
            pages.backupPage.apply {
                clickChooseBackupFileButton()
                selectBackupFileInDocumentsUI(clientUserManager, "user1Name")
            }
        }

        step("And I type my password to restore my backup and continue") {
            pages.backupPage.apply {
                typeBackupPassword(backupPassword)
                tapContinueButtonOnBackupPage()
            }
        }

        step("Then I wait until conversations have been restored") {
            pages.backupPage.apply {
                waitUntilThisTextIsDisplayedOnBackupAlert("Conversations have been restored")
                clickOkButtonOnBackupAlert()
            }
        }

        step("And I see conversation list with BackingUp") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("BackingUp")
            }
        }

        step("And I open conversation BackingUp and see both restored messages") {
            pages.conversationListPage.clickGroupConversation("BackingUp")
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("BackingUp")
                assertRestoredBackupMessageIsVisibleInCurrentConversation("Hello!")
                assertRestoredBackupMessageIsVisibleInCurrentConversation("Hello to you, too!")
            }
        }
    }
}
