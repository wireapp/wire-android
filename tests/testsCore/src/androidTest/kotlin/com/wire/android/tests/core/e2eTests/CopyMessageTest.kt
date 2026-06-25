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
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class CopyMessageTest : BaseUiTest() {

    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, currentUser) }
    }

    @TestCaseId("TC-4315")
    @Category("copyMessage", "regression", "RC")
    @Test
    fun givenMessage_whenCopyingIt_thenCopyConfirmationIsShown() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupConversation()
        sendLocalMessage(MESSAGE)

        copyMessageToClipboard(MESSAGE)
    }

    @TestCaseId("TC-4314")
    @Category("copyMessage", "regression", "RC")
    @Test
    fun givenManyMessages_whenCopyingAfterScroll_thenCopyConfirmationIsShown() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupConversation()
        sendDefaultMessages()
        sendLocalMessage(SCROLLED_MESSAGE)

        step("Scroll through conversation and return to the target message") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertSentMessageIsVisibleInCurrentConversation(SCROLLED_MESSAGE)
                scrollToTopOfConversationScreen()
                scrollToBottomOfConversationScreen()
            }
        }

        copyMessageToClipboard(SCROLLED_MESSAGE)
    }

    private fun prepareGroupConversation() {
        step("Prepare backend team, member, device, and group conversation") {
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
            testServiceHelper.apply {
                userHasGroupConversationInTeam(
                    TEAM_OWNER_ALIAS,
                    GROUP_CONVERSATION_NAME,
                    TEAM_MEMBER_ALIAS,
                    TEAM_NAME
                )
            }
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser() {
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
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun openGroupConversation() {
        step("Open group conversation") {
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                clickGroupConversation(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.assertConversationScreenVisible()
        }
    }

    private fun sendLocalMessage(message: String) {
        step("Send message '$message' in group conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField(message)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(message)
            }
        }
    }

    private fun sendDefaultMessages() {
        step("Send default messages to group conversation") {
            repeat(DEFAULT_MESSAGE_COUNT) { index ->
                sendLocalMessage("Default message ${index + 1}")
            }
        }
    }

    private fun copyMessageToClipboard(message: String) {
        step("Copy message '$message'") {
            pages.conversationViewPage.apply {
                longPressOnMessage(message)
                tapCopyTextOption()
            }
            waitUntilToastIsDisplayed(COPIED_TOAST)
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "CopyCats"
        const val GROUP_CONVERSATION_NAME = "CopyMe"
        const val MESSAGE = "Good day!"
        const val SCROLLED_MESSAGE = "That is a lot of messages"
        const val DEFAULT_MESSAGE_COUNT = 20
        const val COPIED_TOAST = "Message copied"
    }
}
