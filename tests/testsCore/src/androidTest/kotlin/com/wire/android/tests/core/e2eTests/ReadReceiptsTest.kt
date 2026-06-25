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
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class ReadReceiptsTest : BaseUiTest() {

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

    @TestCaseId("TC-4492")
    @Category("readReceipts", "regression", "RC")
    @Test
    fun givenAccountWithReadReceiptsEnabled_whenTurningReadReceiptsOff_thenPrivacySettingIsOff() {
        prepareTeamWithMember()
        loginCurrentUser()

        step("Open Privacy Settings") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.clickPrivacySettingsButtonOnSettingsPage()
        }

        step("Turn send read receipts off") {
            pages.settingsPage.apply {
                assertSendReadReceiptsToggleIsOn()
                tapSendReadReceiptsToggle()
                assertSendReadReceiptsToggleIsOff()
            }
        }
    }

    @Ignore("Blocked: requires another user/device to send/read messages and stable message-details read-receipts selectors.")
    @TestCaseId("TC-4488")
    @Category("readReceipts", "regression", "RC", "blocked")
    @Test
    fun givenOneOnOneConversation_whenRemoteUserReadsMyMessage_thenReadReceiptIsShown() {
        // TC mapping preserved until remote read-receipt helper and message-details page object are available.
    }

    @Ignore("Blocked: requires multi-account switching plus message-details read-receipts selectors.")
    @TestCaseId("TC-4489")
    @Category("readReceipts", "regression", "RC", "blocked")
    @Test
    fun givenOneOnOneConversation_whenIReadOtherAccountMessage_thenReadReceiptIsSent() {
        // TC mapping preserved until stable multi-account read-receipt flow is available.
    }

    @Ignore("Blocked: requires multiple remote devices sending read receipts and message-details read-receipts selectors.")
    @TestCaseId("TC-4490", "TC-4491")
    @Category("readReceipts", "regression", "RC", "blocked")
    @Test
    fun givenGroupConversation_whenMembersReadMessage_thenReadReceiptsAreShown() {
        // TC mapping preserved until remote read-receipt helper and message-details page object are available.
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
        }

        step("Team owner has 1:1 conversation with member") {
            testServiceHelper.userHas1on1ConversationInTeam(TEAM_OWNER_ALIAS, TEAM_MEMBER_ALIAS, TEAM_NAME)
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

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "ReadReceipts"
    }
}
