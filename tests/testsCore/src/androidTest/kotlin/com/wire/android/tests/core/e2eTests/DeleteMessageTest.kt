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
@file:Suppress("ArgumentListWrapping")

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
class DeleteMessageTest : BaseUiTest() {

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

    @TestCaseId("TC-4316")
    @Category("deleteMessage", "regression", "RC", "conversationView")
    @Test
    fun givenOneOnOneConversation_whenDeletingOwnMessageForEveryone_thenDeletedLabelIsShown() {
        prepareOneOnOneConversation()
        loginCurrentUser()
        openConversation(memberName())
        sendLocalMessage(ONE_ON_ONE_MESSAGE)

        deleteMessageForEveryone(ONE_ON_ONE_MESSAGE)
    }

    @TestCaseId("TC-4318")
    @Category("deleteMessage", "regression", "RC", "groups")
    @Test
    fun givenGroupConversation_whenDeletingOwnMessageForEveryone_thenDeletedLabelIsShown() {
        prepareGroupConversation()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)
        sendLocalMessage(GROUP_MESSAGE)

        deleteMessageForEveryone(GROUP_MESSAGE)
    }

    @TestCaseId("TC-4317")
    @Category("deleteMessage", "regression", "RC", "conversationView")
    @Test
    fun givenOneOnOneConversation_whenDeletingReceivedMessageForMyself_thenMessageIsHiddenLocally() {
        prepareOneOnOneConversation()
        loginCurrentUser()
        openConversation(memberName())
        sendRemoteOneOnOneMessage(ONE_ON_ONE_RECEIVED_MESSAGE)

        deleteMessageForMe(ONE_ON_ONE_RECEIVED_MESSAGE)
    }

    @TestCaseId("TC-4319")
    @Category("deleteMessage", "regression", "RC", "groups")
    @Ignore(
        "Blocked: Pixel 9a retry hung/crashed during the group delete-for-me flow after login; keep skipped until the group setup/run is stabilized."
    )
    @Test
    fun givenGroupConversation_whenDeletingReceivedMessageForMyself_thenMessageIsHiddenLocally() {
        prepareGroupConversation()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)
        sendRemoteGroupMessage(GROUP_RECEIVED_MESSAGE)

        deleteMessageForMe(GROUP_RECEIVED_MESSAGE)
    }

    private fun prepareOneOnOneConversation() {
        prepareTeamWithMembers(TEAM_MEMBER_ALIAS)
        step("Team owner has 1:1 conversation with member") {
            testServiceHelper.userHas1on1ConversationInTeam(TEAM_OWNER_ALIAS, TEAM_MEMBER_ALIAS, ONE_ON_ONE_TEAM_NAME)
        }
    }

    private fun prepareGroupConversation() {
        prepareTeamWithMembers("$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS")
        step("Team owner has group conversation with members") {
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                GROUP_TEAM_NAME
            )
        }
    }

    private fun prepareTeamWithMembers(memberAliases: String) {
        val teamName = if (memberAliases.contains(EXTRA_MEMBER_ALIAS)) GROUP_TEAM_NAME else ONE_ON_ONE_TEAM_NAME
        step("Prepare backend team owner and members") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                TEAM_OWNER_ALIAS,
                teamName,
                "en_US",
                true,
                backendClient,
                context
            )
            teamHelper.userXAddsUsersToTeam(
                TEAM_OWNER_ALIAS,
                memberAliases,
                teamName,
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

    private fun deleteMessageForEveryone(message: String) {
        step("Delete message '$message' for everyone") {
            pages.conversationViewPage.apply {
                longPressOnMessage(message)
                tapDeleteOption()
                tapDeleteForEveryoneOption()
                assertDeletedMessageLabelVisible()
                assertMessageNotVisible(message)
            }
        }
    }

    private fun deleteMessageForMe(message: String) {
        step("Delete message '$message' for me") {
            pages.conversationViewPage.apply {
                longPressOnMessage(message)
                tapDeleteOption()
                tapDeleteForMeOption()
                assertMessageNotVisible(message)
            }
        }
    }

    private fun sendRemoteOneOnOneMessage(message: String) {
        step("Remote member sends 1:1 message '$message'") {
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
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(message)
        }
    }

    private fun sendRemoteGroupMessage(message: String) {
        step("Remote member sends message '$message'") {
            testServiceHelper.apply {
                addDevice(TEAM_MEMBER_ALIAS, null, DEVICE_NAME)
                userSendMessageToConversation(
                    TEAM_MEMBER_ALIAS,
                    message,
                    DEVICE_NAME,
                    GROUP_CONVERSATION_NAME,
                    false
                )
            }
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(message)
        }
    }

    private fun memberName(): String =
        teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS).name.orEmpty()

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val EXTRA_MEMBER_ALIAS = "user3Name"
        const val ONE_ON_ONE_TEAM_NAME = "Messaging"
        const val GROUP_TEAM_NAME = "MessageDeleting"
        const val GROUP_CONVERSATION_NAME = "MyTeam"
        const val DEVICE_NAME = "Device1"
        const val ONE_ON_ONE_MESSAGE = "Hello to you, too!"
        const val GROUP_MESSAGE = "Hello group, too!"
        const val ONE_ON_ONE_RECEIVED_MESSAGE = "Please delete this for yourself"
        const val GROUP_RECEIVED_MESSAGE = "Please delete this group message for yourself"
    }
}
