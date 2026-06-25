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
@file:Suppress("ArgumentListWrapping", "MaximumLineLength")

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
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class SelfDeletingMessagesTest : BaseUiTest() {

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

    @TestCaseId("TC-4523")
    @Category("selfDeletingMessages", "regression", "RC", "smoke")
    @Test
    fun givenGroupConversation_whenOpeningSelfDeletingTimer_thenAllTimerOptionsAreVisible() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupConversation()

        step("Open self-deleting message timer options") {
            pages.conversationViewPage.apply {
                tapMessageInInputField()
                tapSelfDeleteTimerButton()
            }
        }

        step("Verify available self-deleting timer options") {
            TIMER_OPTIONS.forEach { option ->
                pages.conversationViewPage.assertSelfDeleteOptionVisible(option)
            }
        }
    }

    @Ignore(
        "Blocked: current Pixel 9a run cannot find the self-deleting timer button after focusing the composer; needs updated selector or product setup."
    )
    @TestCaseId("TC-4517")
    @Category("selfDeletingMessages", "regression", "RC", "smoke")
    @Test
    fun givenGroupConversation_whenSendingTenSecondSelfDeletingMessage_thenMessageExpires() = Unit

    @Ignore("Blocked: source parity waits 60 seconds and requires remote read/send through a second backend device.")
    @TestCaseId("TC-4524")
    @Category("selfDeletingMessages", "regression")
    @Test
    fun givenGroupConversation_whenSendingOneMinuteSelfDeletingMessage_thenMessageExpires() = Unit

    @Ignore("Blocked: receiving ephemeral messages requires stable backend device setup and test-service send-ephemeral wrapper.")
    @TestCaseId("TC-4518", "TC-4520", "TC-4522", "TC-4526")
    @Category("selfDeletingMessages", "regression", "RC")
    @Test
    fun givenConversation_whenReceivingSelfDeletingPayload_thenMessageExpires() = Unit

    @Ignore("Blocked: shares the current self-deleting timer button selector/setup issue from TC-4517.")
    @TestCaseId("TC-4519")
    @Category("selfDeletingMessages", "regression", "RC")
    @Test
    fun givenOneOnOneConversation_whenSendingTenSecondSelfDeletingMessage_thenMessageExpires() = Unit

    @Ignore("Blocked: self-deleting image/file flows need stable attachment helpers plus expiry assertions.")
    @TestCaseId("TC-4525", "TC-4527", "TC-4531")
    @Category("selfDeletingMessages", "fileSharing", "regression", "RC")
    @Test
    fun givenGroupConversation_whenSendingSelfDeletingAsset_thenAssetExpires() = Unit

    @Ignore("Blocked: enforced group timer settings require dedicated group-details self-deleting-message page helpers.")
    @TestCaseId("TC-4529", "TC-4521", "TC-4530", "TC-4528", "TC-4532")
    @Category("selfDeletingMessages", "regression", "RC")
    @Test
    fun givenGroupConversation_whenChangingEnforcedSelfDeletingTimer_thenSystemMessagesAndMessagesMatchTimer() = Unit

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
            currentUser = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser() {
        step("Login team member via staging deep link") {
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
        step("Open group conversation") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(GROUP_CONVERSATION_NAME)
                clickGroupConversation(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.assertConversationScreenVisible()
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Deleting"
        const val GROUP_CONVERSATION_NAME = "SelfDeleting"

        val TIMER_OPTIONS = listOf(
            "OFF",
            "10 seconds",
            "5 minutes",
            "1 hour",
            "1 day",
            "7 days",
            "4 weeks"
        )
    }
}
