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
import backendUtils.team.TeamRoles
import backendUtils.team.updateUserProfileImage
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class SearchTests : BaseUiTest() {
    override val deletePersonalUsersAfterTest = true

    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var member2: ClientUser
    private lateinit var member3: ClientUser
    private lateinit var secondTeamOwner: ClientUser
    private lateinit var personalUser: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4505")
    @Category("regression", "RC", "search", "smoke")
    @Test
    fun givenTeamOwnerWithTeamMember_whenISearchByUsername_thenMemberIsDisplayed() {
        step("Given There is a team owner user1Name with team Search") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Search",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Search with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Search",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        loginToStagingAs(teamOwner)

        step("When I start a new conversation and search for user2Name by name") {
            pages.conversationListPage.tapStartNewConversationButton()
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I see user name user2Name in Search result list") {
            pages.searchPage.assertUsernameInSearchResultIs(member1.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4504")
    @Category("regression", "RC", "search")
    @Test
    fun givenTeamOwnerWithTeamMember_whenISearchByEmail_thenMemberIsNotDisplayed() {
        step("Given There is a team owner user1Name with team Search") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Search",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Search with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Search",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        loginToStagingAs(teamOwner)

        step("When I start a new conversation and search for user2Name by email") {
            pages.conversationListPage.tapStartNewConversationButton()
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUserEmailInSearchField(clientUserManager, "user2Email")
            }
        }

        step("Then I do not see user name user2Name in Search result list") {
            pages.searchPage.assertUsernameNotInSearchResult(member1.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4503")
    @Category("regression", "RC", "search")
    @Test
    fun givenTeamOwnerWithTeamMember_whenISearchByUniqueUsername_thenMemberIsDisplayed() {
        step("Given There is a team owner user1Name with team Search") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Search",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Search with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Search",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And User user2Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        loginToStagingAs(teamOwner)

        step("When I start a new conversation and search for user2Name by unique username") {
            pages.conversationListPage.tapStartNewConversationButton()
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I see user name user2Name in Search result list") {
            pages.searchPage.assertUsernameInSearchResultIs(member1.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4502")
    @Category("regression", "RC", "search")
    @Test
    fun givenConnectedPersonalUsers_whenISearchForContactByUniqueUsername_thenContactIsDisplayed() {
        step("Given There are 2 users where user1Name is me") {
            clientUserManager.createXPersonalUsers(2, backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            backendClient.updateUserProfileImage(teamOwner, context)
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And User Myself is connected to user2Name") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user2Name")
        }

        step("And User user2Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        loginToStagingAs(teamOwner)

        step("And I see conversation user2Name in conversation list") {
            pages.conversationListPage.assertConversationVisible(member1.name ?: "")
        }

        step("When I start a new conversation and search for user2Name by unique username") {
            pages.conversationListPage.tapStartNewConversationButton()
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I see user name user2Name in Search result list") {
            pages.searchPage.assertUsernameInSearchResultIs(member1.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4515")
    @Category("regression", "search", "RC", "WPB-3261")
    @Test
    fun givenExistingGroupConversation_whenIOpenNewConversation_thenItsMembersAreSuggested() {
        step("Given There is a team owner user1Name with team AddGroup") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AddGroup",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user2Name,user3Name,user4Name to team AddGroup with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "AddGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name,user4Name in team AddGroup") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name,user4Name",
                "AddGroup"
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        loginToStagingAs(teamOwner)

        step("And I open group conversation MyTeam") {
            pages.conversationListPage.tapConversationNameInConversationList("MyTeam")
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("And I open the group details page") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.assertGroupDetailsPageVisible()
        }

        step("And I open the Participants tab and see user2Name, user3Name and user4Name") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                assertUsernameIsAddedToParticipantsList(member1.name ?: "")
                assertUsernameIsAddedToParticipantsList(member2.name ?: "")
                assertUsernameIsAddedToParticipantsList(member3.name ?: "")
            }
        }

        step("And I close the group details and conversation view") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("When I tap on start a new conversation button") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step("Then I see user2Name, user3Name and user4Name in the search suggestions list") {
            pages.searchPage.apply {
                assertUsernameInSearchResultIs(member1.name ?: "")
                assertUsernameInSearchResultIs(member2.name ?: "")
                assertUsernameInSearchResultIs(member3.name ?: "")
            }
        }
    }

    // Inbound/Outbound search settings
    // For details, see:
    // https://wearezeta.atlassian.net/wiki/spaces/ENGINEERIN/pages/566035910/Searching+for+users
    @Suppress("LongMethod")
    @TestCaseId("TC-4506")
    @Category("regression", "RC", "search")
    @Test
    fun givenAnotherTeamIsSearchableByAllTeams_whenISearchByExactOrPartialHandle_thenUserIsDisplayed() {
        step("Given There is a team owner user1Name with team Searchers") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Searchers",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user3Name to team Searchers with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user3Name",
                "Searchers",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And There is a team owner user2Name with team SearchEnabled") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "SearchEnabled",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user3Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            secondTeamOwner = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And User user2Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        loginToStagingAs(member1)

        step("And I tap on start a new conversation button") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step(
            "And TeamOwner user2Name sets the search behaviour for SearchVisibilityInbound " +
                "to SearchableByAllTeams for team SearchEnabled"
        ) {
            backendSetupHelper.setSearchVisibilityInbound("user2Name", "SearchEnabled", true)
        }

        step("When I search for user2Name by exact unique username") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I see user name user2Name in Search result list") {
            pages.searchPage.assertUsernameInSearchResultIs(secondTeamOwner.name ?: "")
        }

        step("When I clear the search and enter user2UniqueUsername partially") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeUniqueUserNamePartiallyInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I see user name user2Name in Search result list") {
            pages.searchPage.assertUsernameInSearchResultIs(secondTeamOwner.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4507", "TC-4508")
    @Category("regression", "RC", "search")
    @Test
    fun givenAnotherTeamIsSearchableByOwnTeam_whenISearchByNameOrHandle_thenOnlyExactHandleFindsUser() {
        step("Given There is a team owner user1Name with team Searchers") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Searchers",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user3Name to team Searchers with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user3Name",
                "Searchers",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And There is a team owner user2Name with team SearchDisabled") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "SearchDisabled",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user3Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            secondTeamOwner = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And User user2Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        step(
            "And TeamOwner user2Name sets the search behaviour for SearchVisibilityInbound " +
                "to SearchableByOwnTeam for team SearchDisabled"
        ) {
            backendSetupHelper.setSearchVisibilityInbound("user2Name", "SearchDisabled", false)
        }

        loginToStagingAs(member1)

        step("And I tap on start a new conversation button") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step("When I search for user2Name by full name") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I do not see unique user name user2Name in Search result list") {
            pages.searchPage.assertUniqueUsernameNotInSearchResult(secondTeamOwner.uniqueUsername ?: "")
        }

        step("When I clear the search and enter the first 5 characters of user2Name") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeFirstCharactersOfUserNameInSearchField(clientUserManager, "user2Name", 5)
            }
        }

        step("Then I do not see user name user2UniqueUsername in Search result list") {
            pages.searchPage.assertUniqueUsernameNotInSearchResult(secondTeamOwner.uniqueUsername ?: "")
        }

        // TC-4508 - I want to be able to find a user from another team through exact handle,
        // if they have SearchableByOwnTeam enabled.
        step("When I clear the search and enter the exact unique username user2UniqueUsername") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeUniqueUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I see unique user name user2UniqueUsername in Search result list") {
            pages.searchPage.assertUniqueUsernameInSearchResultIs(secondTeamOwner.uniqueUsername ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4509", "TC-4510")
    @Category("regression", "RC", "search")
    @Test
    fun givenMyTeamHidesNamesOutsideTeam_whenISearchAnotherTeam_thenOnlyExactHandleFindsUser() {
        step("Given There is a team owner user1Name with team Searchers") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Searchers",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user3Name to team Searchers with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user3Name",
                "Searchers",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And There is a team owner user2Name with team ToSearch") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "ToSearch",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user3Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            secondTeamOwner = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And User user2Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        loginToStagingAs(member1)

        step("And I tap on start a new conversation button") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step(
            "And TeamOwner user1Name enables TeamSearchVisibility and sets it " +
                "to SearchVisibilityNoNameOutsideTeam for team Searchers"
        ) {
            backendSetupHelper.setTeamSearchVisibilityEnabled("user1Name", "Searchers", true)
            backendSetupHelper.setTeamSearchVisibility("user1Name", "Searchers", "no-name-outside-team")
        }

        step("When I search for user2Name by full name") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I do not see user name user2Name in Search result list") {
            pages.searchPage.assertUsernameNotInSearchResult(secondTeamOwner.name ?: "")
        }

        // TC-4510 - I want to find a user from another team by their exact handle,
        // if my team has SearchVisibilityNoNameOutsideTeam enabled.
        step("When I clear the search and enter the exact unique username user2UniqueUsername") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeUniqueUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I see user name user2Name in Search result list") {
            pages.searchPage.assertUsernameInSearchResultIs(secondTeamOwner.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4511", "TC-4512")
    @Category("regression", "RC", "search")
    @Test
    fun givenMyTeamHidesNamesOutsideTeam_whenISearchPersonalUser_thenOnlyExactHandleFindsUser() {
        step("Given There is a team owner user1Name with team Searchers") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Searchers",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user2Name to team Searchers with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Searchers",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And There is a personal user user3Name") {
            clientUserManager.createPersonalUsersByAliases(listOf("user3Name"), backendClient)
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            personalUser = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And User user3Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user3Name")
            }
        }

        step("And Personal user user3Name sets profile image") {
            backendClient.updateUserProfileImage(personalUser, context)
        }

        loginToStagingAs(member1)

        step("And I tap on start a new conversation button") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step(
            "And TeamOwner user1Name enables TeamSearchVisibility and sets it " +
                "to SearchVisibilityNoNameOutsideTeam for team Searchers"
        ) {
            backendSetupHelper.setTeamSearchVisibilityEnabled("user1Name", "Searchers", true)
            backendSetupHelper.setTeamSearchVisibility("user1Name", "Searchers", "no-name-outside-team")
        }

        step("When I search for user3Name by full name") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUserNameInSearchField(clientUserManager, "user3Name")
            }
        }

        step("Then I do not see user name user3Name in Search result list") {
            pages.searchPage.assertUsernameNotInSearchResult(personalUser.name ?: "")
        }

        // TC-4512 - I want to find a personal user by their exact handle,
        // if my team has SearchVisibilityNoNameOutsideTeam enabled.
        step("When I clear the search and enter the exact unique username user3UniqueUsername") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeUniqueUserNameInSearchField(clientUserManager, "user3Name")
            }
        }

        step("Then I see user name user3Name in Search result list") {
            pages.searchPage.assertUsernameInSearchResultIs(personalUser.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4513", "TC-4514")
    @Category("regression", "RC", "search")
    @Test
    fun givenIAmPersonalUser_whenISearchTeamOwner_thenOnlyExactHandleFindsUser() {
        step("Given There is a team owner user1Name with team Searchers") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Searchers",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And There is a personal user user2Name") {
            clientUserManager.createPersonalUsersByAliases(listOf("user2Name"), backendClient)
            personalUser = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User user1Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
        }

        step("And User user2Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        step("And Personal user user2Name sets profile image") {
            backendClient.updateUserProfileImage(personalUser, context)
        }

        step("And User user2Name is me") {
            clientUserManager.setSelfUser(personalUser)
        }

        loginToStagingAs(personalUser)

        step("When I start a new conversation and search for user1Name by exact unique username") {
            pages.conversationListPage.tapStartNewConversationButton()
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(clientUserManager, "user1Name")
            }
        }

        step("Then I see unique user name user1UniqueUsername in Search result list") {
            pages.searchPage.assertUniqueUsernameInSearchResultIs(teamOwner.uniqueUsername ?: "")
        }

        // TC-4514 - I should not be able to find a user from another team through full text search
        // as a personal user.
        step("When I clear the search and enter the first 5 characters of user1Name") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeFirstCharactersOfUserNameInSearchField(clientUserManager, "user1Name", 5)
            }
        }

        step("Then I do not see unique user name user1UniqueUsername in Search result list") {
            pages.searchPage.assertUniqueUsernameNotInSearchResult(teamOwner.uniqueUsername ?: "")
        }

        step("When I clear the search and enter the full name user1Name") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeUserNameInSearchField(clientUserManager, "user1Name")
            }
        }

        step("Then I do not see unique user name user1UniqueUsername in Search result list") {
            pages.searchPage.assertUniqueUsernameNotInSearchResult(teamOwner.uniqueUsername ?: "")
        }
    }

    // Keeps the repeated staging login flow consistent across search scenarios.
    private fun loginToStagingAs(user: ClientUser) {
        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link and proceed to login") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterUserIdentifier(user.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterUserPassword(user.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.waitUntilConversationPageVisibleDismissingPostLoginPrompts()
        }
    }
}
