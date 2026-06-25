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
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class BlockTest : BaseUiTest() {

    override val deletePersonalUsersAfterTest = true

    private var currentUser: ClientUser? = null
    private var otherUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @TestCaseId("TC-4246")
    @Category("blockUser", "regression", "RC")
    @Test
    fun givenSameTeamUser_whenOpeningProfileOptions_thenBlockOptionIsNotVisible() {
        prepareSameTeamOneOnOneConversation()
        loginCurrentUser()
        openOtherUserProfile()

        step("Assert same-team member cannot be blocked from profile options") {
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptionsIfVisible()
                assertBlockOptionNotVisible()
            }
        }
    }

    @TestCaseId("TC-4247", "TC-4248")
    @Category("blockUser", "unblockUser", "regression", "RC")
    @Test
    fun givenConnectedPersonalUser_whenBlockingFromProfile_thenUnblockButtonRestoresProfileState() {
        prepareConnectedPersonalUsers()
        loginCurrentUser()
        openOtherUserProfile()

        blockOtherUserFromProfile()

        step("Unblock user from profile button") {
            pages.connectedUserProfilePage.apply {
                clickUnblockUserButton()
                clickUnblockButtonAlert()
                assertBlockedLabelNotVisible()
                assertUnblockUserButtonNotVisible()
            }
        }
    }

    @TestCaseId("TC-4253", "TC-4252")
    @Category("blockUser", "unblockUser", "regression", "RC")
    @Test
    fun givenConnectedPersonalUser_whenBlockingFromProfile_thenUnblockOptionRestoresProfileState() {
        prepareConnectedPersonalUsers()
        loginCurrentUser()
        openOtherUserProfile()

        blockOtherUserFromProfile()

        step("Unblock user from profile options") {
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
                clickUnblockOption()
                clickUnblockButtonAlert()
                assertBlockedLabelNotVisible()
                assertUnblockUserButtonNotVisible()
            }
        }
    }

    @TestCaseId("TC-4249")
    @Category("blockUser", "regression", "RC")
    @Test
    fun givenSameTeamUser_whenOpeningConversationListActions_thenBlockOptionIsNotVisible() {
        prepareSameTeamOneOnOneConversation()
        loginCurrentUser()

        step("Assert same-team member cannot be blocked from conversation-list actions") {
            pages.conversationListPage.apply {
                assertConversationVisible(otherUser?.name.orEmpty())
                longPressConversation(otherUser?.name.orEmpty())
                assertBlockOptionNotVisibleInConversationActions()
            }
        }
    }

    @TestCaseId("TC-4250", "TC-4251")
    @Category("blockUser", "unblockUser", "regression", "RC")
    @Test
    fun givenConnectedPersonalUser_whenBlockingFromConversationList_thenUnblockRestoresListState() {
        prepareConnectedPersonalUsers()
        loginCurrentUser()

        blockOtherUserFromConversationList()

        step("Unblock user from conversation-list actions") {
            pages.conversationListPage.apply {
                longPressConversation(otherUser?.name.orEmpty())
                tapUnblockOptionInConversationActions()
                tapUnblockConfirmButton()
                assertConversationBlockedLabelNotVisible(otherUser?.name.orEmpty())
            }
        }
    }

    @TestCaseId("TC-4254", "TC-4255")
    @Category("blockUser", "unblockUser", "regression", "RC")
    @Test
    fun givenConnectedUserFromAnotherTeam_whenBlockingFromProfile_thenUnblockButtonRestoresProfileState() {
        prepareConnectedTeamOwnersFromDifferentTeams()
        loginCurrentUser()
        openOtherUserProfile()

        blockOtherUserFromProfile()

        step("Unblock cross-team user from profile button") {
            pages.connectedUserProfilePage.apply {
                clickUnblockUserButton()
                clickUnblockButtonAlert()
                assertBlockedLabelNotVisible()
                assertUnblockUserButtonNotVisible()
            }
        }
    }

    @TestCaseId("TC-4256", "TC-4257")
    @Category("blockUser", "unblockUser", "regression", "RC")
    @Test
    fun givenConnectedUserFromAnotherTeam_whenBlockingFromConversationList_thenUnblockRestoresListState() {
        prepareConnectedTeamOwnersFromDifferentTeams()
        loginCurrentUser()

        blockOtherUserFromConversationList()

        step("Unblock cross-team user from conversation-list actions") {
            pages.conversationListPage.apply {
                longPressConversation(otherUser?.name.orEmpty())
                tapUnblockOptionInConversationActions()
                tapUnblockConfirmButton()
                assertConversationBlockedLabelNotVisible(otherUser?.name.orEmpty())
            }
        }
    }

    private fun prepareSameTeamOneOnOneConversation() {
        step("Prepare backend team owner and same-team member") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                CURRENT_USER_ALIAS,
                TEAM_NAME,
                "en_US",
                true,
                backendClient,
                context
            )
            teamHelper.userXAddsUsersToTeam(
                CURRENT_USER_ALIAS,
                OTHER_USER_ALIAS,
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            testServiceHelper.userHas1on1ConversationInTeam(CURRENT_USER_ALIAS, OTHER_USER_ALIAS, TEAM_NAME)

            currentUser = teamHelper.usersManager.findUserBy(CURRENT_USER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            otherUser = teamHelper.usersManager.findUserBy(OTHER_USER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun prepareConnectedPersonalUsers() {
        step("Prepare connected personal users") {
            val users = teamHelper.usersManager.createPersonalUsersByAliases(
                listOf(CURRENT_USER_ALIAS, OTHER_USER_ALIAS),
                backendClient
            )
            currentUser = users.first()
            otherUser = users.last()
            runBlocking {
                backendClient.sendConnectionRequest(otherUser!!, currentUser!!)
                backendClient.acceptAllIncomingConnectionRequests(currentUser!!)
            }
        }
    }

    private fun prepareConnectedTeamOwnersFromDifferentTeams() {
        step("Prepare connected team owners from different teams") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                CURRENT_USER_ALIAS,
                TEAM_NAME,
                "en_US",
                true,
                backendClient,
                context
            )
            teamHelper.usersManager.createTeamOwnerByAlias(
                OTHER_USER_ALIAS,
                OTHER_TEAM_NAME,
                "en_US",
                true,
                backendClient,
                context
            )
            currentUser = teamHelper.usersManager.findUserBy(CURRENT_USER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            otherUser = teamHelper.usersManager.findUserBy(OTHER_USER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            runBlocking {
                backendClient.sendConnectionRequest(otherUser!!, currentUser!!)
                backendClient.acceptAllIncomingConnectionRequests(currentUser!!)
            }
            testServiceHelper.userHas1on1ConversationInTeam(CURRENT_USER_ALIAS, OTHER_USER_ALIAS, TEAM_NAME)
        }
    }

    private fun loginCurrentUser() {
        step("Login current user via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterPersonalUserLoggingEmail(currentUser?.email.orEmpty())
                clickLoginButton()
                enterPersonalUserLoginPassword(currentUser?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("Complete login flow") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                setUserNameIfVisible(currentUser?.uniqueUsername.orEmpty())
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun openOtherUserProfile() {
        step("Open other user's 1:1 profile") {
            pages.conversationListPage.apply {
                assertConversationVisible(otherUser?.name.orEmpty())
                tapConversationNameInConversationList(otherUser?.name.orEmpty())
            }
            pages.conversationViewPage.apply {
                assertConversationIsVisibleWithTeamMember(otherUser?.name.orEmpty())
                click1On1ConversationDetails(otherUser?.name.orEmpty())
            }
        }
    }

    private fun blockOtherUserFromProfile() {
        step("Block other user from profile options") {
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
                clickBlockOption()
                clickBlockButtonAlert()
                assertToastMessageIsDisplayed("${otherUser?.name.orEmpty()} blocked")
                assertBlockedLabelVisible()
                assertUnblockUserButtonVisible()
            }
        }
    }

    private fun blockOtherUserFromConversationList() {
        step("Block other user from conversation-list actions") {
            pages.conversationListPage.apply {
                assertConversationVisible(otherUser?.name.orEmpty())
                longPressConversation(otherUser?.name.orEmpty())
                tapBlockOptionInConversationActions()
                tapBlockConfirmButton()
                assertToastMessageIsDisplayed("${otherUser?.name.orEmpty()} blocked")
                assertConversationBlockedLabelVisible(otherUser?.name.orEmpty())
            }
        }
    }

    private companion object {
        const val CURRENT_USER_ALIAS = "user1Name"
        const val OTHER_USER_ALIAS = "user2Name"
        const val TEAM_NAME = "Blocking"
        const val OTHER_TEAM_NAME = "ToBeBlocked"
    }
}
