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
class FederationTests : BaseUiTest() {
    override val deletePersonalUsersAfterTest = true

    private lateinit var teamOwnerB: ClientUser
    private lateinit var teamOwnerBella: ClientUser
    private lateinit var teamMember: ClientUser
    private lateinit var personalUser: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers("anta")
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    // ######################
    // Inbound/Outbound search settings
    // ######################

    @Suppress("LongMethod")
    @TestCaseId("TC-4099")
    @Category("regression", "RC", "federation", "federationSearch")
    @Test
    fun givenAnotherTeamHasSearchableByAllTeamsEnabled_whenISearchByExactHandleOrFullText_thenIFindUserFromThatTeam() {
        step("Given There is a team owner TeamOwnerA with team Searchers on anta backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Searchers",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwnerA adds Member1 to team Searchers with role Member") {
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

        step("And There is a team owner TeamOwnerB with team SearchEnabled on anta backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "SearchEnabled",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwnerB = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And TeamOwnerA configures MLS for team Searchers") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "Searchers", backendClient)
        }

        step("And TeamOwnerB configures MLS for team SearchEnabled") {
            backendSetupHelper.userConfiguresMLSForTeam("user2Name", "SearchEnabled", backendClient)
        }

        step("And Member1 is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(teamMember)
        }

        step("And TeamOwnerB sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open anta backend deep link") {
            pages.loginPage.clickStagingDeepLink("anta")
        }

        step("And I see the alert informing me that I am about to switch to anta backend") {
            pages.loginPage.assertCustomBackendAlertVisible("anta")
        }

        step("And I tap proceed on the custom backend alert and continue on the backend page") {
            pages.loginPage.apply {
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter Member1's email and tap next to see the user login screen") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
            }
        }

        step("And I enter Member1's password and tap next to login") {
            pages.loginPage.apply {
                enterTeamMemberLoggingPassword(teamMember.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I tap on start a new conversation button") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step("And TeamOwnerB sets SearchVisibilityInbound to SearchableByAllTeams for team SearchEnabled") {
            backendSetupHelper.setSearchVisibilityInbound("user2Name", "SearchEnabled", true)
        }

        step("When I tap on search people field and enter TeamOwnerB's exact unique username") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I see TeamOwnerB in the search results") {
            pages.searchPage.assertUsernameInSearchResultIs(teamOwnerB.name ?: "")
        }

        step("When I clear the search field and enter the first 5 characters of TeamOwnerB's name") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeFirstCharactersOfUserNameInSearchField(clientUserManager, "user2Name", 5)
            }
        }

        step("Then I see TeamOwnerB in the search results") {
            pages.searchPage.assertUsernameInSearchResultIs(teamOwnerB.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4100", "TC-4101")
    @Category("regression", "RC", "federation", "federationSearch")
    @Test
    fun givenAnotherTeamHasSearchableByOwnTeamEnabled_whenISearchByFullTextOrExactHandle_thenOnlyExactHandleFindsUserFromThatTeam() {
        step("Given There is a team owner TeamOwnerA with team Searchers on anta backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Searchers",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwnerA adds Member1 to team Searchers with role Member") {
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

        step("And There is a team owner TeamOwnerB with team SearchDisabled on anta backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "SearchDisabled",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwnerB = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And TeamOwnerA configures MLS for team Searchers") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "Searchers", backendClient)
        }

        step("And TeamOwnerB configures MLS for team SearchDisabled") {
            backendSetupHelper.userConfiguresMLSForTeam("user2Name", "SearchDisabled", backendClient)
        }

        step("And Member1 is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(teamMember)
        }

        step("And TeamOwnerB sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open anta backend deep link") {
            pages.loginPage.clickStagingDeepLink("anta")
        }

        step("And I see the alert informing me that I am about to switch to anta backend") {
            pages.loginPage.assertCustomBackendAlertVisible("anta")
        }

        step("And I tap proceed on the custom backend alert and continue on the backend page") {
            pages.loginPage.apply {
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter Member1's email and tap next to see the user login screen") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
            }
        }

        step("And I enter Member1's password and tap next to login") {
            pages.loginPage.apply {
                enterTeamMemberLoggingPassword(teamMember.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I tap on start a new conversation button") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step("And TeamOwnerB sets SearchVisibilityInbound to SearchableByOwnTeam for team SearchDisabled") {
            backendSetupHelper.setSearchVisibilityInbound("user2Name", "SearchDisabled", false)
        }

        step("When I tap on search people field and enter TeamOwnerB's full name") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I do not see TeamOwnerB in the search results") {
            pages.searchPage.assertNoSearchResultsVisible()
        }

        step("When I clear the search field and enter the first 5 characters of TeamOwnerB's name") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeFirstCharactersOfUserNameInSearchField(clientUserManager, "user2Name", 5)
            }
        }

        step("Then I do not see TeamOwnerB in the search results") {
            pages.searchPage.assertUsernameNotReturnedBySearch(teamOwnerB.name ?: "")
        }

        // TC-4101 - I want to be able to find a user from another team through exact handle,
        // if they have SearchableByOwnTeam enabled.
        step("When I clear the search field and enter TeamOwnerB's exact unique username") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeUniqueUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I see TeamOwnerB in the search results") {
            pages.searchPage.assertUsernameInSearchResultIs(teamOwnerB.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4102", "TC-4103")
    @Category("regression", "RC", "federation", "federationSearch")
    @Test
    fun givenMyTeamEnablesSearchVisibilityNoNameOutsideTeam_whenISearchAnotherTeamUserByFullTextOrHandle_thenOnlyExactHandleFindsThem() {
        step("Given There is a team owner TeamOwnerA with team Searchers on anta backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Searchers",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwnerA adds Member1 to team Searchers with role Member") {
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

        step("And There is a team owner TeamOwnerB with team ToSearch on anta backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "ToSearch",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwnerB = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And TeamOwnerA configures MLS for team Searchers") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "Searchers", backendClient)
        }

        step("And TeamOwnerB configures MLS for team ToSearch") {
            backendSetupHelper.userConfiguresMLSForTeam("user2Name", "ToSearch", backendClient)
        }

        step("And Member1 is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(teamMember)
        }

        step("And TeamOwnerB sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open anta backend deep link") {
            pages.loginPage.clickStagingDeepLink("anta")
        }

        step("And I see the alert informing me that I am about to switch to anta backend") {
            pages.loginPage.assertCustomBackendAlertVisible("anta")
        }

        step("And I tap proceed on the custom backend alert and continue on the backend page") {
            pages.loginPage.apply {
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter Member1's email and tap next to see the user login screen") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
            }
        }

        step("And I enter Member1's password and tap next to login") {
            pages.loginPage.apply {
                enterTeamMemberLoggingPassword(teamMember.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I tap on start a new conversation button") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step("And TeamOwnerA enables TeamSearchVisibility for team Searchers") {
            backendSetupHelper.setTeamSearchVisibilityEnabled("user1Name", "Searchers", true)
        }

        step("And TeamOwnerA sets TeamSearchVisibility to SearchVisibilityNoNameOutsideTeam for team Searchers") {
            backendSetupHelper.setTeamSearchVisibility("user1Name", "Searchers", "no-name-outside-team")
        }

        step("When I tap on search people field and enter TeamOwnerB's full name") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I do not see TeamOwnerB in the search results") {
            pages.searchPage.assertNoSearchResultsVisible()
        }

        // TC-4103 - I want to find a user from another team by their exact handle,
        // if my team has SearchVisibilityNoNameOutsideTeam enabled.
        step("When I clear the search field and enter TeamOwnerB's exact unique username") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeUniqueUserNameInSearchField(clientUserManager, "user2Name")
            }
        }

        step("Then I see TeamOwnerB in the search results") {
            pages.searchPage.assertUsernameInSearchResultIs(teamOwnerB.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4104", "TC-4105")
    @Category("regression", "RC", "federation", "federationSearch")
    @Test
    fun givenMyTeamEnablesSearchVisibilityNoNameOutsideTeam_whenISearchPersonalUserByFullTextOrHandle_thenOnlyExactHandleFindsThem() {
        step("Given There is a team owner TeamOwner with team Searchers on anta backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Searchers",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner adds Member1 to team Searchers with role Member") {
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

        step("And There is a personal user user3Name on anta backend") {
            clientUserManager.createPersonalUsersByAliases(listOf("user3Name"), backendClient)
            personalUser = clientUserManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And TeamOwner configures MLS for team Searchers") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "Searchers", backendClient)
        }

        step("And Member1 is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamMember)
        }

        step("And Personal user user3Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user3Name")
            }
        }

        step("And Personal user user3Name sets their profile image") {
            backendClient.updateUserProfileImage(personalUser, context)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open anta backend deep link") {
            pages.loginPage.clickStagingDeepLink("anta")
        }

        step("And I see the alert informing me that I am about to switch to anta backend") {
            pages.loginPage.assertCustomBackendAlertVisible("anta")
        }

        step("And I tap proceed on the custom backend alert and continue on the backend page") {
            pages.loginPage.apply {
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter Member1's email and tap next to see the user login screen") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
            }
        }

        step("And I enter Member1's password and tap next to login") {
            pages.loginPage.apply {
                enterTeamMemberLoggingPassword(teamMember.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I tap on start a new conversation button") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step("And TeamOwner enables TeamSearchVisibility for team Searchers") {
            backendSetupHelper.setTeamSearchVisibilityEnabled("user1Name", "Searchers", true)
        }

        step("And TeamOwner sets TeamSearchVisibility to SearchVisibilityNoNameOutsideTeam for team Searchers") {
            backendSetupHelper.setTeamSearchVisibility("user1Name", "Searchers", "no-name-outside-team")
        }

        step("When I tap on search people field and enter personal user user3Name's full name") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUserNameInSearchField(clientUserManager, "user3Name")
            }
        }

        step("Then I do not see personal user user3Name in the search results") {
            pages.searchPage.assertNoSearchResultsVisible()
        }

        // TC-4105 - I want to find a personal user by their exact handle,
        // if my team has SearchVisibilityNoNameOutsideTeam enabled.
        step("When I clear the search field and enter personal user user3Name's exact unique username") {
            pages.searchPage.apply {
                clearSearchInputField()
                typeUniqueUserNameInSearchField(clientUserManager, "user3Name")
            }
        }

        step("Then I see personal user user3Name in the search results") {
            pages.searchPage.assertUsernameInSearchResultIs(personalUser.name ?: "")
        }
    }

    // ######################
    // Connect
    // ######################

    @Suppress("LongMethod")
    @TestCaseId("TC-4110")
    @Category("regression", "RC", "federation", "federationConnect")
    @Test
    fun givenUserOnDifferentBackend_whenISendAndCancelConnectionRequest_thenTheirConversationIsNotInList() {
        val chalaBackend = BackendClient.loadBackend("chala")

        step("Given There is a team owner TeamOwnerBella with team Banana on bella backend") {
            initCommonTestHelpers("bella")
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Banana",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And There is a team owner TeamOwnerChala with team Mango on chala backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user3Name",
                "Mango",
                "en_US",
                true,
                chalaBackend,
                context
            )
        }

        step("And TeamOwnerChala adds Member1 to team Mango with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user3Name",
                "user2Name",
                "Mango",
                TeamRoles.Member,
                chalaBackend,
                context,
                true
            )
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And TeamOwnerBella configures MLS for team Banana") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "Banana", backendClient)
        }

        step("And TeamOwnerChala configures MLS for team Mango") {
            backendSetupHelper.userConfiguresMLSForTeam("user3Name", "Mango", chalaBackend)
        }

        step("And Member1 sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        step("And Member1 adds 1 device") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And TeamOwnerBella is me") {
            teamOwnerBella = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwnerBella)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open bella backend deep link") {
            pages.loginPage.clickStagingDeepLink("bella")
        }

        step("And I see the alert informing me that I am about to switch to bella backend") {
            pages.loginPage.assertCustomBackendAlertVisible("bella")
        }

        step("And I tap proceed on the custom backend alert and continue on the backend page") {
            pages.loginPage.apply {
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter TeamOwnerBella's email and tap next to see the user login screen") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwnerBella.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
            }
        }

        step("And I enter TeamOwnerBella's password and tap next to login") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingPassword(teamOwnerBella.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I tap on start a new conversation button") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step("When I tap on search people field") {
            pages.searchPage.tapSearchPeopleField()
        }

        step("And I enter Member1's unique username with @${chalaBackend.domain} in the search field") {
            pages.searchPage.typeFederatedUserNameInSearchField(clientUserManager, "user2Name", domain = chalaBackend.domain)
        }

        step("And I see Member1 in the search results") {
            pages.searchPage.assertUsernameInSearchResultIs(teamMember.name ?: "")
        }

        step("And I tap on Member1 in the search results") {
            pages.searchPage.tapUsernameInSearchResult(teamMember.name ?: "")
        }

        step("Then I see Member1 on the unconnected user profile page") {
            pages.unconnectedUserProfilePage.assertUserNameInUnconnectedUserProfilePage(teamMember.name ?: "")
        }

        step("When I tap connect on the unconnected user profile page") {
            pages.unconnectedUserProfilePage.clickConnectionRequestButton()
        }

        step("And I wait until the cancel connection request button is visible") {
            pages.unconnectedUserProfilePage.assertCancelConnectionRequestButtonVisible()
        }

        step("And I cancel the connection request") {
            pages.unconnectedUserProfilePage.clickCancelConnectionRequestButton()
        }

        step("And I close the unconnected user profile and return to the conversation list") {
            pages.unconnectedUserProfilePage.clickCloseButtonOnUnconnectedUserProfilePage()
            pages.searchPage.clickCloseButtonOnSearchInputField()
            pages.conversationListPage.apply {
                clickCloseButtonOnNewConversationScreen()
                assertConversationListVisible()
            }
        }

        step("Then I do not see Member1's conversation in the conversation list") {
            pages.conversationListPage.assertConversationNotVisible(teamMember.name ?: "")
        }
    }
}
