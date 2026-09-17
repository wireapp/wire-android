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
import backendUtils.client.getBackendClientIds
import backendUtils.client.removeBackendClient
import backendUtils.team.TeamRoles
import backendUtils.team.deleteTeamMember
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class SessionExpirationTests : BaseUiTest() {

    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var memberStaging: ClientUser
    private lateinit var memberAnta: ClientUser
    private lateinit var memberBella: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4537")
    @Category("regression", "RC", "sessionExpiration")
    @Test
    fun givenLoggedInTeamMember_whenTheirDeviceIsRemoved_thenAppropriateDeviceIsSignedOut() {
        step("Given There is a team owner TeamOwner with team SessionExpiration") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "SessionExpiration",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team SessionExpiration with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "SessionExpiration",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
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
                enterTeamMemberLoggingEmail(member1.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(member1.password ?: "")
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

        step("When User Member1 removes all their registered OTR clients") {
            val registeredClientIds = backendClient.getBackendClientIds(member1)
            if (registeredClientIds.isEmpty()) {
                throw AssertionError("No registered OTR clients were found for Member1.")
            }
            registeredClientIds.forEach { clientId ->
                backendClient.removeBackendClient(member1, clientId)
            }
        }

        step("Then I see the removed device alert and expected subtext") {
            pages.commonAppPage.apply {
                assertRemovedDeviceDialogVisible()
                assertRemovedDeviceDialogSubtextVisible(
                    "You were logged out because your device was removed."
                )
            }
        }

        step("When I tap OK button on the alert") {
            pages.commonAppPage.confirmRemovedDeviceDialog()
        }

        step("Then I see email input Page") {
            pages.loginPage.assertEmailInputPageVisible()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4538")
    @Category("regression", "RC", "sessionExpiration", "multipleAccounts")
    @Test
    fun givenThreeLoggedInAccounts_whenEachClientIsRemoved_thenAccountsAreLoggedOutInSequence() {
        val antaBackend = BackendClient.loadBackend("ANTA")
        val bellaBackend = BackendClient.loadBackend("BELLA")

        step("And There is a team owner TeamOwnerStaging with team TeamStaging") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "TeamStaging",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwnerStaging adds MemberStaging to team TeamStaging with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user4Name",
                "TeamStaging",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And There is a team owner TeamOwnerAnta with team TeamAnta on anta backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "TeamAnta",
                "en_US",
                true,
                antaBackend,
                context
            )
        }

        step("And User TeamOwnerAnta adds MemberAnta to team TeamAnta with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user2Name",
                "user5Name",
                "TeamAnta",
                TeamRoles.Member,
                antaBackend,
                context,
                true
            )
        }

        step("And There is a team owner TeamOwnerBella with team TeamBella on bella backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user3Name",
                "TeamBella",
                "en_US",
                true,
                bellaBackend,
                context
            )
        }

        step("And User TeamOwnerBella adds MemberBella to team TeamBella with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user3Name",
                "user6Name",
                "TeamBella",
                TeamRoles.Member,
                bellaBackend,
                context,
                true
            )
        }

        step("And User MemberStaging is me") {
            memberStaging = clientUserManager.findUserByNameOrNameAlias("user4Name")
            memberAnta = clientUserManager.findUserByNameOrNameAlias("user5Name")
            memberBella = clientUserManager.findUserByNameOrNameAlias("user6Name")
            clientUserManager.setSelfUser(memberStaging)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginToBackendAs(memberStaging)

        step("And I open User Profile and see MemberStaging is my currently active account") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(memberStaging.name ?: "")
            }
        }

        step("When I tap New Team or Account button") {
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
        }

        step("Then I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And User MemberAnta is me") {
            clientUserManager.setSelfUser(memberAnta)
        }

        loginToBackendAs(memberAnta, declineShareDataAlert = false)

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I open User Profile and see MemberAnta is my currently active account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(memberAnta.name ?: "")
            }
        }

        step("When I tap New Team or Account button") {
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
        }

        step("And User MemberBella is me") {
            clientUserManager.setSelfUser(memberBella)
        }

        loginToBackendAs(memberBella, declineShareDataAlert = false)

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I open User Profile and see all three logged-in accounts") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(memberBella.name ?: "")
                assertOtherLoggedInAccountVisible(memberAnta.name ?: "")
                assertOtherLoggedInAccountVisible(memberStaging.name ?: "")
            }
        }

        step("When User MemberBella removes all their registered OTR clients") {
            removeAllRegisteredClients(memberBella, bellaBackend)
        }

        step("Then I see the removed device alert") {
            pages.commonAppPage.assertRemovedDeviceDialogVisible()
        }

        step("When I tap OK button on the alert") {
            pages.commonAppPage.confirmRemovedDeviceDialog()
        }

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I open User Profile and see MemberStaging is active with MemberAnta also logged in") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(memberStaging.name ?: "")
                assertOtherLoggedInAccountVisible(memberAnta.name ?: "")
            }
        }

        step("When User MemberStaging removes all their registered OTR clients") {
            removeAllRegisteredClients(memberStaging, backendClient)
        }

        step("Then I see the removed device alert") {
            pages.commonAppPage.assertRemovedDeviceDialogVisible()
        }

        step("When I tap OK button on the alert") {
            pages.commonAppPage.confirmRemovedDeviceDialog()
        }

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I open User Profile and see only MemberAnta is logged in") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(memberAnta.name ?: "")
                assertNoOtherAccountsLoggedIn()
            }
        }

        step("When User MemberAnta removes all their registered OTR clients") {
            removeAllRegisteredClients(memberAnta, antaBackend)
        }

        step("Then I see the removed device alert") {
            pages.commonAppPage.assertRemovedDeviceDialogVisible()
        }

        step("When I tap OK button on the alert") {
            pages.commonAppPage.confirmRemovedDeviceDialog()
        }

        step("Then I see email input Page") {
            pages.loginPage.assertEmailInputPageVisible()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4539")
    @Category("regression", "RC", "sessionExpiration")
    @Test
    fun givenIAmLoggedIn_whenMyAccountIsRemovedFromTeam_thenIAmAutomaticallyLoggedOut() {
        step("Given There is a team owner TeamOwner with team SessionExpiration") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "SessionExpiration",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team SessionExpiration with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "SessionExpiration",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginToBackendAs(member1)

        step("When User TeamOwner removes user Member1 from team SessionExpiration") {
            teamOwner.deleteTeamMember(
                backendClient,
                member1.id ?: error("Member1 has no backend user ID.")
            )
        }

        step("Then I see the deleted account alert and expected subtext") {
            pages.commonAppPage.apply {
                assertDeletedAccountDialogVisible()
                assertDeletedAccountDialogSubtextVisible(
                    "You were logged out because your account was deleted."
                )
            }
        }

        step("When I tap OK button on the alert") {
            pages.commonAppPage.confirmDeletedAccountDialog()
        }

        step("Then I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }
    }

    // Keeps the repeated backend login flow consistent for each account.
    private fun loginToBackendAs(user: ClientUser, declineShareDataAlert: Boolean = true) {
        val backendName = user.backendName ?: error("Backend is missing for ${user.name}.")
        step("And I open ${backendName.lowercase()} backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink(backendName)
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterUserIdentifier(user.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterUserPassword(user.password ?: "")
                clickLoginButton()
            }
        }

        step(
            if (declineShareDataAlert) {
                "And I wait until I am fully logged in and decline share data alert"
            } else {
                "And I wait until I am fully logged in"
            }
        ) {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                if (declineShareDataAlert) {
                    clickDeclineShareDataAlert()
                }
            }
        }
    }

    // Removes every registered client from the same backend as the account under test.
    private fun removeAllRegisteredClients(user: ClientUser, backend: BackendClient) {
        val registeredClientIds = backend.getBackendClientIds(user)
        if (registeredClientIds.isEmpty()) {
            throw AssertionError("No registered OTR clients were found for ${user.name}.")
        }
        registeredClientIds.forEach { clientId ->
            backend.removeBackendClient(user, clientId)
        }
    }
}
