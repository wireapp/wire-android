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
class ConversationListFilterTests : BaseUiTest() {
    override val deletePersonalUsersAfterTest = true

    private lateinit var member: ClientUser
    private lateinit var contact: ClientUser
    private lateinit var teamOwner: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @TestCaseId("TC-4299")
    @Category("regression", "RC", "conversationListFilter", "search")
    @Test
    fun givenConnectedPersonalUser_whenSearchingForContactByFullName_thenContactIsVisibleInSearchResults() {
        givenPersonalUserIsLoggedInWithConnectedContact()

        step("When I search for contact user2Name by full name") {
            pages.conversationListPage.apply {
                tapSearchConversationField()
                typeConversationNameInSearchField(contact.name ?: "")
            }
        }

        step("Then I see conversation user2Name in search result list") {
            pages.conversationListPage.assertConversationVisible(contact.name ?: "")
        }
    }

    @TestCaseId("TC-4300")
    @Category("regression", "RC", "conversationListFilter", "search")
    @Test
    fun givenTeamOwnerWithGroupConversation_whenSearchingByFullName_thenGroupConversationIsVisibleInSearchResults() {
        givenTeamOwnerIsLoggedInWithSearchGroupConversation("Search GroupChat")

        step("When I search for conversation Search GroupChat by full name") {
            pages.conversationListPage.apply {
                tapSearchConversationField()
                typeConversationNameInSearchField("Search GroupChat")
            }
        }

        step("Then I see conversation Search GroupChat in search result list") {
            pages.conversationListPage.assertGroupConversationVisible("Search GroupChat")
        }
    }

    @TestCaseId("TC-4302")
    @Category("regression", "RC", "conversationListFilter", "search")
    @Test
    fun givenTeamOwnerWithGroupConversation_whenSearchingByPartialName_thenGroupConversationIsVisibleInSearchResults() {
        givenTeamOwnerIsLoggedInWithSearchGroupConversation("Search GroupChat")

        step("When I search for conversation Search GroupChat by its first 3 characters") {
            pages.conversationListPage.apply {
                tapSearchConversationField()
                typeFirstNCharsInSearchField("Search GroupChat", 3)
            }
        }

        step("Then I see conversation Search GroupChat in search result list") {
            pages.conversationListPage.assertGroupConversationVisible("Search GroupChat")
        }
    }

    @TestCaseId("TC-4303")
    @Category("regression", "RC", "conversationListFilter", "search")
    @Test
    fun givenTeamOwnerWithGroupConversation_whenSearchingInLowercase_thenGroupConversationIsVisibleInSearchResults() {
        givenTeamOwnerIsLoggedInWithSearchGroupConversation("Search GroupChat")

        step("When I search for conversation Search GroupChat in lower case") {
            pages.conversationListPage.apply {
                tapSearchConversationField()
                typeConversationNameInSearchField("Search GroupChat".lowercase())
            }
        }

        step("Then I see conversation Search GroupChat in search result list") {
            pages.conversationListPage.assertGroupConversationVisible("Search GroupChat")
        }
    }

    @TestCaseId("TC-4304")
    @Category("regression", "RC", "conversationListFilter", "search")
    @Test
    fun givenTeamOwnerWithGroupConversation_whenSearchingInUppercase_thenGroupConversationIsVisibleInSearchResults() {
        givenTeamOwnerIsLoggedInWithSearchGroupConversation("Search GroupChat")

        step("When I search for conversation Search GroupChat in upper case") {
            pages.conversationListPage.apply {
                tapSearchConversationField()
                typeConversationNameInSearchField("Search GroupChat".uppercase())
            }
        }

        step("Then I see conversation Search GroupChat in search result list") {
            pages.conversationListPage.assertGroupConversationVisible("Search GroupChat")
        }
    }

    @TestCaseId("TC-4301")
    @Category("regression", "RC", "conversationListFilter", "search")
    @Test
    fun givenConnectedPersonalUser_whenSearchingForContactByPartialName_thenContactIsVisibleInSearchResults() {
        givenPersonalUserIsLoggedInWithConnectedContact()

        step("When I search for contact user2Name by the first 3 characters of their name") {
            pages.conversationListPage.apply {
                tapSearchConversationField()
                typeFirstNCharsInSearchField(contact.name ?: "", 3)
            }
        }

        step("Then I see conversation user2Name in search result list") {
            pages.conversationListPage.assertConversationVisible(contact.name ?: "")
        }
    }

    @TestCaseId("TC-4305")
    @Category("regression", "RC", "conversationListFilter", "search")
    @Test
    fun givenExistingGroupConversation_whenSearchingForNonMatchingConversation_thenNoSearchResultIsVisible() {
        givenTeamOwnerIsLoggedInWithSearchGroupConversation("Search Groupchat")

        step("When I search for a random conversation name") {
            val randomConversationName = List(5) { ('a'..'z').random() }.joinToString("")
            pages.conversationListPage.apply {
                tapSearchConversationField()
                typeConversationNameInSearchField(randomConversationName)
            }
        }

        step("Then I do not see conversation Search Groupchat in search result list") {
            pages.conversationListPage.assertConversationNotVisible("Search Groupchat")
        }
    }

    // Shared personal-user setup: creates connected users, logs in as user1Name, and verifies the contact conversation.
    private fun givenPersonalUserIsLoggedInWithConnectedContact() {
        step("Given There are 2 personal users where user1Name is me") {
            clientUserManager.createXPersonalUsers(2, backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
            member = clientUserManager.findUserByNameOrNameAlias("user1Name")
            contact = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member)
        }

        step("And User user2Name is connected to user1Name") {
            backendSetupHelper.userIsConnectedTo("user2Name", "user1Name")
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(member.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(member.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation user2Name in conversation list") {
            pages.conversationListPage.assertConversationVisible(contact.name ?: "")
        }
    }

    // Shared team setup: creates the group, logs in as user1Name, and verifies the group conversation.
    @Suppress("LongMethod")
    private fun givenTeamOwnerIsLoggedInWithSearchGroupConversation(groupConversationName: String) {
        step("Given There is a team owner user1Name with team Messaging") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Messaging",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user2Name to team Messaging with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Messaging",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation $groupConversationName with user2Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                groupConversationName,
                "user2Name",
                "Messaging"
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation $groupConversationName in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible(groupConversationName)
        }
    }
}
