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
class MentionsTest : BaseUiTest() {

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

    @TestCaseId("TC-4448")
    @Category("mentions", "regression", "RC")
    @Test
    fun givenGroupConversation_whenSelectingUserFromMentionList_thenMentionIsSent() {
        prepareGroupConversation()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)

        val mentionedUser = memberOne()
        val sentMention = "@${mentionedUser.name.orEmpty()}"

        step("Open mention list and select member") {
            pages.conversationViewPage.apply {
                tapMentionSomeoneButton()
                assertUserVisibleInMentionList(mentionedUser.name.orEmpty())
                selectUserFromMentionList(mentionedUser.name.orEmpty())
                clickSendButton()
                assertVisibleMentionedNameIs(sentMention)
            }
        }
    }

    @Ignore(
        "Blocked: original TC requires another user to send a real backend mention; low-level TestService supports mentions, but no stable high-level helper exists for resolving mention user id/domain and local device timing."
    )
    @TestCaseId("TC-4449")
    @Category("mentions", "regression", "RC", "blocked")
    @Test
    fun givenGroupConversation_whenRemoteUserMentionsMe_thenMentionIsVisible() {
        // TC mapping is preserved until TestServiceHelper has a stable send-mention wrapper.
    }

    private fun prepareGroupConversation() {
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
                "$TEAM_MEMBER_ONE_ALIAS,$TEAM_MEMBER_TWO_ALIAS",
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("Team owner has group conversation with members") {
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                "$TEAM_MEMBER_ONE_ALIAS,$TEAM_MEMBER_TWO_ALIAS",
                TEAM_NAME
            )
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

    private fun memberOne(): ClientUser =
        teamHelper.usersManager.findUserBy(TEAM_MEMBER_ONE_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ONE_ALIAS = "user2Name"
        const val TEAM_MEMBER_TWO_ALIAS = "user3Name"
        const val TEAM_NAME = "Notification"
        const val GROUP_CONVERSATION_NAME = "MyTeam"
    }
}
