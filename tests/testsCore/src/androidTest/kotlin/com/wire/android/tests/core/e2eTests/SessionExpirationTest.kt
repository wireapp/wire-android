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
import backendUtils.team.deleteTeamMember
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
class SessionExpirationTest : BaseUiTest() {

    private var teamOwner: ClientUser? = null
    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, teamOwner) }
    }

    @TestCaseId("TC-4539")
    @Category("sessionExpiration", "regression", "RC")
    @Test
    fun givenLoggedInTeamMember_whenAccountIsRemovedFromTeam_thenDeletedAccountAlertLogsOutUser() {
        prepareTeamMember()
        loginCurrentUser()

        step("Remove current user from team through backend") {
            teamOwner?.deleteTeamMember(backendClient, currentUser?.id.orEmpty())
        }

        step("Verify deleted account alert and return to welcome page") {
            pages.loginPage.apply {
                assertDeletedAccountAlertVisible()
                clickOkButtonOnDeletedAccountAlert()
            }
            pages.registrationPage.assertEmailWelcomePage()
        }
    }

    @TestCaseId("TC-4537")
    @Category("sessionExpiration", "regression", "RC")
    @Ignore("Blocked: no Kotlin helper mapped yet for removing all registered OTR clients")
    @Test
    fun givenLoggedInTeamMember_whenAllClientsAreRemoved_thenRemovedDeviceAlertLogsOutUser() {
        // TC mapping only. Needs backend helper parity for removing all registered OTR clients.
    }

    @TestCaseId("TC-4538")
    @Category("sessionExpiration", "regression", "RC")
    @Ignore("Blocked: multi-backend multiple-account flow needs ANTA/BELLA environment support")
    @Test
    fun givenMultipleAccounts_whenClientIsDeleted_thenRemainingAccountBecomesActive() {
        // TC mapping only. Needs multi-backend account setup and switching parity.
    }

    @TestCaseId("TC-4848")
    @Category("sessionExpiration", "security")
    @Ignore(
        "Blocked: column removed-device flow needs column backend login, email 2FA, remove-all-clients helper parity, " +
            "removed-device alert subtext assertion, and post-alert signed-out assertion."
    )
    @Test
    fun givenColumnLoggedInUser_whenAllClientsAreRemoved_thenRemovedDeviceAlertLogsOutUser() = Unit

    @TestCaseId("TC-4850")
    @Category("sessionExpiration", "multipleAccounts")
    @Ignore(
        "Blocked: column multi-account client-removal flow needs column backend login, email 2FA, stable multi-account " +
            "add/switch/logout helpers, and remove-all-clients helper parity."
    )
    @Test
    fun givenColumnMultipleAccounts_whenClientsAreRemoved_thenRemainingAccountFlowIsMappedOnly() = Unit

    @TestCaseId("TC-4849")
    @Category("sessionExpiration", "security")
    @Ignore(
        "Blocked: column removed-account flow needs column backend login, email 2FA, deleted-account alert subtext, " +
            "restart, and invalid-credentials assertion parity."
    )
    @Test
    fun givenColumnLoggedInUser_whenAccountIsRemoved_thenDeletedAccountAndReloginFailureAreMappedOnly() = Unit

    private fun prepareTeamMember() {
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
            teamOwner = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            currentUser = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
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
        const val TEAM_NAME = "SessionExpiration"
    }
}
