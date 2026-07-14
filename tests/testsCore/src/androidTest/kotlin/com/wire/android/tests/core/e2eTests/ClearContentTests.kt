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
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class ClearContentTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4271", "TC-4272")
    @Category("regression", "RC", "groups", "clearContent", "smoke", "smokeSchwarz")
    @Test
    fun givenGroupConversation_whenIClearContentViaGroupDetailsAndConversationList_thenContentIsCleared() {
        step("Given There is a team owner user1Name with team Clearing") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Clearing",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user2Name,user3Name to team Clearing with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Clearing",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name has group conversation ClearContent with user2Name,user3Name in team Clearing") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ClearContent",
                "user2Name,user3Name",
                "Clearing"
            )
        }

        step("And User user2Name and user3Name have devices for sending messages") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
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

        step("And I see conversation ClearContent in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("ClearContent")
        }

        step("And I tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.clickGroupConversation("ClearContent")
        }

        step("And User user2Name sends message Hello! to group conversation ClearContent") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "ClearContent"
            )
        }

        step("And I see the message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("And User user3Name sends message Good Morning to group conversation ClearContent") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Good Morning",
                "Device2",
                "ClearContent"
            )
        }

        step("And I see the message Good Morning in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Good Morning")
        }

        step("And I tap on group conversation title ClearContent to open group details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("ClearContent")
        }

        step("And I tap show more options button") {
            pages.groupConversationDetailsPage.tapShowMoreOptionsButton()
        }

        step("When I tap clear content button and confirm on group details page") {
            pages.groupConversationDetailsPage.apply {
                tapClearContentButton()
                tapClearContentConfirmButton()
            }
        }

        step("Then I see Conversation content was deleted toast message on group details page") {
            pages.groupConversationDetailsPage.assertToastMessageIsDisplayed("Conversation content was deleted")
        }

        step("When I close the group conversation details through X icon") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        step("And I see conversation view with ClearContent is in foreground") {
            pages.conversationViewPage.assertChannelConversationInForeground("ClearContent")
        }

        step("Then I do not see the messages in current conversation") {
            pages.conversationViewPage.apply {
                assertMessageNotVisible("Hello!")
                assertMessageNotVisible("Good Morning")
            }
        }

        // TC-4272 I want to verify that I can clear the content of a group conversation from conversation list

        step("And User user2Name sends message Hello! to group conversation ClearContent") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "ClearContent"
            )
        }

        step("And User user3Name sends message Good Morning to group conversation ClearContent") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Good Morning",
                "Device2",
                "ClearContent"
            )
        }

        step("And I see the messages in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
                assertReceivedMessageIsVisibleInCurrentConversation("Good Morning")
            }
        }

        step("And I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("When I long tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.longPressConversation("ClearContent")
        }

        step("And I tap clear content button and confirm on conversation list") {
            pages.conversationListPage.apply {
                tapClearContentButtonOnConversationList()
                tapClearContentConfirmButtonOnConversationList()
            }
        }

        step("Then I see Conversation content was deleted toast message on conversation list") {
            pages.conversationListPage.assertToastMessageIsDisplayedOnConversationList(
                "Conversation content was deleted"
            )
        }

        step("When I tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.clickGroupConversation("ClearContent")
        }

        step("Then I do not see the messages in current conversation") {
            pages.conversationViewPage.apply {
                assertMessageNotVisible("Hello!")
                assertMessageNotVisible("Good Morning")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4273")
    @Category("regression", "RC", "groups", "clearContent")
    @Test
    fun givenILeftGroupConversation_whenClearingContentFromConversationList_thenConversationContentIsCleared() {
        step("Given There is a team owner user1Name with team Clearing") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Clearing",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user2Name,user3Name to team Clearing with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Clearing",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name has group conversation ClearContent with user2Name,user3Name in team Clearing") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ClearContent",
                "user2Name,user3Name",
                "Clearing"
            )
        }

        step("And User user2Name and user3Name have devices for sending messages") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
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

        step("And I see conversation ClearContent in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("ClearContent")
        }

        step("And I tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.clickGroupConversation("ClearContent")
        }

        step("And User user2Name sends message Hello! to group conversation ClearContent") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "ClearContent"
            )
        }

        step("And User user3Name sends message Good Morning to group conversation ClearContent") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Good Morning",
                "Device2",
                "ClearContent"
            )
        }

        step("And I see the messages in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
                assertReceivedMessageIsVisibleInCurrentConversation("Good Morning")
            }
        }

        step("And I tap on group conversation title ClearContent and open more options") {
            pages.conversationViewPage.clickOnGroupConversationDetails("ClearContent")
            pages.groupConversationDetailsPage.tapShowMoreOptionsButton()
        }

        step("When I tap leave conversation button and confirm") {
            pages.groupConversationDetailsPage.apply {
                tapLeaveConversationButton()
                tapLeaveConversationConfirmButton()
            }
        }

        step("Then I see you left conversation toast message") {
            waitUntilToastIsDisplayed("You left the conversation.")
        }

        step("And I see conversation ClearContent in conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("ClearContent")
            }
        }

        step("And I tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.clickGroupConversation("ClearContent")
        }

        step("And I see the messages and You left the conversation system message") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
                assertReceivedMessageIsVisibleInCurrentConversation("Good Morning")
                assertSystemMessageVisible("You left the conversation")
            }
        }

        step("And I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("When I long tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.longPressConversation("ClearContent")
        }

        step("And I tap clear content button and confirm on conversation list") {
            pages.conversationListPage.apply {
                tapClearContentButtonOnConversationList()
                tapClearContentConfirmButtonOnConversationList()
            }
        }

        step("Then I see Conversation content was deleted toast message on conversation list") {
            pages.conversationListPage.assertToastMessageIsDisplayedOnConversationList(
                "Conversation content was deleted"
            )
        }

        step("When I tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.clickGroupConversation("ClearContent")
        }

        step("Then I do not see the messages but I see You left the conversation system message") {
            pages.conversationViewPage.apply {
                assertMessageNotVisible("Hello!")
                assertMessageNotVisible("Good Morning")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4274")
    @Category("regression", "RC", "groups", "clearContent")
    @Test
    fun givenIWasRemovedFromGroupConversation_whenClearingContentFromConversationList_thenContentIsCleared() {
        step("Given There is a team owner user1Name with team Clearing") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Clearing",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user2Name,user3Name to team Clearing with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Clearing",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User user1Name has group conversation ClearContent with user2Name,user3Name in team Clearing") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ClearContent",
                "user2Name,user3Name",
                "Clearing"
            )
        }

        step("And User user1Name and user3Name have devices for sending messages") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(member1.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(member1.password ?: "")
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

        step("And I see conversation ClearContent in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("ClearContent")
        }

        step("And I tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.clickGroupConversation("ClearContent")
        }

        step("And User user1Name sends message Hello! to group conversation ClearContent") {
            testServiceHelper.userSendMessageToConversation(
                "user1Name",
                "Hello!",
                "Device1",
                "ClearContent"
            )
        }

        step("And I see the message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("And User user3Name sends message Good Morning to group conversation ClearContent") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Good Morning",
                "Device2",
                "ClearContent"
            )
        }

        step("And I see the message Good Morning in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Good Morning")
        }

        step("When User user1Name removes user user2Name from group conversation ClearContent") {
            backendSetupHelper.userRemovesUserFromGroupConversation(
                "user1Name",
                "user2Name",
                "ClearContent"
            )
        }

        step("Then I see system message user1Name removed you from the conversation in conversation view") {
            val teamOwnerName = clientUserManager.findUserByNameOrNameAlias("user1Name").name ?: ""
            pages.conversationViewPage.assertSystemMessageVisible("$teamOwnerName removed you from the conversation")
        }

        step("And I see the messages in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
                assertReceivedMessageIsVisibleInCurrentConversation("Good Morning")
            }
        }

        step("When I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I long tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.longPressConversation("ClearContent")
        }

        step("And I tap clear content button and confirm on conversation list") {
            pages.conversationListPage.apply {
                tapClearContentButtonOnConversationList()
                tapClearContentConfirmButtonOnConversationList()
            }
        }

        step("Then I see Conversation content was deleted toast message on conversation list") {
            pages.conversationListPage.assertToastMessageIsDisplayedOnConversationList(
                "Conversation content was deleted"
            )
        }

        step("When I tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.clickGroupConversation("ClearContent")
        }

        step("Then I do not see the messages in current conversation") {
            pages.conversationViewPage.apply {
                assertMessageNotVisible("Hello!")
                assertMessageNotVisible("Good Morning")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4275")
    @Category("regression", "RC", "groups", "clearContent")
    @Test
    fun givenGroupConversationWithAssets_whenIClearContentFromGroupDetails_thenMessagesAndAssetsAreRemoved() {
        step("Given There is a team owner user1Name with team Clearing") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Clearing",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds users user2Name,user3Name to team Clearing with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Clearing",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name has group conversation ClearContent with user2Name,user3Name in team Clearing") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ClearContent",
                "user2Name,user3Name",
                "Clearing"
            )
        }

        step("And User user2Name and user3Name have devices for sending messages and assets") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
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

        step("And I see conversation ClearContent in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("ClearContent")
        }

        step("And I tap on conversation name ClearContent in conversation list") {
            pages.conversationListPage.clickGroupConversation("ClearContent")
        }

        step("And User user2Name sends message Hello! to group conversation ClearContent") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "ClearContent"
            )
        }

        step("And I see the message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("And User user3Name sends message Good Morning to group conversation ClearContent") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Good Morning",
                "Device2",
                "ClearContent"
            )
        }

        step("And I see the message Good Morning in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Good Morning")
        }

        step("And User user3Name sends image testing.jpg to conversation ClearContent") {
            testServiceHelper.contactSendsLocalImageConversation(
                context,
                "testing.jpg",
                "user3Name",
                "Device2",
                "ClearContent"
            )
        }

        step("And I see an image in the conversation view") {
            pages.conversationViewPage.assertImageIsVisible()
        }

        step("And User user2Name sends local audio file named test.m4a via device Device1 to group conversation ClearContent") {
            testServiceHelper.contactSendsLocalAudioConversation(
                context,
                "test.m4a",
                "user2Name",
                "Device1",
                "ClearContent"
            )
        }

        step("And I see an audio file in the conversation view") {
            pages.conversationViewPage.assertAudioMessageIsVisible()
        }

        step("And User user2Name sends local video named testing.mp4 via device Device1 to group conversation ClearContent") {
            testServiceHelper.contactSendsLocalVideoConversation(
                context,
                "testing.mp4",
                "user2Name",
                "Device1",
                "ClearContent"
            )
        }

        step("And I scroll to the bottom of conversation view and see file testing.mp4") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertFileWithNameIsVisible("testing.mp4")
            }
        }

        step("And User user2Name sends 1.00MB text file qa_random.txt via device Device1 to group conversation ClearContent") {
            testServiceHelper.contactSendsOneMbTextFileConversation(
                context,
                "qa_random.txt",
                "user2Name",
                "Device1",
                "ClearContent"
            )
        }

        step("And I scroll to the bottom of conversation view and see file qa_random.txt") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertFileWithNameIsVisible("qa_random.txt")
            }
        }

        step("And I tap on group conversation title ClearContent to open group details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("ClearContent")
        }

        step("And I tap show more options button") {
            pages.groupConversationDetailsPage.tapShowMoreOptionsButton()
        }

        step("When I tap clear content button and confirm on group details page") {
            pages.groupConversationDetailsPage.apply {
                tapClearContentButton()
                tapClearContentConfirmButton()
            }
        }

        step("Then I see Conversation content was deleted toast message on group details page") {
            pages.groupConversationDetailsPage.assertToastMessageIsDisplayed("Conversation content was deleted")
        }

        step("When I close the group conversation details through X icon") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        step("And I see conversation view with ClearContent is in foreground") {
            pages.conversationViewPage.assertGroupConversationInForeground("ClearContent")
        }

        step("Then I do not see the messages or assets in current conversation") {
            pages.conversationViewPage.apply {
                assertMessageNotVisible("Hello!")
                assertMessageNotVisible("Good Morning")
                assertImageNotVisible()
                assertAudioMessageNotVisible()
                assertFileWithNameNotVisible("testing.mp4")
                assertFileWithNameNotVisible("qa_random.txt")
            }
        }
    }
}
