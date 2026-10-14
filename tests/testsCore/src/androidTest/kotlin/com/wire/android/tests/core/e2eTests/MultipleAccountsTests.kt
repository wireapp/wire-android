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
import org.junit.Before
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class MultipleAccountsTests : BaseUiTest() {

    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var member2: ClientUser
    private lateinit var memberStaging: ClientUser
    private lateinit var memberAnta: ClientUser
    private lateinit var memberBella: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4457")
    @Category("regression", "RC", "login", "multipleAccounts")
    @Test
    fun givenTwoTeamMembers_whenILogInWithBothAccounts_thenBothAccountsAreLoggedInSimultaneously() {
        step("Given There is a team owner TeamOwner with team MultiAccount") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "MultiAccount",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds users Member1 and Member2 to team MultiAccount with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "MultiAccount",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginToBackendAs(member1)

        step("And I open User Profile and see Member1 is my currently active account") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(member1.name ?: "")
            }
        }

        step("When I tap New Team or Account button") {
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
        }

        step("And User Member2 is me") {
            clientUserManager.setSelfUser(member2)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginToBackendAs(member2)

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I open User Profile and see Member2 is my currently active account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(member2.name ?: "")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4458")
    @Category("regression", "RC", "multipleAccounts")
    @Test
    fun givenTwoLoggedInAccounts_whenISendMessageFromSecondAccount_thenFirstAccountReceivesIt() {
        step("Given There is a team owner TeamOwner with team MultiAccount") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "MultiAccount",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds users Member1 and Member2 to team MultiAccount with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "MultiAccount",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team MultiAccount") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "MultiAccount"
            )
        }

        step("And User Member1 has 1:1 conversation with Member2 in team MultiAccount") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user3Name",
                "MultiAccount"
            )
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginToBackendAs(member1)

        step("And I see conversations TeamOwner and Member2 in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwner.name ?: "")
                assertConversationVisible(member2.name ?: "")
            }
        }

        step("And I open User Profile Page") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.iSeeUserProfilePage()
        }

        step("When I tap New Team or Account button") {
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
        }

        step("Then I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And User Member2 is me") {
            clientUserManager.setSelfUser(member2)
        }

        loginToBackendAs(member2)

        step("Then I see conversation list and conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationVisible(member1.name ?: "")
            }
        }

        step("When I open conversation Member1") {
            pages.conversationListPage.tapConversationNameInConversationList(member1.name ?: "")
        }

        step("And I send message Hello! and see it in the current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("And I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I open User Profile and see Member1 under other logged in accounts") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.assertOtherLoggedInAccountVisible(member1.name ?: "")
        }

        step("When I switch to Member1 account") {
            pages.selfUserProfilePage.tapOtherAccountByName(member1.name ?: "")
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("And I see conversation list with 1 unread message from Member2") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationHasUnreadMessagesCount(member2.name ?: "", "1")
            }
        }

        step("And I open unread conversation Member2") {
            pages.conversationListPage.tapUnreadConversationNameInConversationList(member2.name ?: "")
        }

        step("Then I see message Hello! in the current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4459")
    @Category("regression", "RC", "logout", "multipleAccounts")
    @Test
    fun givenThreeAccountsAreLoggedIn_whenILogOutEachAccount_thenAllAccountsAreLoggedOut() {
        val antaBackend = BackendClient.loadBackend("ANTA")
        val bellaBackend = BackendClient.loadBackend("BELLA")

        step("Given Login with 3 accounts is enabled on the build") {
            assumeFalse(
                "Login with three accounts is not supported by the Bund build.",
                UiAutomatorSetup.appPackage == "com.wire.android.bund"
            )
        }

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

        step("Then I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
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

        step("When I log out MemberBella from User Profile Page") {
            pages.selfUserProfilePage.apply {
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                tapLogoutButton()
            }
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

        step("When I log out MemberStaging from User Profile Page") {
            pages.selfUserProfilePage.apply {
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                tapLogoutButton()
            }
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

        step("When I log out MemberAnta from User Profile Page") {
            pages.selfUserProfilePage.apply {
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                tapLogoutButton()
            }
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
}
