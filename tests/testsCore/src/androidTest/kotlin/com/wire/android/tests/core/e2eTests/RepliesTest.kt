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
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class RepliesTest : BaseUiTest() {

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

    @TestCaseId("TC-4495")
    @Category("replies", "regression", "RC")
    @Test
    fun givenGroupConversation_whenReplyingToTextMessage_thenReplyContainsQuotedMessage() {
        prepareGroupConversation()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)
        sendLocalMessage(GROUP_QUESTION)

        replyToMessage(GROUP_QUESTION, GROUP_REPLY)
    }

    @TestCaseId("TC-4498")
    @Category("replies", "regression", "RC")
    @Test
    fun givenOneOnOneConversation_whenReplyingToTextMessage_thenReplyContainsQuotedMessage() {
        prepareOneOnOneConversation()
        loginCurrentUser()
        openConversation(memberName())
        sendLocalMessage(ONE_ON_ONE_MESSAGE)

        replyToMessage(ONE_ON_ONE_MESSAGE, ONE_ON_ONE_REPLY)
    }

    @TestCaseId("TC-4497", "TC-4500")
    @Category("replies", "regression", "RC")
    @Test
    fun givenConversationWithReply_whenReplyingToReply_thenSecondReplyContainsQuotedReply() {
        prepareGroupConversation()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)
        sendLocalMessage(GROUP_QUESTION)
        replyToMessage(GROUP_QUESTION, GROUP_REPLY)

        replyToMessage(GROUP_REPLY, REPLY_TO_REPLY)
    }

    @TestCaseId("TC-4501")
    @Category("replies", "regression", "RC")
    @Test
    fun givenBusyGroupConversation_whenScrollingBack_thenReplyTargetsCorrectMessage() {
        prepareGroupConversation()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)
        sendDefaultMessages()
        sendLocalMessage(SCROLLED_MESSAGE)

        step("Scroll through conversation and reply to the last message") {
            pages.conversationViewPage.apply {
                scrollToTopOfConversationScreen()
                scrollToBottomOfConversationScreen()
            }
        }
        replyToMessage(SCROLLED_MESSAGE, SCROLLED_REPLY)
    }

    @TestCaseId("TC-4496")
    @Category("replies", "regression", "RC")
    @Ignore(
        "Blocked: received group reply parity needs a high-level backend send-reply wrapper resolving quoted message id/hash " +
            "plus stable remote-device message setup."
    )
    @Test
    fun givenGroupConversation_whenOtherUserRepliesToMyMessage_thenReceivedReplyIsShown() = Unit

    @TestCaseId("TC-4499")
    @Category("replies", "regression", "RC")
    @Ignore(
        "Blocked: received 1:1 reply parity needs a high-level backend send-reply wrapper resolving quoted message id/hash " +
            "plus stable remote-device message setup."
    )
    @Test
    fun givenOneOnOneConversation_whenOtherUserRepliesToMyMessage_thenReceivedReplyIsShown() = Unit

    private fun prepareOneOnOneConversation() {
        prepareTeamWithMembers(TEAM_MEMBER_ALIAS)
        step("Team owner has 1:1 conversation with member") {
            testServiceHelper.userHas1on1ConversationInTeam(TEAM_OWNER_ALIAS, TEAM_MEMBER_ALIAS, TEAM_NAME)
        }
    }

    private fun prepareGroupConversation() {
        prepareTeamWithMembers(TEAM_MEMBER_ALIAS)
        step("Team owner has group conversation with member") {
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                TEAM_MEMBER_ALIAS,
                TEAM_NAME
            )
        }
    }

    private fun prepareTeamWithMembers(memberAliases: String) {
        step("Prepare backend team owner and members") {
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
                memberAliases,
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
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

    private fun openConversation(conversationName: String) {
        step("Open conversation '$conversationName'") {
            pages.conversationListPage.apply {
                assertConversationVisible(conversationName)
                clickGroupConversation(conversationName)
            }
            pages.conversationViewPage.assertConversationScreenVisible()
        }
    }

    private fun sendLocalMessage(message: String) {
        step("Send message '$message'") {
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
                sendLocalMessage("Reply default message ${index + 1}")
            }
        }
    }

    private fun replyToMessage(originalMessage: String, replyMessage: String) {
        step("Reply to message '$originalMessage' with '$replyMessage'") {
            pages.conversationViewPage.apply {
                longPressOnMessage(originalMessage)
                assertReplyOptionVisible()
                tapReplyOption()
                assertReplyPreviewVisible(originalMessage)
                typeMessageInInputField(replyMessage)
                clickSendButton()
                assertMessageIsReplyTo(replyMessage, originalMessage)
            }
        }
    }

    private fun memberName(): String =
        teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS).name.orEmpty()

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Reply"
        const val GROUP_CONVERSATION_NAME = "Replies"
        const val GROUP_QUESTION = "What is your favorite pizza?"
        const val GROUP_REPLY = "Tuna!"
        const val REPLY_TO_REPLY = "Wow!"
        const val ONE_ON_ONE_MESSAGE = "Hello"
        const val ONE_ON_ONE_REPLY = "Bye"
        const val SCROLLED_MESSAGE = "That is a lot of messages"
        const val SCROLLED_REPLY = "Yes!"
        const val DEFAULT_MESSAGE_COUNT = 20
    }
}
