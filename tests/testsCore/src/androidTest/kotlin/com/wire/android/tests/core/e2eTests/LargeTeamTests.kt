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

import CredentialsManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.client.getBackendClientIds
import backendUtils.client.removeBackendClient
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class LargeTeamTests : BaseUiTest() {

    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var member2: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @After
    fun tearDown() {
        runCatching { testServiceHelper.testServiceClient.cleanUp() }
    }

    // TODO: Increase the Kalium Test Service timeout so Member1 can log in and send a message.
    // TODO: Re-enable the commented steps after the timeout is increased.
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12517", "TC-12698")
    @Category("regression", "RC", "largeTeams")
    @Test
    fun givenIAmInLargeTeam_whenICreateGroupAndExchangeMessages_thenGroupIsCreatedAndMessagesAreReceived() {
        step("Given There is a known user TeamOwner") {
            teamOwner = ClientUser(
                "Emery",
                "Kuvalis",
                requiredSecret("LARGE_TEAM_OWNER_LOGIN_DETAILS", "USERNAME"),
                requiredSecret("LARGE_TEAM_OWNER_LOGIN_DETAILS", "PASSWORD")
            ).apply {
                hardcoded = true
                backendName = backendClient.name
                nameAliases.add("user1Name")
            }
            clientUserManager.appendCustomUser(teamOwner)
        }

        step("And There is a known user Member1") {
            member1 = ClientUser(
                "Solomon",
                "Conroy",
                requiredSecret("LARGE_TEAM_MEMBER_LOGIN_DETAILS", "USERNAME"),
                requiredSecret("LARGE_TEAM_MEMBER_LOGIN_DETAILS", "PASSWORD")
            ).apply {
                hardcoded = true
                backendName = backendClient.name
                nameAliases.add("user2Name")
            }
            clientUserManager.appendCustomUser(member1)
        }

        step("And There is a known user Member2") {
            member2 = ClientUser(
                "Florencio",
                "Larkin",
                requiredSecret("LARGE_TEAM_MEMBER2_LOGIN_DETAILS", "USERNAME"),
                requiredSecret("LARGE_TEAM_MEMBER2_LOGIN_DETAILS", "PASSWORD")
            ).apply {
                hardcoded = true
                backendName = backendClient.name
                nameAliases.add("user3Name")
            }
            clientUserManager.appendCustomUser(member2)
        }

        step("And TeamOwner, Member1 and Member2 remove all their registered OTR clients") {
            listOf(teamOwner, member1, member2).forEach { user ->
                backendClient.getBackendClientIds(user).forEach { clientId ->
                    backendClient.removeBackendClient(user, clientId)
                }
            }
        }

        step("And Member1 signs in to staging and completes the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }

        step("And I open Add account from Member1's profile") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewAccountButton()
        }

        step("And Member2 signs in to staging as the second account and completes the login prompts") {
            clientUserManager.setSelfUser(member2)
            loginToStagingAs(member2)
        }

        step("And I open Add account from Member2's profile") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewAccountButton()
        }

        step("And TeamOwner signs in to staging as the third account and completes the login prompts") {
            clientUserManager.setSelfUser(teamOwner)
            loginToStagingAs(teamOwner)
        }

        step("When I start a new conversation and tap New Group") {
            pages.conversationListPage.tapStartNewConversationButton()
            pages.searchPage.tapCreateNewGroupButton()
        }

        step("And I add Member1 and Member2 as participants") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUserNameInSearchField(clientUserManager, "Solomon Conroy")
                UiWaitUtils.waitElement(UiSelectorParams(text = "@881d169258"))
                assertUsernameInSearchResultIs("@881d169258")
                tapUsernameInSearchResult("@881d169258")
                clearSearchInputField()
                typeUserNameInSearchField(clientUserManager, "Florencio Larkin")
                UiWaitUtils.waitElement(UiSelectorParams(text = "@9d966fc182"))
                assertUsernameInSearchResultIs("@9d966fc182")
                tapUsernameInSearchResult("@9d966fc182")
                clearSearchInputField()
                tapContinueButtonOnAddParticipantsPage()
            }
        }

        step("And I enter MyTeam as group name and continue") {
            pages.createNewGroupPage.apply {
                assertCreateNewGroupDetailsPageVisible()
                enterNewGroupName("MyTeam")
                tapContinueButtonOnGroupDetailsPage()
            }
        }

        step("And I see group settings and create the group") {
            pages.createNewGroupPage.apply {
                assertCreateNewGroupSettingsPageVisible()
                tapCreateGroupButtonOnGroupSettingsPage()
            }
        }

        step("Then I see group conversation MyTeam is in foreground") {
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("When I open MyTeam group details and delete the conversation") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.apply {
                tapShowMoreOptionsButton()
                tapDeleteConversationButton()
                tapDeleteGroupButton()
            }
        }

        step("Then I see conversation list and do not see MyTeam") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationNotVisible("MyTeam")
            }
        }

        step("When I open group conversation Full House") {
            pages.conversationListPage.clickGroupConversation("Full House")
            pages.conversationViewPage.assertGroupConversationInForeground("Full House")
        }

        step("And I send message Hello and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello")
            }
        }

        // step("And User Member1 sends message Hi to group conversation Full House") {
        //     testServiceHelper.userSendMessageToConversation(
        //         "user2Name",
        //         "Hi",
        //         "Device1",
        //         "Full House"
        //     )
        // }

        // step("Then I see the message Hi in current conversation") {
        //     pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hi")
        // }
    }

    // Shared app login flow: selects staging, signs in, and clears post-login prompts.
    private fun loginToStagingAs(user: ClientUser) {
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
            clickContinueButtonOnBackendConfigSuccess()
            enterUserIdentifier(user.email.orEmpty())
            clickLoginButton()
            enterUserPassword(user.password.orEmpty())
            clickLoginButton()
        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsCompleted()
            clickAllowNotificationButton()
            clickDeclineShareDataAlert()
        }
        pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
    }

    private fun requiredSecret(parentKey: String, fieldKey: String): String =
        CredentialsManager.getSecretFieldValue(parentKey, fieldKey)
            ?.takeIf { it.isNotBlank() }
            ?: error("Missing secret [$parentKey/$fieldKey] in generated test BuildConfig.")
}
