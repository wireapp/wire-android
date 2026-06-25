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
class ConversationListFilterTest : BaseUiTest() {

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

    @TestCaseId("TC-4299")
    @Category("conversationListFilter", "search", "regression", "RC")
    @Test
    fun givenOneOnOneConversation_whenSearchingByFullContactName_thenConversationIsShown() {
        prepareOneOnOneConversation()
        loginCurrentUser()

        searchConversation(memberName())
        pages.conversationListPage.assertConversationVisible(memberName())
    }

    @TestCaseId("TC-4301")
    @Category("conversationListFilter", "search", "regression", "RC")
    @Test
    fun givenOneOnOneConversation_whenSearchingByPartialContactName_thenConversationIsShown() {
        prepareOneOnOneConversation()
        loginCurrentUser()

        searchConversation(memberName().take(PARTIAL_SEARCH_LENGTH))
        pages.conversationListPage.assertConversationVisible(memberName())
    }

    @TestCaseId("TC-4300")
    @Category("conversationListFilter", "search", "regression", "RC")
    @Test
    fun givenGroupConversation_whenSearchingByFullConversationName_thenConversationIsShown() {
        prepareGroupConversation()
        loginCurrentUser()

        searchConversation(GROUP_CONVERSATION_NAME)
        pages.conversationListPage.assertConversationVisible(GROUP_CONVERSATION_NAME)
    }

    @TestCaseId("TC-4302")
    @Category("conversationListFilter", "search", "regression", "RC")
    @Test
    fun givenGroupConversation_whenSearchingByPartialConversationName_thenConversationIsShown() {
        prepareGroupConversation()
        loginCurrentUser()

        searchConversation(GROUP_CONVERSATION_NAME.take(PARTIAL_SEARCH_LENGTH))
        pages.conversationListPage.assertConversationVisible(GROUP_CONVERSATION_NAME)
    }

    @TestCaseId("TC-4303")
    @Category("conversationListFilter", "search", "regression", "RC")
    @Test
    fun givenGroupConversation_whenSearchingByLowerCaseConversationName_thenConversationIsShown() {
        prepareGroupConversation()
        loginCurrentUser()

        searchConversation(GROUP_CONVERSATION_NAME.lowercase())
        pages.conversationListPage.assertConversationVisible(GROUP_CONVERSATION_NAME)
    }

    @TestCaseId("TC-4304")
    @Category("conversationListFilter", "search", "regression", "RC")
    @Test
    fun givenGroupConversation_whenSearchingByUpperCaseConversationName_thenConversationIsShown() {
        prepareGroupConversation()
        loginCurrentUser()

        searchConversation(GROUP_CONVERSATION_NAME.uppercase())
        pages.conversationListPage.assertConversationVisible(GROUP_CONVERSATION_NAME)
    }

    @TestCaseId("TC-4305")
    @Category("conversationListFilter", "search", "regression", "RC")
    @Test
    fun givenGroupConversation_whenSearchingForMissingConversation_thenConversationIsNotShown() {
        prepareGroupConversation()
        loginCurrentUser()

        searchConversation(MISSING_CONVERSATION_NAME)
        pages.conversationListPage.assertConversationNotVisible(GROUP_CONVERSATION_NAME)
    }

    @TestCaseId("TC-8654", "TC-8655")
    @Category("filter", "regression", "RC")
    @Test
    fun givenOneOnOneAndGroupConversations_whenFilteringByType_thenOnlyMatchingConversationsAreShown() {
        prepareOneOnOneAndGroupConversations()
        loginCurrentUser()

        step("Verify initial conversations are visible") {
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                assertConversationVisible(memberName())
            }
        }

        step("Filter by groups") {
            pages.conversationListPage.apply {
                tapFilterConversationsButton()
                assertFilterConversationsSheetVisible()
                tapConversationFilterOption(GROUPS_FILTER)
                assertFilterPageHeadingVisible(GROUPS_FILTER)
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                assertConversationNotVisible(memberName())
            }
        }

        step("Filter by 1:1 conversations") {
            pages.conversationListPage.apply {
                tapFilterConversationsButton()
                assertFilterConversationsSheetVisible()
                tapConversationFilterOption(ONE_ON_ONE_FILTER)
                assertFilterPageHeadingVisible(ONE_ON_ONE_FILTER)
                assertConversationVisible(memberName())
                assertConversationNotVisible(GROUP_CONVERSATION_NAME)
            }
        }
    }

    private fun prepareOneOnOneConversation() {
        prepareTeamWithMember()
        step("Team owner has 1:1 conversation with member") {
            testServiceHelper.userHas1on1ConversationInTeam(TEAM_OWNER_ALIAS, TEAM_MEMBER_ALIAS, TEAM_NAME)
        }
    }

    private fun prepareGroupConversation() {
        prepareTeamWithMember()
        step("Team owner has group conversation with member") {
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                TEAM_MEMBER_ALIAS,
                TEAM_NAME
            )
        }
    }

    private fun prepareOneOnOneAndGroupConversations() {
        prepareTeamWithMember()
        step("Team owner has 1:1 and group conversations with member") {
            testServiceHelper.userHas1on1ConversationInTeam(TEAM_OWNER_ALIAS, TEAM_MEMBER_ALIAS, TEAM_NAME)
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                TEAM_MEMBER_ALIAS,
                TEAM_NAME
            )
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

    private fun searchConversation(query: String) {
        step("Search conversation list for '$query'") {
            pages.conversationListPage.apply {
                tapSearchConversationField()
                typeTextInSearchField(query)
            }
        }
    }

    private fun memberName(): String = member?.name.orEmpty()

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Messaging"
        const val GROUP_CONVERSATION_NAME = "Search GroupChat"
        const val MISSING_CONVERSATION_NAME = "DefinitelyMissingConversation"
        const val PARTIAL_SEARCH_LENGTH = 3
        const val GROUPS_FILTER = "Groups"
        const val ONE_ON_ONE_FILTER = "1:1 Conversations"
    }
}
