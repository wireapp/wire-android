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
@file:Suppress("ArgumentListWrapping", "MaximumLineLength")

package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import deleteDownloadedFilesContaining
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class BackupTest : BaseUiTest() {

    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        deleteDownloadedFilesContaining("Wire")
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        deleteDownloadedFilesContaining("Wire")
        runCatching { cleanupBackendClient(backendClient, currentUser) }
    }

    @TestCaseId("TC-4243")
    @Category("backup", "regression", "RC")
    @Test
    @Suppress("LongMethod")
    fun givenBackupWithoutPassword_whenRestoringAfterClearDataLogout_thenConversationMessagesAreRestored() {
        prepareGroupConversation()
        loginCurrentUser()

        step("Send messages in group conversation") {
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
                typeMessageInInputField(MESSAGE_1)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(MESSAGE_1)
                typeMessageInInputField(MESSAGE_2)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(MESSAGE_2)
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Create backup without password and save it through DocumentsUI") {
            openBackupSettings()
            pages.settingsPage.apply {
                clickCreateBackupButton()
                clickBackUpNowButton()
                iSeeBackupConfirmation(BACKUP_CREATED_MESSAGE)
                iTapSaveFileButton()
                iTapSaveInOSMenuButton()
                iSeeBackupPageHeading()
            }
            returnToConversationListFromSettings()
        }

        step("Clear local app data and restart Wire") {
            clearAppDataAndRestart()
            pages.registrationPage.assertEmailWelcomePage(timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
        }

        step("Login again and verify local messages are absent before restore") {
            loginCurrentUser()
            openGroupConversation()
            pages.conversationViewPage.apply {
                assertMessageNotVisible(MESSAGE_1)
                assertMessageNotVisible(MESSAGE_2)
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Restore saved backup") {
            openBackupSettings()
            pages.settingsPage.apply {
                clickRestoreBackupButton()
                clickChooseBackupFileButton()
                selectBackupFileInDocumentsUI(teamHelper, TEAM_OWNER_ALIAS)
                waitUntilThisTextIsDisplayedOnBackupAlert(BACKUP_RESTORED_MESSAGE)
                clickOkButtonOnBackupAlert()
            }
        }

        step("Verify backed up messages are visible after restore") {
            pages.conversationListPage.assertConversationListVisible()
            openGroupConversation()
            pages.conversationViewPage.apply {
                assertRestoredBackupMessageIsVisibleInCurrentConversation(MESSAGE_1)
                assertRestoredBackupMessageIsVisibleInCurrentConversation(MESSAGE_2)
            }
        }
    }

    @Ignore(
        "Blocked: Kotlin SettingsPage does not yet expose stable selectors for the backup password fields on create and restore screens."
    )
    @TestCaseId("TC-4244")
    @Category("backup", "regression", "RC")
    @Test
    fun givenBackupWithPassword_whenRestoringAfterClearDataLogout_thenConversationMessagesAreRestored() = Unit

    @Ignore(
        "Stale/blocked: legacy scenario requires reinstalling an old Wire version, creating a backup there, and upgrading to the current APK."
    )
    @TestCaseId("TC-4245")
    @Category("backup", "regression", "RC", "stale")
    @Test
    fun givenBackupCreatedOnPreviousVersion_whenRestoringAfterUpgrade_thenConversationMessagesAreRestored() = Unit

    private fun prepareGroupConversation() {
        step("Prepare backend team owner, member, and group conversation") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                TEAM_OWNER_ALIAS,
                TEAM_NAME,
                "en_US",
                true,
                backendClient,
                context
            )
            teamHelper.userXAddsUsersToTeam(
                TEAM_OWNER_ALIAS,
                TEAM_MEMBER_ALIAS,
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                TEAM_MEMBER_ALIAS,
                TEAM_NAME
            )
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser(attempt: Int = 1) {
        step("Login current user via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamOwnerLoggingEmail(currentUser?.email.orEmpty())
                clickLoginButton()
                enterTeamOwnerLoggingPassword(currentUser?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("Complete login flow") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            if (handleRemovedDeviceDialogIfPresent()) {
                if (attempt >= LOGIN_ATTEMPTS) {
                    throw AssertionError("Removed Device dialog is still shown after $LOGIN_ATTEMPTS login attempts.")
                }
                loginCurrentUser(attempt + 1)
                return@step
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun openBackupSettings() {
        pages.conversationListPage.apply {
            clickConversationsMenuEntry()
            clickSettingsButtonOnMenuEntry()
        }
        pages.settingsPage.apply {
            openBackupAndRestoreConversationsMenu()
            iSeeBackupPageHeading()
        }
    }

    private fun clearAppDataAndRestart() {
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA, clearData = true)
    }

    private fun handleRemovedDeviceDialogIfPresent(): Boolean {
        val removedDeviceDialog = UiWaitUtils.findElementOrNull(UiSelectorParams(text = REMOVED_DEVICE_TITLE))
        if (removedDeviceDialog != null && !removedDeviceDialog.visibleBounds.isEmpty) {
            UiWaitUtils.waitElement(UiSelectorParams(text = "OK")).click()
            pages.registrationPage.assertEmailWelcomePage(timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
            return true
        }
        return false
    }

    private fun openGroupConversation() {
        pages.conversationListPage.apply {
            assertConversationVisible(GROUP_CONVERSATION_NAME)
            tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
        }
        pages.conversationViewPage.assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
    }

    private fun returnToConversationListFromSettings() {
        repeat(BACK_TO_CONVERSATION_LIST_ATTEMPTS) {
            if (runCatching { pages.conversationListPage.assertConversationVisible(GROUP_CONVERSATION_NAME) }.isSuccess) {
                return
            }
            device.pressBack()
            device.waitForIdle()
        }
        pages.conversationListPage.assertConversationVisible(GROUP_CONVERSATION_NAME)
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Messaging"
        const val GROUP_CONVERSATION_NAME = "BackingUp"
        const val MESSAGE_1 = "Hello!"
        const val MESSAGE_2 = "Hello to you, too!"
        const val BACKUP_CREATED_MESSAGE = "Conversations successfully saved"
        const val BACKUP_RESTORED_MESSAGE = "Conversations have been restored"
        const val REMOVED_DEVICE_TITLE = "Removed Device"
        const val LOGIN_ATTEMPTS = 2
        const val BACK_TO_CONVERSATION_LIST_ATTEMPTS = 3
    }
}
