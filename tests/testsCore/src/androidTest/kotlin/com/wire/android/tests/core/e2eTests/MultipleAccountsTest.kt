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
class MultipleAccountsTest : BaseUiTest() {

    private var teamOwner: ClientUser? = null
    private var firstMember: ClientUser? = null
    private var secondMember: ClientUser? = null

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

    @TestCaseId("TC-4457")
    @Category("multipleAccounts", "login", "regression", "RC")
    @Test
    fun givenTwoTeamAccounts_whenAddingSecondAccount_thenSecondAccountBecomesActive() {
        prepareTeamWithTwoMembers()
        loginUser(firstMember)

        step("Verify first account is active before adding another account") {
            openSelfUserProfile()
            pages.selfUserProfilePage.assertCurrentAccountIs(firstMember?.name.orEmpty())
        }

        step("Start adding a new team or account") {
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginUser(secondMember)

        step("Verify second account is active after login") {
            openSelfUserProfile()
            pages.selfUserProfilePage.assertCurrentAccountIs(secondMember?.name.orEmpty())
        }
    }

    @TestCaseId("TC-4458")
    @Category("multipleAccounts", "regression", "RC")
    @Ignore("Blocked: needs stable account-switching and unread-conversation assertions after logging in two accounts.")
    @Test
    fun givenTwoTeamAccounts_whenSecondAccountSendsMessage_thenFirstAccountReceivesIt() {
        // TC mapping only. Needs multi-account switch and unread-count helper parity.
    }

    @TestCaseId("TC-4459")
    @Category("multipleAccounts", "logout", "regression", "RC")
    @Ignore("Blocked/stale: legacy flow depends on ANTA/BELLA multi-backend account setup plus three-account logout ordering.")
    @Test
    fun givenThreeAccountsAcrossBackends_whenLoggingOut_thenExpectedAccountRemainsActive() {
        // TC mapping only. Needs old cross-backend ANTA/BELLA multi-account setup.
    }

    @TestCaseId("TC-4702", "TC-4703")
    @Category("multipleAccounts")
    @Ignore(
        "Blocked: column multiple-account logout flow needs column backend login, email 2FA, stable two-account " +
            "switch/logout helpers, and the current two-account limit alert selectors."
    )
    @Test
    fun givenTwoColumnAccounts_whenLoggingOutAndAddingThirdAccount_thenAccountLimitFlowIsMappedOnly() = Unit

    private fun prepareTeamWithTwoMembers() {
        step("Prepare backend team owner and two members") {
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
                "$FIRST_MEMBER_ALIAS,$SECOND_MEMBER_ALIAS",
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            firstMember = teamHelper.usersManager.findUserBy(FIRST_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            secondMember = teamHelper.usersManager.findUserBy(SECOND_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginUser(user: ClientUser?) {
        step("Login ${user?.name.orEmpty()} via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamOwnerLoggingEmail(user?.email.orEmpty())
                clickLoginButton()
                enterTeamOwnerLoggingPassword(user?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("Complete login flow for ${user?.name.orEmpty()}") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun openSelfUserProfile() {
        step("Open self user profile") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.iSeeUserProfilePage()
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val FIRST_MEMBER_ALIAS = "user2Name"
        const val SECOND_MEMBER_ALIAS = "user3Name"
        const val TEAM_NAME = "MultiAccount"
    }
}
