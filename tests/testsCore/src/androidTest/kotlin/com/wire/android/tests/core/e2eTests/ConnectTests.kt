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
class ConnectTests : BaseUiTest() {
    override val deletePersonalUsersAfterTest = true

    private lateinit var personalUser: ClientUser
    private lateinit var contact: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4295", "TC-4298")
    @Category("regression", "RC", "connect")
    @Test
    fun givenPersonalUser_whenSendingConnectionRequestToAnotherPersonalUser_thenConnectionRequestIsSent() {
        step("Given There are personal users user1Name and user2Name") {
            clientUserManager.createPersonalUsersByAliases(
                listOf("user1Name", "user2Name"),
                backendClient
            )
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            contact = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And users user1Name and user2Name set their unique usernames") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
        }

        step("And Personal user user2Name sets profile image") {
            backendClient.updateUserProfileImage(contact, context)
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
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(personalUser.password ?: "")
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

        step("When I search for user2Name and open their profile") {
            pages.conversationListPage.tapStartNewConversationButton()
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(clientUserManager, "user2Name")
                assertUsernameInSearchResultIs(contact.name ?: "")
                tapUsernameInSearchResult(contact.name ?: "")
            }
        }

        step("Then I see user2Name on the unconnected user profile page") {
            pages.unconnectedUserProfilePage.assertUserNameInUnconnectedUserProfilePage(contact.name ?: "")
        }

        step("When I send a connection request and return to conversation list") {
            pages.unconnectedUserProfilePage.apply {
                clickConnectionRequestButton()
                clickCloseButtonOnUnconnectedUserProfilePage()
            }
            pages.searchPage.clickCloseButtonOnSearchInputField()
            pages.conversationListPage.clickCloseButtonOnNewConversationScreen()
        }

        step("Then I see user2Name conversation has pending status") {
            pages.conversationListPage
                .assertConversationNameWithPendingStatusVisibleInConversationList(contact.name ?: "")
        }

        // TC-4298 - I want to verify that I cannot send a message before my contact accepts my connection request
        step("When I open the pending conversation") {
            pages.conversationListPage.tapConversationNameInConversationList(contact.name ?: "")
        }

        step("Then I see user2Name unconnected profile and connection request information") {
            pages.unconnectedUserProfilePage.apply {
                assertUserNameInUnconnectedUserProfilePage(contact.name ?: "")
                assertConnectionRequestInformationTextIsDisplayed(
                    "When your connection request is accepted, you can communicate directly with this contact."
                )
            }
        }

        step("And I close the unconnected user profile page") {
            pages.unconnectedUserProfilePage.clickCloseButtonOnUnconnectedUserProfilePage()
        }

        step("When user2Name accepts all connection requests") {
            backendSetupHelper.userAcceptsAllIncomingConnectionRequests("user2Name", backendClient)
        }

        step("Then I see user2Name conversation no longer has pending status") {
            pages.conversationListPage.assertPendingStatusIsNoLongerVisible()
        }

        step("And I see user2Name conversation and open it") {
            pages.conversationListPage.apply {
                assertConversationVisible(contact.name ?: "")
                tapConversationNameInConversationList(contact.name ?: "")
            }
        }

        step("When I send the message Hello!") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
            }
        }

        step("Then I see the message Hello! in the current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4296")
    @Category("regression", "RC", "connect")
    @Test
    fun givenPersonalUser_whenCancellingConnectionRequest_thenConnectionRequestIsCancelled() {
        step("Given There are personal users user1Name and user2Name") {
            clientUserManager.createPersonalUsersByAliases(
                listOf("user1Name", "user2Name"),
                backendClient
            )
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            contact = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And users user1Name and user2Name set their unique usernames") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
        }

        step("And Personal user user2Name sets profile image") {
            backendClient.updateUserProfileImage(contact, context)
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
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(personalUser.password ?: "")
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

        step("When I search for user2Name and open their profile") {
            pages.conversationListPage.tapStartNewConversationButton()
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(clientUserManager, "user2Name")
                assertUsernameInSearchResultIs(contact.name ?: "")
                tapUsernameInSearchResult(contact.name ?: "")
            }
        }

        step("Then I see user2Name on the unconnected user profile page") {
            pages.unconnectedUserProfilePage.assertUserNameInUnconnectedUserProfilePage(contact.name ?: "")
        }

        step("When I send the connection request") {
            pages.unconnectedUserProfilePage.clickConnectionRequestButton()
        }

        step("And I see the cancel connection request button and cancel the request") {
            pages.unconnectedUserProfilePage.apply {
                assertCancelConnectionRequestButtonVisible()
                clickCancelConnectionRequestButton()
            }
        }

        step("And I close the unconnected profile and return to conversation list") {
            pages.unconnectedUserProfilePage.clickCloseButtonOnUnconnectedUserProfilePage()
            pages.searchPage.clickCloseButtonOnSearchInputField()
            pages.conversationListPage.apply {
                clickCloseButtonOnNewConversationScreen()
                assertConversationListVisible()
            }
        }

        step("Then I do not see user2Name conversation") {
            pages.conversationListPage.assertConversationNotVisible(contact.name ?: "")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4297")
    @Category("regression", "RC", "connect", "smoke", "smokeSchwarz")
    @Test
    fun givenPersonalUser_whenReceivingConnectionRequest_thenConnectionRequestCanBeAccepted() {
        step("Given There are personal users user1Name and user2Name") {
            clientUserManager.createPersonalUsersByAliases(
                listOf("user1Name", "user2Name"),
                backendClient
            )
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            contact = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And users user1Name and user2Name set their unique usernames") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
        }

        step("And Personal user user2Name sets profile image") {
            backendClient.updateUserProfileImage(contact, context)
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
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(personalUser.password ?: "")
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

        step("When user2Name sends a connection request to me") {
            backendSetupHelper.connectionRequestIsSentTo("user2Name", "user1Name")
        }

        step("Then I see a connection request from user2Name") {
            pages.conversationListPage.assertConnectionRequestNameIs(contact.name ?: "")
        }

        step("And I see Wants to connect subtitle for user2Name") {
            pages.conversationListPage.assertConversationSubtitleVisible("Wants to connect")
        }

        step("And I open the connection request from user2Name") {
            pages.conversationListPage.clickConnectionRequestOfUser(contact.name ?: "")
        }

        step("And I see user2Name connection request information and actions") {
            pages.unconnectedUserProfilePage.apply {
                assertUserNameInUnconnectedUserProfilePage(contact.name ?: "")
                assertAcceptButtonIsDisplayed()
                assertIgnoreButtonIsDisplayed()
                assertConnectionRequestNotificationTextIsDisplayed()
            }
        }

        step("When I accept the connection request") {
            pages.unconnectedUserProfilePage.clickAcceptButton()
        }

        step("And I see the request was accepted and start a conversation") {
            pages.connectedUserProfilePage.apply {
                assertToastMessageIsDisplayed("Connection request accepted")
                clickStartConversationButton()
            }
        }

        step("Then I see conversation view with user2Name in foreground") {
            pages.conversationViewPage.assertConversationIsVisibleWithUser(contact.name ?: "")
        }
    }
}
