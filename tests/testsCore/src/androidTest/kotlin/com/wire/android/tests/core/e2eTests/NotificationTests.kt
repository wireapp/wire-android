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
import com.wire.android.tests.core.BaseCallUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration.Companion.seconds

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
class NotificationTests : BaseCallUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var member2: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        initCallTestHelpers()
    }

    // 1:1 notifications

    @Suppress("LongMethod")
    @TestCaseId("TC-4461", "TC-4467")
    @Category("regression", "RC", "notifications")
    @Test
    fun givenOneOnOneConversation_whenIReceiveMessageInForeground_thenNotificationAppearsAndClearsAfterReading() {
        step("Given There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has 1:1 conversation with user2Name in team Notifications") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("When User user1Name sends message Hello! to User Myself") {
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

        step("Then I open notification center and see message Hello! from user1Name") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertOneOnOneMessageNotificationVisible(
                    "Hello!",
                    teamOwner.name ?: ""
                )
            }
        }

        step("When I close notification center and open unread conversation user1Name") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapUnreadConversationNameInConversationList(teamOwner.name ?: "")
        }

        step("Then I see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        // TC-4467 - I want to verify that push notifications disappear once I have read a message in a 1:1 conversation
        step("When I open notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I do not see message Hello! from user1Name in notification center") {
            pages.notificationsPage.waitUntilMessageNotificationGone("Hello!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4463", "TC-4468")
    @Category("regression", "notifications")
    @Test
    fun givenOneOnOneConversation_whenAppIsInBackgroundAndIReceiveMessage_thenNotificationIsDisplayedAndTappingItOpensConversation() {
        step("And There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has 1:1 conversation with user2Name in team Notifications") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see conversation user1Name in conversation list") {
            pages.conversationListPage.assertConversationVisible(teamOwner.name ?: "")
        }

        step("When I minimise Wire") {
            device.pressHome()
        }

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

        step("And I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I see message Hello! from user1Name in the notification center") {
            pages.notificationsPage.assertOneOnOneMessageNotificationVisible(
                "Hello!",
                teamOwner.name ?: ""
            )
        }

        // TC-4468 - I want to be able to open a 1:1 conversation when tapping on a push notification
        step("When I tap message Hello! from user1Name in the notification center") {
            pages.notificationsPage.tapMessageNotification("Hello!")
        }

        step("Then I see conversation view with user1Name in foreground") {
            pages.conversationViewPage.assertOneOnOneConversationInForeground(teamOwner.name ?: "")
        }

        step("And I see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4465")
    @Category("regression", "notifications")
    @Test
    fun givenOneOnOneConversation_whenAppIsTerminatedAndIReceiveMessage_thenNotificationIsDisplayed() {
        step("And There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has 1:1 conversation with user2Name in team Notifications") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see conversation user1Name in conversation list") {
            pages.conversationListPage.assertConversationVisible(teamOwner.name ?: "")
        }

        step("When I swipe the app away from background") {
            pages.commonAppPage.swipeWireAppAwayFromBackground()
        }

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

        step("And I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I see message Hello! from user1Name in the notification center") {
            pages.notificationsPage.assertOneOnOneMessageNotificationVisible(
                "Hello!",
                teamOwner.name ?: ""
            )
        }

        step("When I tap message Hello! from user1Name in the notification center") {
            pages.notificationsPage.tapMessageNotification("Hello!")
        }

        step("Then I see conversation view with user1Name in foreground") {
            pages.conversationViewPage.assertOneOnOneConversationInForeground(teamOwner.name ?: "")
        }

        step("And I see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    // Runs only on Graphene OS phone
    // This test is needed for Akamaya, who is using devices without playservices
    @Suppress("LongMethod")
    @TestCaseId("TC-4466")
    @Category("regression", "RC", "websocket")
    @Test
    fun givenWebSocketOnlyDevice_whenAppIsTerminatedAndIReceiveMessage_thenNotificationIsDisplayed() {
        step("Given Test runs only on node Google-bluejay-Pixel6a-25181JEGR05249") {
            assumeTrue(
                "This test runs only on the Graphene Pixel 6a device.",
                device.executeShellCommand("getprop ro.boot.serialno").trim() == "25181JEGR05249"
            )
        }

        step("And There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has 1:1 conversation with user2Name in team Notifications") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("And I see conversation user1Name in conversation list") {
            pages.conversationListPage.assertConversationVisible(teamOwner.name ?: "")
        }

        step("And I open Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("When I tap Network Settings menu") {
            pages.settingsPage.tapNetworkSettingsButton()
        }

        step("Then I see there is no option to enable or disable my websocket") {
            pages.settingsPage.assertWebSocketToggleNotVisible()
        }

        step("When I swipe the app away from background") {
            pages.commonAppPage.swipeWireAppAwayFromBackground()
        }

        step("And I open notification center and see my Websocket connection service is running") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertWebsocketServiceNotificationVisible()
            }
        }

        step("And I close the notification center") {
            pages.notificationsPage.closeNotificationCenter()
        }

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

        step("And I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I see message Hello! from user1Name in the notification center") {
            pages.notificationsPage.assertOneOnOneMessageNotificationVisible(
                "Hello!",
                teamOwner.name ?: ""
            )
        }

        step("When I tap message Hello! from user1Name in the notification center") {
            pages.notificationsPage.tapMessageNotification("Hello!")
        }

        step("Then I see conversation view with user1Name in foreground") {
            pages.conversationViewPage.assertOneOnOneConversationInForeground(teamOwner.name ?: "")
        }

        step("And I see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    // Group notifications

    @Suppress("LongMethod")
    @TestCaseId("TC-4462", "TC-4470")
    @Category("regression", "RC", "notifications")
    @Test
    fun givenGroupConversation_whenIReceiveMessageInForeground_thenNotificationAppearsAndClearsAfterReading() {
        step("Given There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user2Name,user3Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation NotificationsGroup with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "NotificationsGroup",
                "user2Name,user3Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("When User user3Name sends message Hello! to group conversation NotificationsGroup") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello!",
                null,
                "NotificationsGroup"
            )
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("Then I open notification center and see message Hello! from user3Name in NotificationsGroup") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertGroupMessageNotificationVisible(
                    "Hello!",
                    member2.name ?: "",
                    "NotificationsGroup"
                )
            }
        }

        step("When I close notification center and open unread conversation NotificationsGroup") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapUnreadConversationNameInConversationList("NotificationsGroup")
        }

        step("Then I see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        // TC-4470 - I want to verify that push notifications disappear once I have read a message in a group conversation
        step("When I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I do not see message Hello! from user3Name in NotificationsGroup") {
            pages.notificationsPage.waitUntilMessageNotificationGone("Hello!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4464", "TC-4469")
    @Category("regression", "notifications")
    @Test
    fun givenGroupConversation_whenAppIsInBackgroundAndIReceiveMessage_thenNotificationIsDisplayedAndTappingItOpensConversation() {
        step("And There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user2Name,user3Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation NotificationsGroup with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "NotificationsGroup",
                "user2Name,user3Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see conversation NotificationsGroup in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("NotificationsGroup")
        }

        step("When I minimise Wire") {
            device.pressHome()
        }

        step("And User user3Name sends message Hello! to group conversation NotificationsGroup") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello!",
                null,
                "NotificationsGroup"
            )
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("And I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I see message Hello! from user3Name in NotificationsGroup") {
            pages.notificationsPage.assertGroupMessageNotificationVisible(
                "Hello!",
                member2.name ?: "",
                "NotificationsGroup"
            )
        }

        // TC-4469 - I want to be able to open a group conversation when tapping on a push notification
        step("When I tap message Hello! from NotificationsGroup in the notification center") {
            pages.notificationsPage.tapMessageNotification("Hello!")
        }

        step("Then I see group conversation NotificationsGroup in foreground") {
            pages.conversationViewPage.assertGroupConversationInForeground("NotificationsGroup")
        }

        step("And I see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4460")
    @Category("regression", "RC", "notifications", "connect")
    @Test
    fun givenPersonalUser_whenIReceiveConnectionRequest_thenNotificationIsDisplayed() {
        step("Given There are personal users user1Name and user2Name") {
            clientUserManager.createPersonalUsersByAliases(
                listOf("user1Name", "user2Name"),
                backendClient
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
        }

        step("And User user2Name sets their unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        step("And User user1Name is me") {
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("When User user2Name sends connection request to me") {
            backendSetupHelper.connectionRequestIsSentTo("user2Name", "user1Name")
        }

        step("And I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I see Wants to connect from user2Name in the notification center") {
            pages.notificationsPage.assertOneOnOneMessageNotificationVisible(
                "Wants to connect",
                member2.name ?: ""
            )
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4471")
    @Category("regression", "RC", "notifications", "mentions")
    @Test
    fun givenGroupConversation_whenIAmMentioned_thenMentionNotificationIsDisplayed() {
        step("Given There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user2Name,user3Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation NotificationsGroup with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "NotificationsGroup",
                "user2Name,user3Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("When User user1Name sends mention @user2Name to group conversation NotificationsGroup") {
            testServiceHelper.userSendsMentionToConversation(
                "user1Name",
                "user2Name",
                "NotificationsGroup"
            )
        }

        step("And I minimise Wire") {
            device.pressHome()
        }

        step("And I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I see mention @user2Name from user1Name in NotificationsGroup") {
            pages.notificationsPage.assertGroupMessageNotificationVisible(
                "@${member1.name ?: ""}",
                teamOwner.name ?: "",
                "NotificationsGroup"
            )
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4078")
    @Category("regression", "notification", "calling")
    @Test
    fun givenOneOnOneConversation_whenMemberCallsMeInBackground_thenIncomingCallNotificationIsDisplayed() {
        step("And There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has 1:1 conversation with user2Name in team Notifications") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Notifications"
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And user2Name starts instance using Chrome") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        loginToStagingAs(teamOwner)

        step("And I minimise Wire") {
            device.pressHome()
        }

        step("When User user2Name calls me") {
            runBlocking {
                callingManager.callConversation("user2Name", "user1Name")
            }
        }

        step("And I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I see Calling from user2Name in the notification center") {
            pages.notificationsPage.iSeeOneOnOneIncomingCallNotification(member1.name ?: "")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4079")
    @Category("regression", "RC", "notifications", "calling")
    @Test
    fun givenGroupConversation_whenIDeclineIncomingCall_thenMissedCallNotificationIsDisplayed() {
        step("Given There is a team owner user1Name with team WeLikeCalls") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "WeLikeCalls",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And I wait for 3 seconds") {
            UiWaitUtils.waitFor(3.seconds)
        }

        step("And TeamOwner user1Name enables conference calling for team WeLikeCalls") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "WeLikeCalls"
                )
            }
        }

        step("And User user1Name adds user user2Name to team WeLikeCalls with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "WeLikeCalls",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation WantToCall with user2Name in team WeLikeCalls") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "WantToCall",
                "user2Name",
                "WeLikeCalls"
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And user2Name starts instance using Chrome") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        loginToStagingAs(teamOwner)

        step("When User user2Name calls group conversation WantToCall") {
            runBlocking {
                callingManager.callGroupConversation("user2Name", "WantToCall")
            }
        }

        step("And I see incoming group call from group WantToCall") {
            pages.notificationsPage.iSeeIncomingGroupCall("WantToCall")
        }

        step("And I decline the call") {
            pages.callingPage.iTapOnHangUpButton()
        }

        step("And user2Name stops calling WantToCall") {
            runBlocking {
                callingManager.stopOutgoingCall(
                    clientUserManager.splitAliases("user2Name"),
                    "WantToCall"
                )
            }
        }

        step("And I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I see Missed call from user2Name in group WantToCall") {
            pages.notificationsPage.assertGroupMessageNotificationVisible(
                "Missed call",
                member1.name ?: "",
                "WantToCall"
            )
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4080")
    @Category("regression", "notifications", "calling")
    @Test
    fun givenGroupConversation_whenIAcceptCallAndMinimiseWire_thenOngoingCallNotificationIsDisplayed() {
        step("Given There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner user1Name enables conference calling for team Notifications") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "Notifications"
                )
            }
        }

        step("And User user1Name adds user user2Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation WantToCall with user2Name in team Notifications") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "WantToCall",
                "user2Name",
                "Notifications"
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And user2Name starts instance using Chrome") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        loginToStagingAs(teamOwner)

        step("When User user2Name calls group conversation WantToCall") {
            runBlocking {
                callingManager.callGroupConversation("user2Name", "WantToCall")
            }
        }

        step("And I see incoming group call from group WantToCall") {
            pages.notificationsPage.iSeeIncomingGroupCall("WantToCall")
        }

        step("And I accept the call") {
            pages.callingPage.iAcceptCall()
        }

        step("Then I see an ongoing group call") {
            pages.callingPage.iSeeOngoingGroupCall()
            UiWaitUtils.waitFor(1.seconds)
        }

        step("When I minimise Wire and open the notification center") {
            device.pressHome()
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I see an ongoing call in group WantToCall in the notification center") {
            pages.notificationsPage.assertOngoingGroupCallNotificationVisible("WantToCall")
        }
    }

    // Status notifications: 1:1 conversations

    @Suppress("LongMethod")
    @TestCaseId("TC-4552")
    @Category("regression", "RC", "notifications")
    @Test
    fun givenMyStatusIsSetToAvailable_whenIReceiveOneOnOneMessage_thenNotificationIsDisplayedUntilRead() {
        step("Given There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has a 1:1 conversation with user2Name in team Notifications") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I open User Profile Page and see change status options") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertChangeStatusOptionsVisible()
            }
        }

        step("When I change my status to Available") {
            pages.selfUserProfilePage.changeAvailabilityStatus("Available")
        }

        step("Then I see information about the Available status and tap OK") {
            pages.selfUserProfilePage.apply {
                assertStatusChangeTextVisible(
                    "You will appear as Available to other people. You will receive notifications for incoming calls " +
                        "and for messages according to the Notifications setting in each conversation."
                )
                confirmStatusChange()
            }
        }

        step("And I close User Profile Page and see the conversation list") {
            pages.selfUserProfilePage.closeUserProfile()
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When User user1Name sends message Hello! to User Myself") {
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

        step("Then I open the notification center and see message Hello! from user1Name") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertOneOnOneMessageNotificationVisible("Hello!", teamOwner.name ?: "")
            }
        }

        step("When I close the notification center and open unread conversation user1Name") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapUnreadConversationNameInConversationList(teamOwner.name ?: "")
        }

        step("Then I see message Hello! in the current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I do not see message Hello! from user1Name in the notification center") {
            pages.notificationsPage.waitUntilMessageNotificationGone("Hello!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4553")
    @Category("regression", "RC", "notifications")
    @Test
    fun givenMyStatusIsSetToBusy_whenIReceiveOneOnOneMessage_thenNotificationIsNotDisplayed() {
        step("Given There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has a 1:1 conversation with user2Name in team Notifications") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I open User Profile Page and see change status options") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertChangeStatusOptionsVisible()
            }
        }

        step("When I change my status to Busy") {
            pages.selfUserProfilePage.changeAvailabilityStatus("Busy")
        }

        step("Then I see information about the Busy status and tap OK") {
            pages.selfUserProfilePage.apply {
                assertStatusChangeTextVisible(
                    "You will appear as Busy to other people. You will only receive notifications for mentions, " +
                        "replies, and calls in conversations that are not muted."
                )
                confirmStatusChange()
            }
        }

        step("And I close User Profile Page and see the conversation list") {
            pages.selfUserProfilePage.closeUserProfile()
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When User user1Name sends message Hello! to User Myself") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user1Name",
                "Hello!",
                "Device1",
                "user2Name"
            )
        }

        step("Then I open the notification center and do not see message Hello! from user1Name") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertMessageNotVisibleInNotificationCenter("Hello!")
            }
        }

        step("When I close the notification center and open unread conversation user1Name") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapUnreadConversationNameInConversationList(teamOwner.name ?: "")
        }

        step("Then I see message Hello! in the current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I still do not see message Hello! from user1Name in the notification center") {
            pages.notificationsPage.waitUntilMessageNotificationGone("Hello!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4554")
    @Category("regression", "RC", "notifications")
    @Test
    fun givenMyStatusIsSetToAway_whenIReceiveOneOnOneMessage_thenNotificationIsNotDisplayed() {
        step("Given There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has a 1:1 conversation with user2Name in team Notifications") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I open User Profile Page and see change status options") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertChangeStatusOptionsVisible()
            }
        }

        step("When I change my status to Away") {
            pages.selfUserProfilePage.changeAvailabilityStatus("Away")
        }

        step("Then I see information about the Away status and tap OK") {
            pages.selfUserProfilePage.apply {
                assertStatusChangeTextVisible(
                    "You will appear as Away to other people. You will not receive notifications about any incoming " +
                        "calls or messages."
                )
                confirmStatusChange()
            }
        }

        step("And I close User Profile Page and see the conversation list") {
            pages.selfUserProfilePage.closeUserProfile()
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When User user1Name sends message Hello! to User Myself") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user1Name",
                "Hello!",
                "Device1",
                "user2Name"
            )
        }

        step("Then I open the notification center and do not see message Hello! from user1Name") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertMessageNotVisibleInNotificationCenter("Hello!")
            }
        }

        step("When I close the notification center and open unread conversation user1Name") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapUnreadConversationNameInConversationList(teamOwner.name ?: "")
        }

        step("Then I see message Hello! in the current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I still do not see message Hello! from user1Name in the notification center") {
            pages.notificationsPage.waitUntilMessageNotificationGone("Hello!")
        }
    }

    // Status notifications: group conversations

    @Suppress("LongMethod")
    @TestCaseId("TC-4555")
    @Category("regression", "RC", "notifications")
    @Test
    fun givenMyStatusIsSetToAvailable_whenIReceiveGroupMessage_thenNotificationIsDisplayedUntilRead() {
        step("Given There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user2Name and user3Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group NotificationsGroup with user2Name and user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "NotificationsGroup",
                "user2Name,user3Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I open User Profile Page and see change status options") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertChangeStatusOptionsVisible()
            }
        }

        step("When I change my status to Available") {
            pages.selfUserProfilePage.changeAvailabilityStatus("Available")
        }

        step("Then I see information about the Available status and tap OK") {
            pages.selfUserProfilePage.apply {
                assertStatusChangeTextVisible(
                    "You will appear as Available to other people. You will receive notifications for incoming calls " +
                        "and for messages according to the Notifications setting in each conversation."
                )
                confirmStatusChange()
            }
        }

        step("And I close User Profile Page and see the conversation list") {
            pages.selfUserProfilePage.closeUserProfile()
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When User user3Name sends message Hello! to group NotificationsGroup") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello!",
                null,
                "NotificationsGroup"
            )
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("Then I open the notification center and see message Hello! from user3Name in NotificationsGroup") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertGroupMessageNotificationVisible(
                    "Hello!",
                    member2.name ?: "",
                    "NotificationsGroup"
                )
            }
        }

        step("When I close the notification center and open unread conversation NotificationsGroup") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapUnreadConversationNameInConversationList("NotificationsGroup")
        }

        step("Then I see message Hello! in the current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I do not see message Hello! from user3Name in NotificationsGroup") {
            pages.notificationsPage.waitUntilMessageNotificationGone("Hello!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4556")
    @Category("regression", "RC", "notifications")
    @Test
    fun givenMyStatusIsSetToBusy_whenIReceiveGroupMessage_thenNotificationIsNotDisplayed() {
        step("Given There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user2Name and user3Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group NotificationsGroup with user2Name and user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "NotificationsGroup",
                "user2Name,user3Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I open User Profile Page and see change status options") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertChangeStatusOptionsVisible()
            }
        }

        step("When I change my status to Busy") {
            pages.selfUserProfilePage.changeAvailabilityStatus("Busy")
        }

        step("Then I see information about the Busy status and tap OK") {
            pages.selfUserProfilePage.apply {
                assertStatusChangeTextVisible(
                    "You will appear as Busy to other people. You will only receive notifications for mentions, " +
                        "replies, and calls in conversations that are not muted."
                )
                confirmStatusChange()
            }
        }

        step("And I close User Profile Page and see the conversation list") {
            pages.selfUserProfilePage.closeUserProfile()
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When User user3Name sends message Hello! to group NotificationsGroup") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello!",
                null,
                "NotificationsGroup"
            )
        }

        step("Then I open the notification center and do not see message Hello! from user3Name in NotificationsGroup") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertMessageNotVisibleInNotificationCenter("Hello!")
            }
        }

        step("When I close the notification center and open unread conversation NotificationsGroup") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapUnreadConversationNameInConversationList("NotificationsGroup")
        }

        step("Then I see message Hello! in the current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I still do not see message Hello! from user3Name in NotificationsGroup") {
            pages.notificationsPage.waitUntilMessageNotificationGone("Hello!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4557")
    @Category("regression", "RC", "notifications")
    @Test
    fun givenMyStatusIsSetToAway_whenIReceiveGroupMessage_thenNotificationIsNotDisplayed() {
        step("Given There is a team owner user1Name with team Notifications") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notifications",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user2Name and user3Name to team Notifications with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Notifications",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group NotificationsGroup with user2Name and user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "NotificationsGroup",
                "user2Name,user3Name",
                "Notifications"
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I open User Profile Page and see change status options") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertChangeStatusOptionsVisible()
            }
        }

        step("When I change my status to Away") {
            pages.selfUserProfilePage.changeAvailabilityStatus("Away")
        }

        step("Then I see information about the Away status and tap OK") {
            pages.selfUserProfilePage.apply {
                assertStatusChangeTextVisible(
                    "You will appear as Away to other people. You will not receive notifications about any incoming " +
                        "calls or messages."
                )
                confirmStatusChange()
            }
        }

        step("And I close User Profile Page and see the conversation list") {
            pages.selfUserProfilePage.closeUserProfile()
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When User user3Name sends message Hello! to group NotificationsGroup") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello!",
                null,
                "NotificationsGroup"
            )
        }

        step("Then I open the notification center and do not see message Hello! from user3Name in NotificationsGroup") {
            pages.notificationsPage.apply {
                openNotificationCenter()
                assertMessageNotVisibleInNotificationCenter("Hello!")
            }
        }

        step("When I close the notification center and open unread conversation NotificationsGroup") {
            pages.notificationsPage.closeNotificationCenter()
            pages.conversationListPage.tapUnreadConversationNameInConversationList("NotificationsGroup")
        }

        step("Then I see message Hello! in the current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I open the notification center") {
            pages.notificationsPage.openNotificationCenter()
        }

        step("Then I still do not see message Hello! from user3Name in NotificationsGroup") {
            pages.notificationsPage.waitUntilMessageNotificationGone("Hello!")
        }
    }

    // Keeps the repeated staging login flow consistent across notification scenarios.
    private fun loginToStagingAs(user: ClientUser) {
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
                enterTeamMemberLoggingEmail(user.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(user.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }
    }
}
