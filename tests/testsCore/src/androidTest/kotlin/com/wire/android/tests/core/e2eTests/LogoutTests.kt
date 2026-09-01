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
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class LogoutTests : BaseUiTest() {

    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4445")
    @Category("regression", "RC", "logout")
    @Test
    fun givenTeamOwner_whenILogInAndLogOut_thenLogoutIsSuccessful() {
        step("Given There is a team owner TeamOwner with team Logout") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Logout",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User TeamOwner is me") {
            clientUserManager.setSelfUser(teamOwner)
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
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
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

        step("And I tap User Profile Button and see User Profile Page") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.iSeeUserProfilePage()
        }

        step("When I tap log out button on User Profile Page") {
            pages.selfUserProfilePage.tapLogoutButton()
        }

        step("Then I see alert informing me that I am about to clear my data when I log out") {
            pages.selfUserProfilePage.iSeeClearDataOnLogOutAlert()
        }

        step("When I tap log out button on clear data alert") {
            pages.selfUserProfilePage.tapLogoutButton()
        }

        step("Then I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4447")
    @Category("regression", "RC", "logout")
    @Test
    fun givenTeamMemberWithConversation_whenILogOutWithoutClearingDataAndLogInAgain_thenHistoryIsPreserved() {
        prepareMemberConversationAndLogin()

        step("And I see and open conversation TeamOwner") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwner.name ?: "")
                tapConversationNameInConversationList(teamOwner.name ?: "")
            }
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And User TeamOwner sends message Hello to you, too! to Member1") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user1Name",
                "Hello to you, too!",
                "Device1",
                "user2Name"
            )
        }

        step("And I see message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("And I return to conversation list and open User Profile Page") {
            closeKeyboardIfOpened()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.iSeeUserProfilePage()
        }

        step("When I log out without clearing my data") {
            pages.selfUserProfilePage.apply {
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                tapLogoutButton()
            }
        }

        step("Then I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link and sign in again") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
                enterTeamMemberLoggingEmail(member1.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(member1.password ?: "")
                clickLoginButton()
            }
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }

        step("Then I see conversation list and open conversation TeamOwner") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationVisible(teamOwner.name ?: "")
                tapConversationNameInConversationList(teamOwner.name ?: "")
            }
        }

        step("And I see message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4446")
    @Category("regression", "RC", "logout")
    @Test
    fun givenTeamMemberWithConversation_whenILogOutWithClearData_thenConversationHistoryIsRemoved() {
        prepareMemberConversationAndLogin()

        step("And I see and open conversation TeamOwner") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwner.name ?: "")
                tapConversationNameInConversationList(teamOwner.name ?: "")
            }
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And User TeamOwner sends message Hello to you, too! to Member1") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user1Name",
                "Hello to you, too!",
                "Device1",
                "user2Name"
            )
        }

        step("And I see message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("And I return to conversation list and open User Profile Page") {
            closeKeyboardIfOpened()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.iSeeUserProfilePage()
        }

        step("When I tap log out button and see the clear data alert") {
            pages.selfUserProfilePage.apply {
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
            }
        }

        step("And I select the option to clear my data and log out") {
            pages.selfUserProfilePage.apply {
                iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                tapInfoTextCheckbox()
                tapLogoutButton()
            }
        }

        step("Then I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link and sign in again") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
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

        step("Then I see and open conversation TeamOwner") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwner.name ?: "")
                tapConversationNameInConversationList(teamOwner.name ?: "")
            }
        }

        step("And I do not see message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertMessageNotVisible("Hello to you, too!")
        }
    }

    // Shared setup for TC-4446 and TC-4447; each test keeps its distinct logout behavior explicit.
    private fun prepareMemberConversationAndLogin() {
        step("Given There is a team owner TeamOwner with team Logout") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Logout",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 and Member2 to team Logout with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Logout",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Logout") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Logout"
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
    }

}
