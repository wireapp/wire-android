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
import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class ClearContentTest : BaseUiTest() {

    private var teamOwner: ClientUser? = null
    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, teamOwner ?: currentUser) }
    }

    @TestCaseId("TC-4272")
    @Category("clearContent", "regression", "RC", "smoke")
    @Test
    fun givenGroupConversationWithMessages_whenClearingContentFromConversationList_thenMessagesAreRemoved() {
        prepareGroupConversation()
        loginCurrentUser()

        openConversationAndSendMessages()

        step("Clear conversation content from conversation list") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                longPressConversation(GROUP_CONVERSATION_NAME)
                tapClearContentButtonInConversationActions()
                tapClearContentConfirmationButton()
            }
            waitUntilToastIsDisplayed(CONTENT_DELETED_TOAST)
        }

        step("Reopen conversation and assert messages are gone") {
            pages.conversationListPage.tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
                assertMessageNotVisible(MESSAGE_1)
                assertMessageNotVisible(MESSAGE_2)
            }
        }
    }

    @TestCaseId("TC-4271")
    @Category("groups", "clearContent", "regression", "RC")
    @Test
    fun givenGroupConversationWithMessages_whenClearingContentFromGroupDetails_thenMessagesAreRemoved() {
        prepareGroupConversation()
        loginCurrentUser()

        openConversationAndSendMessages()

        step("Clear conversation content from group details") {
            pages.conversationViewPage.clickOnGroupConversationDetails(GROUP_CONVERSATION_NAME)
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapShowMoreOptionsButton()
                tapClearContentOption()
                tapClearContentConfirmationButton()
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("Verify messages are gone from the conversation") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
                assertMessageNotVisible(MESSAGE_1)
                assertMessageNotVisible(MESSAGE_2)
            }
        }
    }

    @TestCaseId("TC-4273")
    @Category("groups", "clearContent", "regression", "RC")
    @Test
    fun givenLeftGroupConversation_whenClearingContentFromConversationList_thenMessagesAreRemoved() {
        prepareGroupConversation()
        loginCurrentUser()

        openConversationAndSendMessages()

        step("Leave group conversation from group details") {
            pages.conversationViewPage.clickOnGroupConversationDetails(GROUP_CONVERSATION_NAME)
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapShowMoreOptionsButton()
                tapLeaveConversationOption()
                tapLeaveConversationConfirmationButton()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("Verify left conversation history remains visible") {
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation(MESSAGE_1)
                assertReceivedMessageIsVisibleInCurrentConversation(MESSAGE_2)
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Clear left conversation content from conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                longPressConversation(GROUP_CONVERSATION_NAME)
                tapClearContentButtonInConversationActions()
                tapClearContentConfirmationButton()
            }
            waitUntilToastIsDisplayed(CONTENT_DELETED_TOAST)
        }

        step("Reopen left conversation and assert messages are gone") {
            pages.conversationListPage.tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
            pages.conversationViewPage.apply {
                assertMessageNotVisible(MESSAGE_1)
                assertMessageNotVisible(MESSAGE_2)
            }
        }
    }

    @TestCaseId("TC-4274")
    @Category("groups", "clearContent", "regression", "RC")
    @Test
    fun givenRemovedFromGroupConversation_whenClearingContentFromConversationList_thenMessagesAreRemoved() {
        prepareGroupConversation(loginUserAlias = TEAM_MEMBER_ALIAS)
        loginCurrentUser()

        openConversationAndSendMessages()

        step("Owner removes current user from group conversation") {
            testServiceHelper.userRemovesUserFromGroupConversation(
                TEAM_OWNER_ALIAS,
                TEAM_MEMBER_ALIAS,
                GROUP_CONVERSATION_NAME
            )
        }

        step("Verify removed conversation history remains visible") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation(MESSAGE_1)
                assertReceivedMessageIsVisibleInCurrentConversation(MESSAGE_2)
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Clear removed conversation content from conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                longPressConversation(GROUP_CONVERSATION_NAME)
                tapClearContentButtonInConversationActions()
                tapClearContentConfirmationButton()
            }
            waitUntilToastIsDisplayed(CONTENT_DELETED_TOAST)
        }

        step("Reopen removed conversation and assert messages are gone") {
            pages.conversationListPage.tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
            pages.conversationViewPage.apply {
                assertMessageNotVisible(MESSAGE_1)
                assertMessageNotVisible(MESSAGE_2)
            }
        }
    }

    @TestCaseId("TC-4275")
    @Category("groups", "clearContent", "regression", "RC")
    @Test
    fun givenGroupConversationWithMessagesAndAssets_whenClearingContent_thenMessagesAndAssetsAreRemoved() {
        prepareGroupConversation()
        loginCurrentUser()

        openConversationAndSendMessages()

        step("Receive image, audio, video, and text file assets") {
            receiveRemoteGroupAssets()
            pages.conversationViewPage.apply {
                assertImageFileWithNameIsVisible(IMAGE_FILE_NAME)
                assertAudioMessageIsVisible()
                scrollToBottomOfConversationScreen()
                assertFileWithNameIsVisible(VIDEO_FILE_NAME)
                assertFileWithNameIsVisible(TEXT_FILE_NAME)
            }
        }

        step("Clear conversation content from group details") {
            pages.conversationViewPage.clickOnGroupConversationDetails(GROUP_CONVERSATION_NAME)
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapShowMoreOptionsButton()
                tapClearContentOption()
                tapClearContentConfirmationButton()
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("Verify messages and assets are gone from the conversation") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
                assertMessageNotVisible(MESSAGE_1)
                assertMessageNotVisible(MESSAGE_2)
                assertFileWithNameNotVisible(IMAGE_FILE_NAME)
                assertAudioMessageNotVisible()
                assertFileWithNameNotVisible(VIDEO_FILE_NAME)
                assertFileWithNameNotVisible(TEXT_FILE_NAME)
            }
        }
    }

    private fun prepareGroupConversation(loginUserAlias: String = TEAM_OWNER_ALIAS) {
        step("Prepare backend team owner, members, and group conversation") {
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
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                TEAM_NAME
            )
            teamOwner = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            currentUser = teamHelper.usersManager.findUserBy(loginUserAlias, ClientUserManager.FindBy.NAME_ALIAS)
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

    private fun handleRemovedDeviceDialogIfPresent(): Boolean {
        val removedDeviceDialog = UiWaitUtils.findElementOrNull(UiSelectorParams(text = REMOVED_DEVICE_TITLE))
        if (removedDeviceDialog != null && !removedDeviceDialog.visibleBounds.isEmpty) {
            UiWaitUtils.waitElement(UiSelectorParams(text = "OK")).click()
            pages.registrationPage.assertEmailWelcomePage(timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
            return true
        }
        return false
    }

    private fun openConversationAndSendMessages() {
        step("Open group conversation and receive messages") {
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
                receiveRemoteGroupMessage(MESSAGE_1)
                assertReceivedMessageIsVisibleInCurrentConversation(MESSAGE_1)
                receiveRemoteGroupMessage(MESSAGE_2)
                assertReceivedMessageIsVisibleInCurrentConversation(MESSAGE_2)
            }
        }
    }

    private fun receiveRemoteGroupMessage(message: String) {
        testServiceHelper.apply {
            addDevice(EXTRA_MEMBER_ALIAS, null, DEVICE_NAME)
            userSendMessageToConversation(
                EXTRA_MEMBER_ALIAS,
                message,
                DEVICE_NAME,
                GROUP_CONVERSATION_NAME,
                false
            )
        }
    }

    private fun receiveRemoteGroupAssets() {
        testServiceHelper.apply {
            addDevice(TEAM_MEMBER_ALIAS, null, DEVICE_NAME)
            addDevice(EXTRA_MEMBER_ALIAS, null, EXTRA_DEVICE_NAME)
            contactSendsLocalImageConversation(context, IMAGE_FILE_NAME, TEAM_MEMBER_ALIAS, DEVICE_NAME, GROUP_CONVERSATION_NAME)
            contactSendsLocalAudioConversation(context, AUDIO_FILE_NAME, TEAM_MEMBER_ALIAS, DEVICE_NAME, GROUP_CONVERSATION_NAME)
            contactSendsLocalVideoConversation(context, VIDEO_FILE_NAME, TEAM_MEMBER_ALIAS, DEVICE_NAME, GROUP_CONVERSATION_NAME)
            contactSendsLocalTextConversation(context, TEXT_FILE_NAME, EXTRA_MEMBER_ALIAS, EXTRA_DEVICE_NAME, GROUP_CONVERSATION_NAME)
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val EXTRA_MEMBER_ALIAS = "user3Name"
        const val TEAM_NAME = "Clearing"
        const val GROUP_CONVERSATION_NAME = "ClearContent"
        const val MESSAGE_1 = "Hello!"
        const val MESSAGE_2 = "Good Morning"
        const val CONTENT_DELETED_TOAST = "Conversation content was deleted"
        const val REMOVED_DEVICE_TITLE = "Removed Device"
        const val LOGIN_ATTEMPTS = 2
        const val DEVICE_NAME = "remote-device"
        const val EXTRA_DEVICE_NAME = "extra-remote-device"
        const val IMAGE_FILE_NAME = "testing.jpg"
        const val AUDIO_FILE_NAME = "test.m4a"
        const val VIDEO_FILE_NAME = "testing.mp4"
        const val TEXT_FILE_NAME = "qa_random.txt"
    }
}
