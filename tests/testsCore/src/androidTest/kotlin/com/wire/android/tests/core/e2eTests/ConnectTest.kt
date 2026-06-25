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
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ConnectTest : BaseUiTest() {

    override val deletePersonalUsersAfterTest = true

    private lateinit var currentUser: ClientUser
    private lateinit var otherUser: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @TestCaseId("TC-4295", "TC-4298")
    @Category("connect", "regression", "RC")
    @Test
    fun givenPersonalUsers_whenSendingConnectionRequest_thenMessagingWorksAfterRequestIsAccepted() {
        preparePersonalUsers()
        loginCurrentUser()

        sendConnectionRequestFromSearch()
        closeSearchAndAssertPendingConversation()

        step("Assert pending connection cannot send messages yet") {
            pages.conversationListPage.tapConversationNameInConversationList(otherUser.name.orEmpty())
            pages.unconnectedUserProfilePage.apply {
                assertUserNameInUnconnectedUserProfilePage(otherUser.name.orEmpty())
                assertOutgoingConnectionInfoTextIsDisplayed()
            }
            device.pressBack()
        }

        step("Other user accepts all incoming requests via backend") {
            runBlocking {
                backendClient.acceptAllIncomingConnectionRequests(otherUser)
            }
        }

        step("Open accepted conversation and send a message") {
            pages.conversationListPage.apply {
                assertPendingStatusIsNoLongerVisible()
                tapConversationNameInConversationList(otherUser.name.orEmpty())
            }
            pages.conversationViewPage.apply {
                UiWaitUtils.waitFor(5.seconds)
                typeMessageInInputField(MESSAGE)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(MESSAGE)
            }
        }
    }

    @TestCaseId("TC-4296")
    @Category("connect", "regression", "RC")
    @Test
    fun givenPersonalUsers_whenCancelingConnectionRequest_thenConversationIsRemovedFromList() {
        preparePersonalUsers()
        loginCurrentUser()

        step("Send and cancel connection request from profile") {
            openOtherUserFromSearch()
            pages.unconnectedUserProfilePage.apply {
                assertUserNameInUnconnectedUserProfilePage(otherUser.name.orEmpty())
                clickConnectionRequestButton()
                assertCancelConnectionRequestButtonVisible()
                clickCancelConnectionRequestButton()
                clickCloseButtonOnUnconnectedUserProfilePage()
            }
        }

        step("Close search and assert conversation is not visible") {
            pages.searchPage.clickCloseButtonOnSearchInputField()
            pages.conversationListPage.clickCloseButtonOnNewConversationScreen()
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationNotVisible(otherUser.name.orEmpty())
            }
        }
    }

    @TestCaseId("TC-4297")
    @Category("connect", "regression", "RC", "smoke")
    @Test
    fun givenIncomingConnectionRequest_whenAcceptingIt_thenConversationCanBeOpened() {
        preparePersonalUsers()
        loginCurrentUser()

        step("Other user sends connection request via backend") {
            testServiceHelper.connectionRequestIsSentTo(OTHER_USER_ALIAS, CURRENT_USER_ALIAS)
        }

        step("Open incoming connection request") {
            pages.conversationListPage.apply {
                assertConnectionRequestNameIs(otherUser.name.orEmpty())
                assertWantsToConnectSubtitleVisible()
                clickConnectionRequestOfUser(otherUser.name.orEmpty())
            }
        }

        step("Accept request and start conversation") {
            pages.unconnectedUserProfilePage.apply {
                assertUserNameInUnconnectedUserProfilePage(otherUser.name.orEmpty())
                assertConnectionRequestNotificationTextIsDisplayed()
                assertAcceptButtonIsDisplayed()
                assertIgnoreButtonIsDisplayed()
                clickAcceptButton()
            }
            pages.connectedUserProfilePage.apply {
                UiWaitUtils.waitFor(1.seconds)
                assertStartConversationButtonVisible()
                clickStartConversationButton()
            }
            pages.conversationViewPage.assertConversationIsVisibleWithTeamMember(otherUser.name.orEmpty())
        }
    }

    private fun preparePersonalUsers() {
        step("Prepare personal users") {
            val users = teamHelper.usersManager.createPersonalUsersByAliases(
                listOf(CURRENT_USER_ALIAS, OTHER_USER_ALIAS),
                backendClient
            )
            currentUser = users.first()
            otherUser = users.last()
            runBlocking {
                testServiceHelper.usersSetUniqueUsername(OTHER_USER_ALIAS)
            }
        }
    }

    private fun loginCurrentUser() {
        step("Login current personal user via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterPersonalUserLoggingEmail(currentUser.email.orEmpty())
                clickLoginButton()
                enterPersonalUserLoginPassword(currentUser.password.orEmpty())
                clickLoginButton()
            }
        }

        step("Complete login flow") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                setUserNameIfVisible(currentUser.uniqueUsername.orEmpty())
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun sendConnectionRequestFromSearch() {
        step("Search other user and send connection request") {
            openOtherUserFromSearch()
            pages.unconnectedUserProfilePage.apply {
                assertUserNameInUnconnectedUserProfilePage(otherUser.name.orEmpty())
                clickConnectionRequestButton()
            }
            pages.connectedUserProfilePage.assertToastMessageIsDisplayed("Connection request sent")
        }
    }

    private fun openOtherUserFromSearch() {
        pages.conversationListPage.tapStartNewConversationButton()
        pages.searchPage.apply {
            tapSearchPeopleField()
            typeRawTextInSearchField(otherUser.uniqueUsername.orEmpty())
            assertUsernameInSearchResultIs(otherUser.name.orEmpty())
            tapUsernameInSearchResult(otherUser.name.orEmpty())
        }
    }

    private fun closeSearchAndAssertPendingConversation() {
        step("Close search and assert pending conversation") {
            pages.unconnectedUserProfilePage.clickCloseButtonOnUnconnectedUserProfilePage()
            pages.searchPage.clickCloseButtonOnSearchInputField()
            pages.conversationListPage.clickCloseButtonOnNewConversationScreen()
            pages.conversationListPage
                .assertConversationNameWithPendingStatusVisibleInConversationList(otherUser.name.orEmpty())
        }
    }

    private companion object {
        const val CURRENT_USER_ALIAS = "user1Name"
        const val OTHER_USER_ALIAS = "user2Name"
        const val MESSAGE = "Hello!"
    }
}
