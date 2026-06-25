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
class EditMessageTest : BaseUiTest() {

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

    @TestCaseId("TC-4330", "TC-4331")
    @Category("editMessage", "regression", "RC", "smoke")
    @Test
    fun givenOneOnOneConversation_whenEditingOwnMessage_thenEditedMessageAndLabelAreShown() {
        prepareOneOnOneConversation()
        loginCurrentUser()
        openConversation(memberName())
        sendLocalMessage(ORIGINAL_MESSAGE)

        editOwnMessage(ORIGINAL_MESSAGE, EDITED_MESSAGE)
    }

    @TestCaseId("TC-4334", "TC-4335")
    @Category("editMessage", "regression", "RC", "smoke")
    @Test
    fun givenGroupConversation_whenEditingOwnMessage_thenEditedMessageAndLabelAreShown() {
        prepareGroupConversation()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)
        sendLocalMessage(ORIGINAL_MESSAGE)

        editOwnMessage(ORIGINAL_MESSAGE, EDITED_MESSAGE)
    }

    @TestCaseId("TC-4338")
    @Category("editMessage", "regression", "RC")
    @Test
    fun givenGroupConversationWithManyMessages_whenScrollingBack_thenEditOptionIsStillVisible() {
        prepareGroupConversation()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)
        sendLocalMessage(ORIGINAL_MESSAGE)
        sendDefaultMessages()
        sendLocalMessage(SCROLLED_MESSAGE)

        step("Scroll through conversation and open actions for the last message") {
            pages.conversationViewPage.apply {
                scrollToTopOfConversationScreen()
                scrollToBottomOfConversationScreen()
                longPressOnMessage(SCROLLED_MESSAGE)
                assertEditTextOptionVisible()
            }
        }
    }

    @TestCaseId("TC-4332", "TC-4333")
    @Category("editMessage", "regression", "RC")
    @Ignore(
        "Blocked: received 1:1 edit parity needs a remote edit helper that preserves the sent message id; " +
            "legacy edited timestamp/date assertions also need current UI selector parity beyond the visible Edited label."
    )
    @Test
    fun givenOneOnOneConversation_whenOtherUserEditsMessage_thenEditedMessageAndLabelAreMappedOnly() = Unit

    @TestCaseId("TC-4336", "TC-4337")
    @Category("editMessage", "regression", "RC")
    @Ignore(
        "Blocked: received group edit parity needs a remote edit helper that preserves the sent message id; " +
            "legacy edited timestamp/date assertions also need current UI selector parity beyond the visible Edited label."
    )
    @Test
    fun givenGroupConversation_whenOtherUserEditsMessage_thenEditedMessageAndLabelAreMappedOnly() = Unit

    @TestCaseId("TC-8151")
    @Category("editMessage", "regression", "RC")
    @Ignore("Blocked: repeated remote edits need a helper that edits the same backend-sent group message id multiple times.")
    @Test
    fun givenGroupConversation_whenOtherUserEditsSameMessageRepeatedly_thenLatestEditIsMappedOnly() = Unit

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
                sendLocalMessage("Edit default message ${index + 1}")
            }
        }
    }

    private fun editOwnMessage(originalMessage: String, editedMessage: String) {
        step("Edit message '$originalMessage' to '$editedMessage'") {
            pages.conversationViewPage.apply {
                longPressOnMessage(originalMessage)
                tapEditTextOption()
                typeMessageInInputField(editedMessage)
                clickEditMessageButton()
                assertSentMessageIsVisibleInCurrentConversation(editedMessage)
                assertMessageNotVisible(originalMessage)
                assertEditedLabelVisible()
            }
        }
    }

    private fun memberName(): String =
        teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS).name.orEmpty()

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Messaging"
        const val GROUP_CONVERSATION_NAME = "EditMe"
        const val ORIGINAL_MESSAGE = "Hello!"
        const val EDITED_MESSAGE = "Good Morning!"
        const val SCROLLED_MESSAGE = "That is a lot of messages"
        const val DEFAULT_MESSAGE_COUNT = 20
    }
}
