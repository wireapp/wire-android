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
class ConversationNotificationsTests : BaseUiTest() {
    private lateinit var teamMember: ClientUser
    private lateinit var messageSender: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4312", "TC-4313", "TC-4307")
    @Category("regression", "RC", "groups", "conversationNotifications")
    @Test
    fun givenGroupConversation_whenNotificationsAreSetToCallsMentionsAndReplies_thenPushNotificationIsNotReceived() {
        givenTeamMemberIsLoggedInWithGroupConversation()

        // TC-4307 - I want to receive push notifications for messages when conversation status is set to Everything
        step("And User user3Name sends message Hello! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello!",
                "Device1",
                "MyTeam"
            )
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("Then I open notification center and see message Hello! from user3Name in MyTeam") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertGroupMessageNotificationVisible(
                    "Hello!",
                    messageSender.name ?: "",
                    "MyTeam"
                )
            }
        }

        step("And I close notification center") {
            pages.notificationsPage.closeNotificationCenter()
        }

        step("And I open unread conversation MyTeam and see message Hello!") {
            pages.conversationListPage.tapUnreadConversationNameInConversationList("MyTeam")
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I open MyTeam details and notification settings") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.apply {
                tapShowMoreOptionsButton()
                tapNotificationsButton()
            }
        }

        // TC-4313 - I want to see conversation notification for group conversation is set to Everything by default
        step("Then I see default notification is Everything") {
            pages.groupConversationDetailsPage.assertNotificationStatusVisible("Everything")
        }

        step("When I set notification status to Calls, mentions and replies") {
            pages.groupConversationDetailsPage.apply {
                tapNotificationStatus("Calls, mentions and replies")
                assertNotificationStatusVisible("Calls, mentions and replies")
            }
        }

        step("And I tap back button 3 times") {
            repeat(3) {
                device.pressBack()
            }
        }

        step("And User user3Name sends message No notification to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "No notification",
                "Device1",
                "MyTeam"
            )
        }

        step("Then I open notification center and do not see message No notification") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertMessageNotVisibleInNotificationCenter("No notification")
            }
        }

        step("When I close notification center and open conversation MyTeam") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapConversationNameInConversationList("MyTeam")
        }

        step("Then I see message No notification in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("No notification")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4306")
    @Category("regression", "RC", "groups", "conversationNotifications")
    @Test
    fun givenGroupConversation_whenNotificationsAreSetToNothing_thenPushNotificationIsNotReceived() {
        givenTeamMemberIsLoggedInWithGroupConversation()

        step("And User user3Name sends message Hello! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello!",
                "Device1",
                "MyTeam"
            )
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("And I open notification center and see message Hello! from user3Name in MyTeam") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertGroupMessageNotificationVisible(
                    "Hello!",
                    messageSender.name ?: "",
                    "MyTeam"
                )
            }
        }

        step("And I close notification center") {
            pages.notificationsPage.closeNotificationCenter()
        }

        step("And I open unread conversation MyTeam and see message Hello!") {
            pages.conversationListPage.tapUnreadConversationNameInConversationList("MyTeam")
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I open MyTeam details and set notification status to Nothing") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.apply {
                tapShowMoreOptionsButton()
                tapNotificationsButton()
                tapNotificationStatus("Nothing")
                assertNotificationStatusVisible("Nothing")
            }
        }

        step("And I tap back button 3 times") {
            repeat(3) {
                device.pressBack()
            }
        }

        step("And User user3Name sends message No notification to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "No notification",
                "Device1",
                "MyTeam"
            )
        }

        step("Then I open notification center and do not see message No notification") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertMessageNotVisibleInNotificationCenter("No notification")
            }
        }

        step("When I close notification center and open conversation MyTeam") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapConversationNameInConversationList("MyTeam")
        }

        step("Then I see message No notification in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("No notification")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4308", "TC-4309", "TC-4310")
    @Category("regression", "RC", "conversationNotifications")
    @Test
    fun givenOneOnOneConversation_whenNotificationsAreSetToCallsMentionsAndReplies_thenPushNotificationIsNotReceived() {
        givenTeamMemberIsLoggedInWithOneOnOneConversation()

        // TC-4310 - I want to receive push notifications for messages in a 1:1 conversation when status is Everything
        step("And User user1Name sends message Hello! to User Myself") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user1Name",
                "Hello!",
                "Device1",
                "user2Name"
            )
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("And I open notification center and see message Hello! from user1Name") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertOneOnOneMessageNotificationVisible(
                    "Hello!",
                    messageSender.name ?: ""
                )
            }
        }

        step("And I close notification center") {
            pages.notificationsPage.closeNotificationCenter()
        }

        step("And I open unread conversation user1Name and see message Hello!") {
            pages.conversationListPage.tapUnreadConversationNameInConversationList(messageSender.name ?: "")
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("And I open user1Name profile and notification settings") {
            pages.conversationViewPage.click1On1ConversationDetails(messageSender.name ?: "")
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
                tapNotificationsButton()
            }
        }

        // TC-4309 - I want to see conversation notification for 1:1 conversation is set to Everything in default
        step("Then I see default notification is Everything") {
            pages.connectedUserProfilePage.assertNotificationStatusVisible("Everything")
        }

        step("When I set notification status to Calls, mentions and replies") {
            pages.connectedUserProfilePage.apply {
                tapNotificationStatus("Calls, mentions and replies")
                assertNotificationStatusVisible("Calls, mentions and replies")
            }
        }

        step("And I tap back button 3 times") {
            repeat(3) {
                device.pressBack()
            }
        }

        step("And User user1Name sends message No notification to User Myself") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user1Name",
                "No notification",
                "Device1",
                "user2Name"
            )
        }

        step("Then I open notification center and do not see message No notification") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertMessageNotVisibleInNotificationCenter("No notification")
            }
        }

        step("When I close notification center and open conversation user1Name") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapConversationNameInConversationList(messageSender.name ?: "")
        }

        step("Then I see message No notification in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("No notification")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4311")
    @Category("regression", "RC", "conversationNotifications")
    @Test
    fun givenOneOnOneConversation_whenNotificationsAreSetToNothing_thenPushNotificationIsNotReceived() {
        givenTeamMemberIsLoggedInWithOneOnOneConversation()

        step("And User user1Name sends message Hello! to User Myself") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user1Name",
                "Hello!",
                "Device1",
                "user2Name"
            )
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("And I open notification center and see message Hello! from user1Name") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertOneOnOneMessageNotificationVisible(
                    "Hello!",
                    messageSender.name ?: ""
                )
            }
        }

        step("And I close notification center") {
            pages.notificationsPage.closeNotificationCenter()
        }

        step("And I open unread conversation user1Name and see message Hello!") {
            pages.conversationListPage.tapUnreadConversationNameInConversationList(messageSender.name ?: "")
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("And I open user1Name profile and notification settings") {
            pages.conversationViewPage.click1On1ConversationDetails(messageSender.name ?: "")
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
                tapNotificationsButton()
            }
        }

        // TC-4309 - I want to see conversation notification for 1:1 conversation is set to Everything in default
        step("Then I see default notification is Everything") {
            pages.connectedUserProfilePage.assertNotificationStatusVisible("Everything")
        }

        step("When I set notification status to Nothing") {
            pages.connectedUserProfilePage.apply {
                tapNotificationStatus("Nothing")
                assertNotificationStatusVisible("Nothing")
            }
        }

        step("And I tap back button 3 times") {
            repeat(3) {
                device.pressBack()
            }
        }

        step("And User user1Name sends message No notification to User Myself") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user1Name",
                "No notification",
                "Device1",
                "user2Name"
            )
        }

        step("Then I open notification center and do not see message No notification") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertMessageNotVisibleInNotificationCenter("No notification")
            }
        }

        step("When I close notification center and open conversation user1Name") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapConversationNameInConversationList(messageSender.name ?: "")
        }

        step("Then I see message No notification in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("No notification")
        }
    }

    // Shared 1:1 notification setup: creates the team conversation and logs in as user2Name.
    private fun givenTeamMemberIsLoggedInWithOneOnOneConversation() {
        step("Given There is a team owner user1Name with team Notification") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notification",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Notification with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Notification",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has 1:1 conversation with user2Name in team Notification") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Notification"
            )
        }

        step("And User user2Name is me") {
            messageSender = clientUserManager.findUserByNameOrNameAlias("user1Name")
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamMember)
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
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(teamMember.password ?: "")
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

        step("And I see conversation user1Name in conversation list") {
            pages.conversationListPage.assertConversationVisible(messageSender.name ?: "")
        }
    }

    // Shared group notification setup: creates the team and group, prepares the sender device, and logs in as user2Name.
    private fun givenTeamMemberIsLoggedInWithGroupConversation() {
        step("Given There is a team owner user1Name with team Notification") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notification",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user2Name,user3Name to team Notification with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Notification",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "Notification"
            )
        }

        step("And User user2Name is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name")
            messageSender = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(teamMember)
        }

        step("And User user3Name has Device1 available for backend messaging") {
            testServiceHelper.addDevice("user3Name", null, "Device1")
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
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(teamMember.password ?: "")
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

        step("And I see conversation MyTeam in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("MyTeam")
        }
    }
}
