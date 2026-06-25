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
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class ReactionsTest : BaseUiTest() {

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

    @TestCaseId("TC-4475", "TC-4483")
    @Category("reactions", "regression", "RC")
    @Test
    fun givenOneOnOneConversation_whenReactingToTextMessage_thenReactionCanBeAddedAndRemoved() {
        prepareOneOnOneConversation()
        loginCurrentUser()
        openConversation(memberName())
        sendLocalMessage(ONE_ON_ONE_MESSAGE)

        addReactionToMessage(ONE_ON_ONE_MESSAGE, HEART_REACTION)
        removeReaction(HEART_REACTION)
    }

    @TestCaseId("TC-4476", "TC-4484")
    @Category("reactions", "regression", "RC")
    @Test
    fun givenGroupConversation_whenReactingToTextMessage_thenReactionCanBeAddedAndRemoved() {
        prepareGroupConversation()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)
        sendLocalMessage(GROUP_MESSAGE)

        addReactionToMessage(GROUP_MESSAGE, HEART_REACTION)
        removeReaction(HEART_REACTION)
    }

    @TestCaseId("TC-4487")
    @Category("reactions", "regression", "RC")
    @Test
    fun givenBusyGroupConversation_whenScrollingBack_thenMessageCanStillBeReactedTo() {
        prepareGroupConversation()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)
        sendDefaultMessages()
        sendLocalMessage(SCROLLED_MESSAGE)

        step("Scroll through conversation and react to the last message") {
            pages.conversationViewPage.apply {
                scrollToTopOfConversationScreen()
                scrollToBottomOfConversationScreen()
                longPressOnMessage(SCROLLED_MESSAGE)
                tapReactionIcon(HEART_REACTION)
                assertReactionAndUserCountVisible(HEART_REACTION, 1)
            }
        }
    }

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
                sendLocalMessage("Reaction default message ${index + 1}")
            }
        }
    }

    private fun addReactionToMessage(message: String, reaction: String) {
        step("React to message '$message' with '$reaction'") {
            pages.conversationViewPage.apply {
                longPressOnMessage(message)
                assertTextMessageReactionOptionsVisible()
                tapReactionIcon(reaction)
                assertReactionAndUserCountVisible(reaction, 1)
            }
        }
    }

    private fun removeReaction(reaction: String) {
        step("Remove reaction '$reaction'") {
            pages.conversationViewPage.apply {
                tapVisibleReaction(reaction)
                assertReactionNotVisible(reaction)
            }
        }
    }

    private fun memberName(): String =
        teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS).name.orEmpty()

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Reactions"
        const val GROUP_CONVERSATION_NAME = "ReactHere"
        const val ONE_ON_ONE_MESSAGE = "Hello!"
        const val GROUP_MESSAGE = "Hello group!"
        const val SCROLLED_MESSAGE = "That is a lot of messages"
        const val HEART_REACTION = "\u2764\uFE0F"
        const val DEFAULT_MESSAGE_COUNT = 20
    }
}
