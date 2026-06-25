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
class PollMessagesTest : BaseUiTest() {

    private var teamOwner: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, teamOwner) }
    }

    @TestCaseId("TC-4472")
    @Category("polls", "regression", "RC")
    @Test
    fun givenGroupConversation_whenMemberSendsPoll_thenPollMessageAndOptionsAreVisible() {
        prepareGroupConversation()
        loginTeamOwner()
        openGroupConversation()

        step("Member sends poll message through test-service") {
            testServiceHelper.userSendsPollMessageToConversation(
                senderAlias = TEAM_MEMBER_ALIAS,
                msg = POLL_MESSAGE,
                title = POLL_TITLE,
                buttonsCsv = "$FIRST_BUTTON,$SECOND_BUTTON",
                deviceName = null,
                dstConvoName = GROUP_CONVERSATION_NAME
            )
        }

        step("Verify poll message and options are visible") {
            pages.conversationViewPage.apply {
                assertPollMessageVisible(POLL_MESSAGE)
                assertPollButtonVisible(FIRST_BUTTON)
                assertPollButtonVisible(SECOND_BUTTON)
            }
        }
    }

    @TestCaseId("TC-4473")
    @Category("polls", "regression", "RC")
    @Ignore("Blocked: selected poll option is only a visual button color and is not exposed to UIAutomator semantics")
    @Test
    fun givenGroupPoll_whenVoting_thenSelectedOptionCanBeAsserted() {
        // TC mapping only. Enable when selected poll state is exposed semantically.
    }

    @TestCaseId("TC-4474")
    @Category("polls", "regression", "RC")
    @Ignore("Blocked: selected poll option is only a visual button color and is not exposed to UIAutomator semantics")
    @Test
    fun givenGroupPollWithVote_whenChangingVote_thenNewSelectedOptionCanBeAsserted() {
        // TC mapping only. Enable when selected poll state is exposed semantically.
    }

    private fun prepareGroupConversation() {
        step("Prepare team owner, member, and group conversation") {
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
            teamOwner = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginTeamOwner() {
        step("Login team owner via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamOwnerLoggingEmail(teamOwner?.email.orEmpty())
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner?.password.orEmpty())
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

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Polling"
        const val GROUP_CONVERSATION_NAME = "Polls"
        const val POLL_TITLE = "Question"
        const val POLL_MESSAGE = "What is your favorite animal?"
        const val FIRST_BUTTON = "Cat"
        const val SECOND_BUTTON = "Dog"
    }
}
