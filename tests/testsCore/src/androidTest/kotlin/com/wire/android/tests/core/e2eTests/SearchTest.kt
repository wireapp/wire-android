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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class SearchTest : BaseUiTest() {

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

    @TestCaseId("TC-4505")
    @Category("search", "regression", "RC", "smoke")
    @Test
    fun givenTeamMember_whenSearchingByDisplayName_thenMemberIsShown() {
        prepareTeamWithMember()
        loginCurrentUser()

        step("Search for team member by display name") {
            openPeopleSearch()
            pages.searchPage.apply {
                typeUserNameInSearchField(teamHelper, TEAM_MEMBER_ALIAS)
                assertUsernameInSearchResultIs(memberName())
            }
        }
    }

    @TestCaseId("TC-4504")
    @Category("search", "regression", "RC")
    @Test
    fun givenTeamMember_whenSearchingByEmail_thenMemberIsNotShown() {
        prepareTeamWithMember()
        loginCurrentUser()

        step("Search for team member by email") {
            openPeopleSearch()
            pages.searchPage.apply {
                typeRawTextInSearchField(member?.email.orEmpty())
                assertUsernameNotInSearchResult(memberName())
            }
        }
    }

    @TestCaseId("TC-4503")
    @Category("search", "regression", "RC")
    @Test
    fun givenTeamMemberWithUniqueUsername_whenSearchingByUniqueUsername_thenMemberIsShown() {
        prepareTeamWithMember()

        step("Member sets unique username") {
            runBlocking {
                testServiceHelper.usersSetUniqueUsername(TEAM_MEMBER_ALIAS)
            }
            member = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }

        loginCurrentUser()

        step("Search for team member by unique username") {
            openPeopleSearch()
            pages.searchPage.apply {
                typeUniqueUserNameInSearchField(teamHelper, TEAM_MEMBER_ALIAS)
                assertUsernameInSearchResultIs(memberName())
            }
        }
    }

    private fun prepareTeamWithMember() {
        step("Prepare backend team owner and member") {
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
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            member = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
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

    private fun openPeopleSearch() {
        pages.conversationListPage.tapStartNewConversationButton()
        pages.searchPage.tapSearchPeopleField()
    }

    private fun memberName(): String = member?.name.orEmpty()

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Search"
    }
}
