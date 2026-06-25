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
class MessagingTest : BaseUiTest() {

    private var currentUser: ClientUser? = null
    private var member: ClientUser? = null

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

    @TestCaseId("TC-4450")
    @Category("messaging", "regression", "RC", "smoke")
    @Test
    fun givenOneOnOneConversation_whenExchangingMessagesWithTeamMember_thenBothMessagesAreVisible() {
        prepareOneOnOneConversation()
        loginCurrentUser()

        step("Open 1:1 conversation with team member") {
            pages.conversationListPage.apply {
                assertConversationVisible(memberName())
                tapConversationNameInConversationList(memberName())
            }
            pages.conversationViewPage.assertConversationIsVisibleWithTeamMember(memberName())
        }

        step("Send local message") {
            pages.conversationViewPage.apply {
                typeMessageInInputField(LOCAL_MESSAGE)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(LOCAL_MESSAGE)
                typeMessageInInputField(SECOND_LOCAL_MESSAGE)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(SECOND_LOCAL_MESSAGE)
            }
        }
    }

    @TestCaseId("TC-4452")
    @Category("messaging", "regression", "RC")
    @Test
    fun givenOneOnOneConversation_whenSendingVeryLongMessage_thenLongMessageAndRemoteReplyAreVisible() {
        prepareOneOnOneConversation()
        loginCurrentUser()
        openOneOnOneConversation()

        step("Send an 8000-character local message") {
            pages.conversationViewPage.apply {
                typeMessageInInputField(LONG_MESSAGE)
                clickSendButton()
                assertMessageContainingTextIsVisibleInCurrentConversation(LONG_MESSAGE_PREFIX)
            }
        }

        step("Remote member sends a reply and it is visible") {
            sendRemoteOneOnOneMessage(REMOTE_REPLY)
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(REMOTE_REPLY)
        }
    }

    @TestCaseId("TC-4451")
    @Category("messaging", "regression", "RC")
    @Test
    fun givenGroupConversation_whenSendingVeryLongMessage_thenLongMessageAndRemoteReplyAreVisible() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupConversation(LONG_MESSAGE_GROUP)

        step("Send an 8000-character local message") {
            pages.conversationViewPage.apply {
                typeMessageInInputField(LONG_MESSAGE)
                clickSendButton()
                assertMessageContainingTextIsVisibleInCurrentConversation(LONG_MESSAGE_PREFIX)
            }
        }

        step("Remote member sends a reply and it is visible") {
            sendRemoteGroupMessage(LONG_MESSAGE_GROUP, REMOTE_REPLY)
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(REMOTE_REPLY)
        }
    }

    @Ignore("Blocked: current UIAutomator helpers cannot prove >8000-character rejection without send-button/error/counter semantics.")
    @TestCaseId("TC-4453")
    @Category("messaging", "regression", "RC", "blocked")
    @Test
    fun givenGroupConversation_whenMessageExceedsCharacterLimit_thenMessageIsNotSent() = Unit

    @Ignore("Blocked: offline retry flow needs reliable network toggle/restore helpers and Retry/Cancel/error selectors.")
    @TestCaseId("TC-4454", "TC-4456")
    @Category("messaging", "regression", "RC", "blocked")
    @Test
    fun givenGroupConversationOffline_whenRetryingFailedMessage_thenRetryStateAndSuccessfulResendAreValidated() = Unit

    @Ignore("Blocked: offline cancel flow needs reliable network helpers, Retry/Cancel selectors, and delete-for-me confirmation helpers.")
    @TestCaseId("TC-4455")
    @Category("messaging", "regression", "RC", "blocked")
    @Test
    fun givenGroupConversationOffline_whenCancelingFailedMessage_thenFailedMessageIsRemoved() = Unit

    private fun prepareOneOnOneConversation() {
        step("Prepare backend team owner, member, 1:1 conversation, and member device") {
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
            testServiceHelper.userHas1on1ConversationInTeam(TEAM_OWNER_ALIAS, TEAM_MEMBER_ALIAS, TEAM_NAME)
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            member = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun prepareGroupConversation() {
        step("Prepare backend team owner, member, group conversation, and member device") {
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
                LONG_MESSAGE_GROUP,
                TEAM_MEMBER_ALIAS,
                TEAM_NAME
            )
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            member = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser() {
        step("Login team owner via staging deep link") {
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

    private fun openOneOnOneConversation() {
        step("Open 1:1 conversation with team member") {
            pages.conversationListPage.apply {
                assertConversationVisible(memberName())
                tapConversationNameInConversationList(memberName())
            }
            pages.conversationViewPage.assertConversationIsVisibleWithTeamMember(memberName())
        }
    }

    private fun openGroupConversation(conversationName: String) {
        step("Open group conversation $conversationName") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(conversationName)
                clickGroupConversation(conversationName)
            }
            pages.conversationViewPage.assertConversationScreenVisible()
        }
    }

    private fun sendRemoteOneOnOneMessage(message: String) {
        testServiceHelper.apply {
            addDevice(TEAM_MEMBER_ALIAS, null, DEVICE_NAME)
            userSendMessageToPersonalMlsConversation(
                TEAM_MEMBER_ALIAS,
                message,
                DEVICE_NAME,
                TEAM_OWNER_ALIAS,
                false
            )
        }
    }

    private fun sendRemoteGroupMessage(groupConversation: String, message: String) {
        testServiceHelper.apply {
            addDevice(TEAM_MEMBER_ALIAS, null, DEVICE_NAME)
            userSendMessageToConversation(
                TEAM_MEMBER_ALIAS,
                message,
                DEVICE_NAME,
                groupConversation,
                false
            )
        }
    }

    private fun memberName(): String = member?.name.orEmpty()

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Messaging"
        const val LONG_MESSAGE_GROUP = "LongMessage"
        const val LOCAL_MESSAGE = "Hello!"
        const val SECOND_LOCAL_MESSAGE = "Hello again!"
        const val REMOTE_REPLY = "Hello!"
        const val DEVICE_NAME = "Device1"
        const val LONG_MESSAGE_PREFIX = "LongMessagePrefix"
        val LONG_MESSAGE = LONG_MESSAGE_PREFIX + "A".repeat(8000 - LONG_MESSAGE_PREFIX.length)
    }
}
