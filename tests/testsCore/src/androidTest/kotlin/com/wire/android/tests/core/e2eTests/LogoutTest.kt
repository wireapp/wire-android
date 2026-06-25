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

import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class LogoutTest : BaseUiTest() {

    private var currentUser: ClientUser? = null
    private var teamOwner: ClientUser? = null

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

    @TestCaseId("TC-4445")
    @Category("logout", "regression", "RC")
    @Test
    fun givenLoggedInTeamOwner_whenLoggingOut_thenWelcomePageIsShown() {
        prepareTeamOwnerOnly()
        loginCurrentUser(declineShareData = true)

        step("Open self profile and logout without selecting clear-data checkbox") {
            logout(clearData = false)
        }

        step("Verify email welcome page is shown after logout") {
            pages.registrationPage.assertEmailWelcomePage()
        }
    }

    @TestCaseId("TC-4447")
    @Category("logout", "regression", "RC")
    @Ignore(
        "Blocked: even after VPN/test-service connectivity was restored, backend send cannot resolve the owner/member 1:1 conversation for this setup."
    )
    @Test
    fun givenSoftLogout_whenLoggingInAgain_thenConversationHistoryIsStillVisible() {
        prepareTeamWithMemberConversation()
        loginCurrentUser(declineShareData = true)
        sendAndReceiveMessagesInOwnerConversation()

        step("Logout without clearing data") {
            logout(clearData = false)
        }

        step("Login again without answering share-data prompt") {
            loginCurrentUser(declineShareData = false)
        }

        step("Verify conversation and backend message are still visible") {
            openOwnerConversation()
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(BACKEND_MESSAGE)
        }
    }

    @TestCaseId("TC-4446")
    @Category("logout", "regression", "RC")
    @Ignore("Blocked: shares TC-4447 backend-send setup; clear-data behavior cannot be validated until owner/member 1:1 lookup is stable.")
    @Test
    fun givenHardLogoutWithClearData_whenLoggingInAgain_thenLocalConversationHistoryIsCleared() {
        prepareTeamWithMemberConversation()
        loginCurrentUser(declineShareData = true)
        sendAndReceiveMessagesInOwnerConversation()

        step("Logout with clear-data checkbox selected") {
            logout(clearData = true)
        }

        step("Login again and decline share-data prompt") {
            loginCurrentUser(declineShareData = true)
        }

        step("Verify owner conversation is visible but previous backend message is not in local history") {
            openOwnerConversation()
            pages.conversationViewPage.assertMessageNotVisible(BACKEND_MESSAGE)
        }
    }

    private fun prepareTeamOwnerOnly() {
        step("Prepare backend team owner") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                TEAM_OWNER_ALIAS,
                TEAM_NAME,
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            currentUser = teamOwner
        }
    }

    private fun prepareTeamWithMemberConversation() {
        step("Prepare backend team, members, and 1:1 conversation") {
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
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            testServiceHelper.userHas1on1ConversationInTeam(TEAM_MEMBER_ALIAS, TEAM_OWNER_ALIAS, TEAM_NAME)
            teamOwner = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            currentUser = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser(declineShareData: Boolean) {
        step("Login current user via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterPersonalUserLoggingEmail(currentUser?.email.orEmpty())
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(currentUser?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("Complete login flow") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                if (declineShareData) {
                    clickDeclineShareDataAlert()
                }
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun sendAndReceiveMessagesInOwnerConversation() {
        step("Open owner conversation and send local message") {
            openOwnerConversation()
            pages.conversationViewPage.apply {
                typeMessageInInputField(LOCAL_MESSAGE)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(LOCAL_MESSAGE)
            }
        }

        step("Team owner sends backend message to current user") {
            sendBackendMessageToCurrentUser()
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(BACKEND_MESSAGE)
        }

        step("Return to conversation list") {
            device.pressBack()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }
    }

    private fun openOwnerConversation() {
        val ownerName = teamOwner?.name.orEmpty()
        pages.conversationListPage.apply {
            assertConversationVisible(ownerName)
            clickGroupConversation(ownerName)
        }
        pages.conversationViewPage.assertConversationScreenVisible()
    }

    private fun sendBackendMessageToCurrentUser() {
        var lastError: Throwable? = null
        repeat(BACKEND_SEND_ATTEMPTS) { attempt ->
            val result = runCatching {
                testServiceHelper.userSendMessageToPersonalMlsConversation(
                    TEAM_OWNER_ALIAS,
                    BACKEND_MESSAGE,
                    DEVICE_NAME,
                    TEAM_MEMBER_ALIAS,
                    false
                )
            }.recoverCatching {
                testServiceHelper.userSendMessageToConversation(
                    TEAM_OWNER_ALIAS,
                    BACKEND_MESSAGE,
                    DEVICE_NAME,
                    TEAM_MEMBER_ALIAS,
                    false
                )
            }
            if (result.isSuccess) {
                return
            }
            lastError = result.exceptionOrNull()
            if (attempt < BACKEND_SEND_ATTEMPTS - 1) {
                UiWaitUtils.waitFor(BACKEND_SEND_RETRY_DELAY_SECONDS.seconds)
            }
        }
        throw AssertionError("Team owner could not send backend message to current user.", lastError)
    }

    private fun logout(clearData: Boolean) {
        pages.conversationListPage.apply {
            clickConversationsMenuEntry()
            clickConversationsButtonOnMenuEntry()
            clickUserProfileButton()
        }
        pages.selfUserProfilePage.apply {
            iSeeUserProfilePage()
            tapLogoutButton()
            iSeeClearDataOnLogOutAlert()
            if (clearData) {
                iSeeInfoTextCheckbox(CLEAR_DATA_TEXT)
                tapInfoTextCheckbox()
            }
            tapLogoutButtonOnClearDataAlert()
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val EXTRA_MEMBER_ALIAS = "user3Name"
        const val TEAM_NAME = "Logout"
        const val LOCAL_MESSAGE = "Hello!"
        const val BACKEND_MESSAGE = "Hello to you, too!"
        const val DEVICE_NAME = "Device1"
        const val CLEAR_DATA_TEXT = "Delete all your personal information and conversations on this device"
        const val BACKEND_SEND_ATTEMPTS = 6
        const val BACKEND_SEND_RETRY_DELAY_SECONDS = 5
    }
}
