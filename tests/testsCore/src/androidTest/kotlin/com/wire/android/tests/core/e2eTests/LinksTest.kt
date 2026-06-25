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
class LinksTest : BaseUiTest() {

    private var currentUser: ClientUser? = null

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

    @TestCaseId("TC-4433")
    @Category("links", "regression", "RC")
    @Test
    fun givenReceivedLink_whenOpeningIt_thenBrowserShowsTargetPage() {
        prepareGroupConversation()
        loginCurrentUser()

        step("Open group conversation and send link") {
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
                typeMessageInInputField(LINK)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(LINK)
            }
        }

        step("Open link and verify browser navigation") {
            pages.conversationViewPage.apply {
                tapLinkInCurrentConversation(LINK)
                assertVisitLinkDialogVisible(LINK)
                tapOpenButtonOnVisitLinkDialog()
                assertWireAppIsNotInForeground()
            }
            pages.chromePage.apply {
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
                assertUrlContains(EXPECTED_URL_PART)
            }
        }
    }

    @TestCaseId("TC-4434")
    @Category("links", "regression", "RC")
    @Test
    fun givenLinkInLongConversationAfterRestart_whenOpeningIt_thenBrowserShowsTargetPage() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupConversation()

        step("Send link, open it once, and verify browser navigation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField(LINK)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(LINK)
                tapLinkInCurrentConversation(LINK)
                assertVisitLinkDialogVisible(LINK)
                tapOpenButtonOnVisitLinkDialog()
                assertWireAppIsNotInForeground()
            }
            pages.chromePage.apply {
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
                assertUrlContains(EXPECTED_URL_PART)
            }
        }

        step("Restart Wire without clearing local data") {
            restartWireWithoutClearingData()
            assertWireAppIsInForeground()
            ensureGroupConversationOpen()
        }

        step("Create a longer conversation after restart") {
            repeat(DEFAULT_MESSAGES_COUNT) { index ->
                sendLocalMessage("Link default message ${index + 1}")
            }
            sendLocalMessage(LONG_CONVERSATION_MESSAGE)
            sendLocalMessage(REMOTE_PARITY_MESSAGE)
        }

        step("Scroll through conversation and open original link again") {
            pages.conversationViewPage.apply {
                scrollToTopOfConversationScreen()
                scrollToBottomOfConversationScreen()
                scrollToTopOfConversationScreen()
                tapLinkInCurrentConversation(LINK)
                assertVisitLinkDialogVisible(LINK)
                tapOpenButtonOnVisitLinkDialog()
                assertWireAppIsNotInForeground()
            }
            pages.chromePage.assertUrlContains(EXPECTED_URL_PART)
        }
    }

    private fun prepareGroupConversation() {
        step("Prepare backend team owner, member, and group conversation") {
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
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                TEAM_MEMBER_ALIAS,
                TEAM_NAME
            )
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser() {
        step("Login team owner via staging deep link") {
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

    private fun openGroupConversation() {
        pages.conversationListPage.apply {
            assertConversationVisible(GROUP_CONVERSATION_NAME)
            tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
        }
        pages.conversationViewPage.assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
    }

    private fun ensureGroupConversationOpen() {
        val conversationReady = runCatching {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
                assertConversationScreenVisible()
            }
        }.isSuccess
        if (conversationReady) {
            return
        }
        openGroupConversation()
    }

    private fun sendLocalMessage(message: String) {
        var lastError: Throwable? = null
        repeat(SEND_MESSAGE_ATTEMPTS) { attempt ->
            val result = runCatching {
                pages.conversationViewPage.apply {
                    typeMessageInInputField(message)
                    clickSendButton()
                    assertSentMessageIsVisibleInCurrentConversation(message)
                }
            }
            if (result.isSuccess) {
                return
            }
            lastError = result.exceptionOrNull()
            if (attempt < SEND_MESSAGE_ATTEMPTS - 1) {
                device.waitForIdle()
            }
        }
        throw AssertionError("Could not send local message '$message'", lastError)
    }

    private fun restartWireWithoutClearingData() {
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA, clearData = false)
    }

    private fun assertWireAppIsInForeground() {
        val foregroundPackage = device.currentPackageName
        if (foregroundPackage != UiAutomatorSetup.appPackage) {
            throw AssertionError("Wire app is not in foreground. Current package: $foregroundPackage")
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Linking"
        const val GROUP_CONVERSATION_NAME = "WeLikeLinks"
        const val LINK = "www.github.com"
        const val LONG_CONVERSATION_MESSAGE = "That is a lot of messages"
        const val REMOTE_PARITY_MESSAGE = "Yes!"
        const val EXPECTED_URL_PART = "github"
        const val DEFAULT_MESSAGES_COUNT = 20
        const val SEND_MESSAGE_ATTEMPTS = 3
    }
}
