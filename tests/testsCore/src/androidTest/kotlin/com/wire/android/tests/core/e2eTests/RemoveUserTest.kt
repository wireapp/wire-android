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
import backendUtils.team.deleteTeamMember
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.iSeeSystemMessage
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class RemoveUserTest : BaseUiTest() {

    private var teamOwner: ClientUser? = null
    private var currentUser: ClientUser? = null
    private var removedMember: ClientUser? = null

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

    @TestCaseId("TC-4363")
    @Category("groups", "removeUser", "regression", "RC")
    @Test
    fun givenTeamOwnerInGroupConversation_whenTeamMemberIsRemoved_thenRemovedFromTeamSystemMessageIsShown() {
        prepareGroupConversation(TEAM_OWNER_ALIAS)
        loginCurrentUser()
        openGroupConversation()

        removeMemberFromTeam()
        assertRemovedFromTeamSystemMessage()
    }

    @TestCaseId("TC-4365")
    @Category("groups", "removeUser", "regression", "RC")
    @Test
    fun givenTeamMemberInGroupConversation_whenAnotherTeamMemberIsRemoved_thenRemovedFromTeamSystemMessageIsShown() {
        prepareGroupConversation(TEAM_MEMBER_ALIAS)
        loginCurrentUser()
        openGroupConversation()

        removeMemberFromTeam()
        assertRemovedFromTeamSystemMessage()
    }

    @TestCaseId("TC-4494")
    @Category("removeUser", "regression", "RC")
    @Ignore(
        "Blocked: removed-user 1:1 conversation removal needs stable post-removal logout/session handling and list disappearance assertions."
    )
    @Test
    fun givenConnectedUserIsRemovedFromTeam_whenReturningToConversationList_thenOneOnOneConversationIsRemoved() = Unit

    @TestCaseId("TC-4369")
    @Category("groups", "removeUser", "regression", "RC")
    @Ignore(
        "Blocked: guest removal system-message flow needs guest setup, guest-visible group membership, and removal propagation helpers."
    )
    @Test
    fun givenGuestInGroupConversation_whenTeamMemberIsRemoved_thenGuestSeesRemovedFromTeamSystemMessage() = Unit

    private fun prepareGroupConversation(loginUserAlias: String) {
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
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS,$REMOVED_MEMBER_ALIAS",
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS,$REMOVED_MEMBER_ALIAS",
                TEAM_NAME
            )
            teamOwner = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            currentUser = teamHelper.usersManager.findUserBy(loginUserAlias, ClientUserManager.FindBy.NAME_ALIAS)
            removedMember = teamHelper.usersManager.findUserBy(REMOVED_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
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
                tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
        }
    }

    private fun removeMemberFromTeam() {
        step("Remove member from team through backend") {
            teamOwner?.deleteTeamMember(backendClient, removedMember?.id.orEmpty())
            UiWaitUtils.waitFor(2.seconds)
        }
    }

    private fun assertRemovedFromTeamSystemMessage() {
        step("Verify removed from team system message") {
            iSeeSystemMessage("${removedMember?.name.orEmpty()} was removed from the team", 30.seconds)
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val EXTRA_MEMBER_ALIAS = "user3Name"
        const val REMOVED_MEMBER_ALIAS = "user4Name"
        const val TEAM_NAME = "RemoveUser"
        const val GROUP_CONVERSATION_NAME = "MyTeam"
    }
}
