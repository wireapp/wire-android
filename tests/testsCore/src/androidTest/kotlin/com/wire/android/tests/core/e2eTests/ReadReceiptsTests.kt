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
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class ReadReceiptsTests : BaseUiTest() {
    private var teamOwner: ClientUser? = null
    private var member1: ClientUser? = null
    private var member2: ClientUser? = null
    private var member3: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4488")
    @Category("regression", "RC", "readReceipts")
    @Test
    fun givenOneOnOneConversation_whenMemberReadsMyMessage_thenISeeTheirReadReceiptInMessageDetails() {
        step("Given There is a team owner TeamOwner with team Reactions") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reactions",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Reactions with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Reactions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Reactions") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Reactions"
            )
        }

        step("And User Member1 adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        loginToStagingAs(teamOwner)

        step("And I see and open conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1?.name ?: "")
                tapConversationNameInConversationList(member1?.name ?: "")
            }
        }

        step("And I type message Hello! and tap send") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
            }
        }

        step("And I see message Hello! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When User Member1 sends a read receipt on the last message in conversation TeamOwner via Device1") {
            testServiceHelper.userSendsReadReceiptOnLatestMessageInPersonalConversation(
                userAlias = "user2Name",
                conversationWithAlias = "user1Name",
                deviceName = "Device1"
            )
        }

        step("And I long tap message Hello! and tap message details option") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                tapMessageDetailsOption()
            }
        }

        step("And I tap read receipts tab in message details") {
            pages.messageDetailsPage.tapReadReceiptsTab()
        }

        step("Then I see 1 read receipt in read receipts tab") {
            pages.messageDetailsPage.assertReadReceiptsCount(1)
        }

        step("And I see Member1 in the list of users that read my message") {
            pages.messageDetailsPage.assertUserReadMessage(member1?.name ?: "")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4489")
    @Category("regression", "RC", "readReceipts")
    @Test
    fun givenOneOnOneConversation_whenIReadAMessage_thenSenderSeesMyReadReceipt() {
        step("Given There is a team owner TeamOwner with team ReadReceipts") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ReadReceipts",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team ReadReceipts with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ReadReceipts",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team ReadReceipts") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "ReadReceipts"
            )
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        loginToStagingAs(member1)

        step("And I open User Profile and see Member1 is my currently active account") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(member1?.name ?: "")
            }
        }

        step("And I tap New Team or Account button") {
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        loginToStagingAs(teamOwner)

        step("And I see conversation list and open conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationVisible(member1?.name ?: "")
                tapConversationNameInConversationList(member1?.name ?: "")
            }
        }

        step("And I type message Read me! and tap send") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Read me!")
                clickSendButton()
            }
        }

        step("And I see message Read me! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Read me!")
        }

        step("And I close the conversation view through the back arrow") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I tap User Profile Button") {
            pages.conversationListPage.clickUserProfileButton()
        }

        step("And I switch to Member1 account") {
            pages.selfUserProfilePage.tapOtherAccountByName(member1?.name ?: "")
            clientUserManager.setSelfUser(clientUserManager.findUserByNameOrNameAlias("user2Name"))
        }

        step("And I see conversation list and open unread conversation TeamOwner") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationHasUnreadMessagesCount(teamOwner?.name ?: "", "1")
                tapUnreadConversationNameInConversationList(teamOwner?.name ?: "")
            }
        }

        step("When I see message Read me! in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Read me!")
            }
        }

        step("And I keep the conversation open for read receipt processing") {
            UiWaitUtils.waitFor(3.seconds)
        }

        step("And I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I tap User Profile Button") {
            pages.conversationListPage.clickUserProfileButton()
        }

        step("And I switch to TeamOwner account") {
            pages.selfUserProfilePage.tapOtherAccountByName(teamOwner?.name ?: "")
            clientUserManager.setSelfUser(clientUserManager.findUserByNameOrNameAlias("user1Name"))
        }

        step("And I see and open conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1?.name ?: "")
                tapConversationNameInConversationList(member1?.name ?: "")
            }
        }

        step("And I long tap message Read me! and tap message details option") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Read me!")
                tapMessageDetailsOption()
            }
        }

        step("And I tap read receipts tab in message details") {
            pages.messageDetailsPage.tapReadReceiptsTab()
        }

        step("Then I see Member1 in the list of users that read my message") {
            pages.messageDetailsPage.assertUserReadMessage(member1?.name ?: "")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4492")
    @Category("regression", "RC", "readReceipts")
    @Test
    fun givenReadReceiptsAreTurnedOff_whenIReadAMessage_thenSenderDoesNotSeeMyReadReceipt() {
        step("Given There is a team owner TeamOwner with team ReadReceipts") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ReadReceipts",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team ReadReceipts with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ReadReceipts",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team ReadReceipts") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "ReadReceipts"
            )
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        loginToStagingAs(member1)

        step("When I open Privacy Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.clickPrivacySettingsButtonOnSettingsPage()
        }

        step("And I see read receipts are turned on") {
            pages.settingsPage.assertReadReceiptsToggleIsOn()
        }

        step("And I tap read receipts toggle") {
            pages.settingsPage.tapReadReceiptsToggle()
        }

        step("Then I see read receipts are turned off") {
            pages.settingsPage.assertReadReceiptsToggleIsOff()
        }

        step("And I tap back button 2 times") {
            pages.settingsPage.apply {
                clickBackButtonOnPrivacySettingsPage()
                clickBackButtonOnSettingsPage()
            }
        }

        step("And I open User Profile and see Member1 is my currently active account") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(member1?.name ?: "")
            }
        }

        step("And I tap New Team or Account button") {
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        loginToStagingAs(teamOwner)

        step("And I see conversation list and open conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationVisible(member1?.name ?: "")
                tapConversationNameInConversationList(member1?.name ?: "")
            }
        }

        step("And I type message Read me! and tap send") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Read me!")
                clickSendButton()
            }
        }

        step("And I see message Read me! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Read me!")
        }

        step("And I close the conversation view through the back arrow") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I tap User Profile Button") {
            pages.conversationListPage.clickUserProfileButton()
        }

        step("And I switch to Member1 account") {
            pages.selfUserProfilePage.tapOtherAccountByName(member1?.name ?: "")
            clientUserManager.setSelfUser(clientUserManager.findUserByNameOrNameAlias("user2Name"))
        }

        step("And I see conversation list and open unread conversation TeamOwner") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationHasUnreadMessagesCount(teamOwner?.name ?: "", "1")
                tapUnreadConversationNameInConversationList(teamOwner?.name ?: "")
            }
        }

        step("When I see message Read me! in current conversation") {
            pages.conversationViewPage.apply {
                assertOneOnOneConversationInForeground(teamOwner?.name ?: "")
                assertReceivedMessageIsVisibleInCurrentConversation("Read me!")
            }
        }

        step("And I keep the conversation open for read receipt processing") {
            // Allow receipt processing while the message is open before checking that none was sent.
            UiWaitUtils.waitFor(4.seconds)
        }

        step("And I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I tap User Profile Button") {
            pages.conversationListPage.clickUserProfileButton()
        }

        step("And I see TeamOwner listed under other logged in accounts") {
            pages.selfUserProfilePage.assertOtherLoggedInAccountVisible(teamOwner?.name ?: "")
        }

        step("And I switch to TeamOwner account") {
            pages.selfUserProfilePage.tapOtherAccountByName(teamOwner?.name ?: "")
            clientUserManager.setSelfUser(clientUserManager.findUserByNameOrNameAlias("user1Name"))
        }

        step("And I see and open conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1?.name ?: "")
                tapConversationNameInConversationList(member1?.name ?: "")
            }
        }

        step("And I long tap message Read me! and tap message details option") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Read me!")
                tapMessageDetailsOption()
            }
        }

        step("And I tap read receipts tab in message details") {
            pages.messageDetailsPage.tapReadReceiptsTab()
        }

        step("Then I do not see Member1 in the list of users that read my message") {
            pages.messageDetailsPage.assertUserDidNotReadMessage(member1?.name ?: "")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4490")
    @Category("regression", "RC", "readReceipts")
    @Test
    fun givenGroupConversation_whenMembersReadMyMessage_thenISeeTheirReadReceiptsInMessageDetails() {
        step("Given There is a team owner TeamOwner with team ReadReceipts") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ReadReceipts",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1, Member2 and Member3 to team ReadReceipts with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "ReadReceipts",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation Reading is fun with Member1, Member2 and Member3") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "Reading is fun",
                "user2Name,user3Name,user4Name",
                "ReadReceipts"
            )
        }

        step("And Member1, Member2 and Member3 register Device1, Device2 and Device3 respectively") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
            testServiceHelper.addDevice("user3Name", null, "Device2")
            testServiceHelper.addDevice("user4Name", null, "Device3")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        loginToStagingAs(teamOwner)

        step("And I see and open group conversation Reading is fun") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Reading is fun")
                tapConversationNameInConversationList("Reading is fun")
            }
        }

        step("And I type message Read me! and tap send") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Read me!")
                clickSendButton()
            }
        }

        step("And I see message Read me! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Read me!")
        }

        step("When User Member1 sends a read receipt on the last message in Reading is fun via Device1") {
            testServiceHelper.userSendsReadReceiptOnLatestMessageInGroupConversation(
                userAlias = "user2Name",
                conversationName = "Reading is fun",
                deviceName = "Device1"
            )
        }

        step("When User Member2 sends a read receipt on the last message in Reading is fun via Device2") {
            testServiceHelper.userSendsReadReceiptOnLatestMessageInGroupConversation(
                userAlias = "user3Name",
                conversationName = "Reading is fun",
                deviceName = "Device2"
            )
        }

        step("When User Member3 sends a read receipt on the last message in Reading is fun via Device3") {
            testServiceHelper.userSendsReadReceiptOnLatestMessageInGroupConversation(
                userAlias = "user4Name",
                conversationName = "Reading is fun",
                deviceName = "Device3"
            )
        }

        step("And I long tap message Read me! and tap message details option") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Read me!")
                tapMessageDetailsOption()
            }
        }

        step("And I tap read receipts tab in message details") {
            pages.messageDetailsPage.tapReadReceiptsTab()
        }

        step("Then I see 3 read receipts from Member1, Member2 and Member3") {
            pages.messageDetailsPage.apply {
                assertReadReceiptsCount(3)
                assertUserReadMessage(member1?.name ?: "")
                assertUserReadMessage(member2?.name ?: "")
                assertUserReadMessage(member3?.name ?: "")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4491")
    @Category("regression", "RC", "readReceipts")
    @Test
    fun givenGroupConversation_whenIReadAMessage_thenSenderSeesMyReadReceiptAlongsideOtherMembers() {
        step("Given There is a team owner TeamOwner with team ReadReceipts") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ReadReceipts",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1, Member2 and Member3 to team ReadReceipts with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "ReadReceipts",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation Reading is fun with Member1, Member2 and Member3") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "Reading is fun",
                "user2Name,user3Name,user4Name",
                "ReadReceipts"
            )
        }

        step("And User TeamOwner enables read receipts for conversation Reading is fun") {
            backendSetupHelper.userSetsReadReceiptsForConversation(
                userAlias = "user1Name",
                conversationName = "Reading is fun",
                enabled = true
            )
        }

        step("And Member2 and Member3 register Device2 and Device3 respectively") {
            testServiceHelper.addDevice("user3Name", null, "Device2")
            testServiceHelper.addDevice("user4Name", null, "Device3")
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        loginToStagingAs(member1)

        step("And I open User Profile and see Member1 is my currently active account") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(member1?.name ?: "")
            }
        }

        step("And I tap New Team or Account button") {
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        loginToStagingAs(teamOwner)

        step("And I see conversation list and open group conversation Reading is fun") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("Reading is fun")
                tapConversationNameInConversationList("Reading is fun")
            }
        }

        step("And I type message Read me! and tap send") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Read me!")
                clickSendButton()
            }
        }

        step("And I see message Read me! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Read me!")
        }

        step("When User Member2 sends a read receipt on the last message in Reading is fun via Device2") {
            testServiceHelper.userSendsReadReceiptOnLatestMessageInGroupConversation(
                userAlias = "user3Name",
                conversationName = "Reading is fun",
                deviceName = "Device2"
            )
        }

        step("When User Member3 sends a read receipt on the last message in Reading is fun via Device3") {
            testServiceHelper.userSendsReadReceiptOnLatestMessageInGroupConversation(
                userAlias = "user4Name",
                conversationName = "Reading is fun",
                deviceName = "Device3"
            )
        }

        step("And I long tap message Read me! and tap message details option") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Read me!")
                tapMessageDetailsOption()
            }
        }

        step("And I tap read receipts tab in message details") {
            pages.messageDetailsPage.tapReadReceiptsTab()
        }

        step("Then I see 2 read receipts from Member2 and Member3") {
            pages.messageDetailsPage.apply {
                assertReadReceiptsCount(2)
                assertUserReadMessage(member2?.name ?: "")
                assertUserReadMessage(member3?.name ?: "")
            }
        }

        step("And I tap back to return to the conversation") {
            device.pressBack()
            pages.conversationViewPage.assertConversationScreenVisible()
        }

        step("And I close the conversation view through the back arrow") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I tap User Profile Button") {
            pages.conversationListPage.clickUserProfileButton()
        }

        step("And I switch to Member1 account") {
            pages.selfUserProfilePage.tapOtherAccountByName(member1?.name ?: "")
            clientUserManager.setSelfUser(clientUserManager.findUserByNameOrNameAlias("user2Name"))
        }

        step("And I see conversation list and open unread group conversation Reading is fun") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationHasUnreadMessagesCount("Reading is fun", "1")
                tapUnreadConversationNameInConversationList("Reading is fun")
            }
        }

        step("When I see message Read me! in current conversation") {
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                assertGroupConversationInForeground("Reading is fun")
                assertReceivedMessageIsVisibleInCurrentConversation("Read me!")
            }
        }

        step("And I keep the conversation open for read receipt processing") {
            UiWaitUtils.waitFor(5.seconds)
        }

        step("And I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I tap User Profile Button") {
            pages.conversationListPage.clickUserProfileButton()
        }

        step("And I switch to TeamOwner account") {
            pages.selfUserProfilePage.tapOtherAccountByName(teamOwner?.name ?: "")
            clientUserManager.setSelfUser(clientUserManager.findUserByNameOrNameAlias("user1Name"))
        }

        step("And I see and open group conversation Reading is fun") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Reading is fun")
                tapConversationNameInConversationList("Reading is fun")
            }
        }

        step("And I long tap message Read me! and tap message details option") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Read me!")
                tapMessageDetailsOption()
            }
        }

        step("And I tap read receipts tab in message details") {
            pages.messageDetailsPage.tapReadReceiptsTab()
        }

        step("Then I see 3 read receipts from Member1, Member2 and Member3") {
            pages.messageDetailsPage.apply {
                assertReadReceiptsCount(3)
                assertUserReadMessage(member1?.name ?: "")
                assertUserReadMessage(member2?.name ?: "")
                assertUserReadMessage(member3?.name ?: "")
            }
        }
    }

    // Keeps the repeated staging login flow consistent across read receipt scenarios.
    private fun loginToStagingAs(user: ClientUser?) {
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
                enterTeamOwnerLoggingEmail(user?.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(user?.password ?: "")
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
