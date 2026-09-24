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
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class StatusTests : BaseUiTest() {

    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8413")
    @Category("regression", "RC", "status")
    @Test
    fun givenIAmATeamMember_whenISetMyStatusToBusy_thenBusyStatusIsDisplayed() {
        step("Given There is a team owner TeamOwner with team Status") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Status",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Status with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Status",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User Member1 is me") {
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

        step("And I open User Profile Page and see change status options") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertAvailabilityOptionsVisible()
            }
        }

        step("When I change my status to Busy") {
            pages.selfUserProfilePage.changeAvailabilityStatus("None", "Busy")
        }

        step("Then I see information about changing my status to Busy") {
            pages.selfUserProfilePage.assertStatusChangeInfoText(
                "You will appear as Busy to other people. You will only receive notifications for mentions, replies, " +
                    "and calls in conversations that are not muted."
            )
        }

        step("And I confirm the status change and see my status is set to Busy") {
            pages.selfUserProfilePage.apply {
                confirmStatusChange()
                assertAvailabilityStatusSelected("Busy")
            }
        }

        step("And I close User Profile Page and see conversation list") {
            pages.selfUserProfilePage.tapCloseProfileButton()
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I see status icon displayed next to my avatar on conversation list") {
            pages.conversationListPage.assertSelfAvailabilityStatusIconVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8412")
    @Category("regression", "RC", "status")
    @Test
    fun givenIAmATeamMember_whenISetMyStatusToAway_thenAwayStatusIsDisplayed() {
        step("Given There is a team owner TeamOwner with team Status") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Status",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Status with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Status",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User Member1 is me") {
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

        step("And I open User Profile Page and see change status options") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertAvailabilityOptionsVisible()
            }
        }

        step("When I change my status to Away") {
            pages.selfUserProfilePage.changeAvailabilityStatus("None", "Away")
        }

        step("Then I see information about changing my status to Away") {
            pages.selfUserProfilePage.assertStatusChangeInfoText(
                "You will appear as Away to other people. You will not receive notifications about any incoming " +
                    "calls or messages."
            )
        }

        step("And I confirm the status change and see my status is set to Away") {
            pages.selfUserProfilePage.apply {
                confirmStatusChange()
                assertAvailabilityStatusSelected("Away")
            }
        }

        step("And I close User Profile Page and see conversation list") {
            pages.selfUserProfilePage.tapCloseProfileButton()
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I see status icon displayed next to my avatar on conversation list") {
            pages.conversationListPage.assertSelfAvailabilityStatusIconVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8414")
    @Category("regression", "RC", "status")
    @Test
    fun givenIAmATeamMember_whenISetMyStatusToAvailable_thenAvailableStatusIsDisplayed() {
        step("Given There is a team owner TeamOwner with team Status") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Status",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Status with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Status",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User Member1 is me") {
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

        step("And I open User Profile Page and see change status options") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertAvailabilityOptionsVisible()
            }
        }

        step("When I change my status to Available") {
            pages.selfUserProfilePage.changeAvailabilityStatus("None", "Available")
        }

        step("Then I see information about changing my status to Available") {
            pages.selfUserProfilePage.assertStatusChangeInfoText(
                "You will appear as Available to other people. You will receive notifications for incoming calls " +
                    "and for messages according to the Notifications setting in each conversation."
            )
        }

        step("And I confirm the status change and see my status is set to Available") {
            pages.selfUserProfilePage.apply {
                confirmStatusChange()
                assertAvailabilityStatusSelected("Available")
            }
        }

        step("And I close User Profile Page and see conversation list") {
            pages.selfUserProfilePage.tapCloseProfileButton()
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I see status icon displayed next to my avatar on conversation list") {
            pages.conversationListPage.assertSelfAvailabilityStatusIconVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8415", "TC-8416", "TC-8417", "TC-8426")
    @Category("regression", "RC", "status")
    @Test
    fun givenAnotherMemberChangesTheirStatus_whenIViewTheirStatusIndicators_thenBusyStatusIsDisplayed() {
        step("Given There is a team owner TeamOwner with team Status") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Status",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Status with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Status",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Status") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Status"
            )
        }

        step("And User TeamOwner has group conversation We like status with Member1 in team Status") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "We like status",
                "user2Name",
                "Status"
            )
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

        // Prekeys need to be exchanged by sending a message in a PROTEUS conversation for availability to be sent.
        step("And I open group conversation We like status") {
            pages.conversationListPage.clickGroupConversation("We like status")
            pages.conversationViewPage.assertGroupConversationInForeground("We like status")
        }

        step("And Member1 sends message Hello! to group conversation We like status") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "We like status"
            )
        }

        step("And I see the message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        // TC-8417 - I want to see in a group conversation when another user changed their status and sends a message.
        step("When Member1 sets availability status to Busy and sends another group message") {
            testServiceHelper.userSetsAvailabilityStatus("user2Name", "Device1", "BUSY")
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "How are you?",
                "Device1",
                "We like status"
            )
        }

        step("Then I see Member1's status icon in the group conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("How are you?")
                assertUserAvailabilityStatusIconVisible(member1.name ?: "")
            }
        }

        // TC-8426 - I want to see on participants list when another user changed their status.
        step("When I open group details and view the participants list") {
            pages.conversationViewPage.clickOnGroupConversationDetails("We like status")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapOnParticipantsTab()
                assertUsernameIsAddedToParticipantsList(member1.name ?: "")
            }
        }

        step("Then I see Member1's status icon in the participants list") {
            pages.groupConversationDetailsPage.assertUserAvailabilityStatusIconVisible(member1.name ?: "")
        }

        // TC-8415 - I want to see on conversation list when another user changed their status.
        step("When I close group details and return to the conversation list") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("Then I see Member1's status icon on the conversation list") {
            pages.conversationListPage.assertUserAvailabilityStatusIconVisible(member1.name ?: "")
        }

        // TC-8416 - I want to see in a 1:1 conversation when another user changed their status.
        step("When I open my 1:1 conversation with Member1") {
            pages.conversationListPage.tapConversationNameInConversationList(member1.name ?: "")
        }

        step("Then I see Member1's status icon in the conversation title") {
            pages.conversationViewPage.assertUserAvailabilityStatusIconVisible(member1.name ?: "")
        }
    }
}
