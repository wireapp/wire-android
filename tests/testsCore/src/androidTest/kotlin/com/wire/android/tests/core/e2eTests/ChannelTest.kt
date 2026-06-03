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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import service.TestServiceHelper
import service.userSendsGenericMessageToConversation
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.iSeeSystemMessageContainingAll
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
class ChannelTest : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var member2: ClientUser

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
        testServiceHelper = TestServiceHelper(teamHelper.usersManager)
    }

    @After
    fun tearDown() {
        cleanupCreatedUsers(backendClient, teamHelper.usersManager)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8716", "TC-8717", "TC-8723")
    @Category("channels", "regression", "RC")
    @Test
    fun givenTeamMemberWithChannelFeatureEnabled_whenCreatingChannelWithTeammateAndDeletingCreatedChannel_thenChannelConversationIsCreatedAndDeleted() {
        step("There is TeamOwner with team ChannelCreation on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "ChannelCreation",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("User TeamOwner configures MLS for team ChannelCreation") {
            teamHelper.userConfiguresMLSForTeam("user1Name", "ChannelCreation", backendClient)
        }

        step("TeamOwner enables channel feature for team ChannelCreation via backdoor") {
            teamHelper.userEnablesChannelFeatureForTeam("user1Name", "ChannelCreation", backendClient)
        }

        step("User TeamOwner adds users Member1,Member2 to team ChannelCreation with role Member") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "ChannelCreation",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Register sender device and send connection request to receiver via backend") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }
        step("TeamOwner has channel conversation TestChannel in team ChannelCreation") {
            testServiceHelper.userHasChannelConversationInTeam(
                "user1Name",
                "TestChannel",
                "ChannelCreation"
            )
        }

        step("User TeamOwner is me") {
            teamOwner = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("User Member1 and Member2 are available for channel participant selection") {
            member1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
            member2 = teamHelper.usersManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation TestChannel in conversation list") {
            pages.conversationListPage.apply {
                assertChannelConversationVisible("TestChannel")
            }
        }

        step("When I tap on conversation name TestChannel in conversation list") {
            pages.conversationListPage.apply {
                clickChannelConversation("TestChannel")
            }
        }

        step("Then I see channel conversation TestChannel is in foreground") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground("TestChannel")
            }
        }

        step("And I tap on channel conversation title TestChannel to open group details") {
            pages.conversationViewPage.apply {
                UiWaitUtils.waitFor(1.seconds)
                clickOnChannelConversationDetails("TestChannel")
            }
        }

        step("And I open participants tab and start add participant flow") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                tapAddParticipantsButton()
            }
        }
        // TC-8723 - I want to add a participant to an existing channel conversation

        step("And I select Member1 and Member2 from participant suggestions") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameInSuggestionsListIs(member1.name ?: "")
                selectUserInSuggestionList(member1.name ?: "")
                assertUsernameInSuggestionsListIs(member2.name ?: "")
                selectUserInSuggestionList(member2.name ?: "")
                tapContinueButton()
            }
        }

        step("And I verify Member1 and Member2 are added to participants list and click close button") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(member1.name ?: "")
                assertUsernameIsAddedToParticipantsList(member2.name ?: "")
                tapCloseButtonOnChannelConversationDetailsPage()
            }
        }

        step("And I verify system message confirms Member1 and Member2 were added") {
            iSeeSystemMessageContainingAll(
                "You added",
                member1.name ?: "",
                member2.name ?: "",
                "to the conversation"
            )
        }

        step("And I see top of channel conversation message in conversation view") {
            pages.conversationViewPage.apply {
                assertTopOfConversationViewPageVisible()
            }
        }

        step("Then I see channel conversation TestChannel is in foreground") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground("TestChannel")
            }
        }

        step("When I type the message Hello Channel Members into text input field and tap send button") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello Channel Members")
                clickSendButton()
            }
        }

        step("Then I see the message Hello Channel Members in current conversation") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Hello Channel Members")
            }
        }

        step("And User Member2 sends message Hello from Member2 to channel conversation TestChannel") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello from Member2",
                "Device2",
                "TestChannel",
                false
            )
        }

        step("And I see the message Hello from Member2 in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello from Member2")
            }
        }

        step("And I tap back button on conversationViewPage back to conversation list page") {
            pages.conversationViewPage.apply {
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Then I see conversation TestChannel in conversation list") {
            pages.conversationListPage.apply {
                assertChannelConversationVisible("TestChannel")
            }
        }
        // TC-8717 - I want to be able to delete a channel of which I am the creator

        step("And I long press on conversation name TestChannel in conversation list") {
            pages.conversationListPage.apply {
                longPressConversation("TestChannel")
            }
        }

        step("And I see Delete Conversation button and tap it") {
            pages.conversationListPage.apply {
                assertDeleteConversationButtonVisibleInConversationActions()
                tapDeleteConversationButtonInConversationActions()
            }
        }

        step("And I see remove conversation confirmation modal for TestChannel") {
            pages.conversationListPage.apply {
                assertRemoveConversationConfirmationModalVisible("TestChannel")
            }
        }

        step("And I tap Remove Conversation button in remove conversation confirmation modal") {
            pages.conversationListPage.apply {
                tapRemoveConversationButton()
            }
        }

        step("Then I see removed toast message for TestChannel") {
            waitUntilToastIsDisplayed("“TestChannel” removed")
        }

        step("Then I do not see conversation TestChannel in conversation list") {
            pages.conversationListPage.apply {
                assertConversationNotVisible("TestChannel")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8719", "TC-8720", "TC-8721")
    @Category("channels", "regression", "RC")
    @Test
    fun givenTeamMemberWithChannelFeatureEnabled_whenLeavingChannel_thenChannelHistoryRemainsVisibleAndNewMessagesAreNotVisible() {
        step("There is TeamOwner with team LeaveChannel on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "LeaveChannel",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("User TeamOwner configures MLS for team LeaveChannel") {
            teamHelper.userConfiguresMLSForTeam("user1Name", "LeaveChannel", backendClient)
        }

        step("TeamOwner enables channel feature for team LeaveChannel via backdoor") {
            teamHelper.userEnablesChannelFeatureForTeam("user1Name", "LeaveChannel", backendClient)
        }

        step("User TeamOwner adds users Member1,Member2 to team LeaveChannel with role Member") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "LeaveChannel",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Register sender device and send connection request to receiver via backend") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }
        step("TeamOwner has channel conversation LeavingChannel in team LeaveChannel") {
            testServiceHelper.userHasChannelConversationInTeam(
                "user1Name",
                "LeavingChannel",
                "LeaveChannel"
            )
        }

        step("User TeamOwner is me") {
            teamOwner = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("User Member1 and Member2 are available for channel participant selection") {
            member1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
            member2 = teamHelper.usersManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation LeavingChannel in conversation list") {
            pages.conversationListPage.apply {
                assertChannelConversationVisible("LeavingChannel")
            }
        }

        step("When I tap on conversation name LeavingChannel in conversation list") {
            pages.conversationListPage.apply {
                clickChannelConversation("LeavingChannel")
            }
        }

        step("Then I see channel conversation LeavingChannel is in foreground") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground("LeavingChannel")
            }
        }

        step("And I tap on channel conversation title LeavingChannel to open group details") {
            pages.conversationViewPage.apply {
                UiWaitUtils.waitFor(1.seconds)
                clickOnChannelConversationDetails("LeavingChannel")
            }
        }

        step("And I open participants tab and start add participant flow") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                tapAddParticipantsButton()
            }
        }

        step("And I select Member1 and Member2 from participant suggestions") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameInSuggestionsListIs(member1.name ?: "")
                selectUserInSuggestionList(member1.name ?: "")
                assertUsernameInSuggestionsListIs(member2.name ?: "")
                selectUserInSuggestionList(member2.name ?: "")
                tapContinueButton()
            }
        }

        step("And I verify Member1 and Member2 are added to participants list and click close button") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(member1.name ?: "")
                assertUsernameIsAddedToParticipantsList(member2.name ?: "")
                tapCloseButtonOnChannelConversationDetailsPage()
            }
        }

        step("Then I see channel conversation LeavingChannel is in foreground") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground("LeavingChannel")
            }
        }

        step("When I type the message Hello Leaving Channel Members into text input field and tap send button") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello Leaving Channel Members")
                clickSendButton()
            }
        }

        step("Then I see the message Hello Leaving Channel Members in current conversation") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Hello Leaving Channel Members")
            }
        }

        step("And User Member2 sends message Hello from Member2 to channel conversation LeavingChannel") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello from Member2",
                "Device2",
                "LeavingChannel",
                false
            )
        }

        step("And I see the message Hello from Member2 in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello from Member2")
            }
        }

        step("And I tap back button on conversationViewPage back to conversation list page") {
            pages.conversationViewPage.apply {
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Then I see conversation LeavingChannel in conversation list") {
            pages.conversationListPage.apply {
                assertChannelConversationVisible("LeavingChannel")
            }
        }

        step("And I long press on conversation name LeavingChannel in conversation list") {
            pages.conversationListPage.apply {
                longPressConversation("LeavingChannel")
            }
        }

        step("And I see Leave Conversation button and tap it") {
            pages.conversationListPage.apply {
                assertLeaveConversationButtonVisibleInConversationActions()
                tapLeaveConversationButtonInConversationActions()
            }
        }

        step("And I see leave conversation confirmation modal for LeavingChannel") {
            pages.conversationListPage.apply {
                assertLeaveConversationConfirmationModalVisible("LeavingChannel")
            }
        }

        step("And I tap Leave Conversation button in leave conversation confirmation modal") {
            pages.conversationListPage.apply {
                tapLeaveConversationButtonOnModal()
            }
        }

        step("Then I see leave toast message for LeavingChannel") {
            waitUntilToastIsDisplayed("You left the conversation.")
        }

        // TC-8720 - I want to be able to see channel conversation history after I left the conversation

        step("When I tap on conversation name LeavingChannel in conversation list") {
            pages.conversationListPage.apply {
                clickChannelConversation("LeavingChannel")
            }
        }

        step("Then I see the message Hello Leaving Channel Members in current conversation") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Hello Leaving Channel Members")
            }
        }

        step("And User Member2 sends message Hello Again to channel conversation LeavingChannel") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello Again",
                "Device2",
                "LeavingChannel",
                false
            )
        }

        // TC-8721- I should not be able to see new messages after I left the channel conversation

        step("And I do not see the message Hello Again in current conversation") {
            pages.conversationViewPage.apply {
                assertMessageNotVisible("Hello Again")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8724", "TC-8725", "TC-8727")
    @Category("channels", "regression", "RC")
    @Test
    fun givenChannelConversationDeleted_whenSendingAndReceivingMessages_thenMessagesAreSentAndReceivedSuccessfully() {
        step("There is TeamOwner with team DeleteChannel on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "DeleteChannel",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("User TeamOwner configures MLS for team DeleteChannel") {
            teamHelper.userConfiguresMLSForTeam("user1Name", "DeleteChannel", backendClient)
        }

        step("TeamOwner enables channel feature for team DeleteChannel via backdoor") {
            teamHelper.userEnablesChannelFeatureForTeam("user1Name", "DeleteChannel", backendClient)
        }

        step("User TeamOwner adds users Member1,Member2 to team DeleteChannel with role Member") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "DeleteChannel",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Register sender device and send connection request to receiver via backend") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }
        step("TeamOwner has channel conversation Delete in team DeleteChannel") {
            testServiceHelper.userHasChannelConversationInTeam(
                "user1Name",
                "Delete",
                "DeleteChannel"
            )
        }

        step("User TeamOwner is me") {
            teamOwner = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("User Member1 and Member2 are available for channel participant selection") {
            member1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
            member2 = teamHelper.usersManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation Delete in conversation list") {
            pages.conversationListPage.apply {
                assertChannelConversationVisible("Delete")
            }
        }

        step("When I tap on conversation name Delete in conversation list") {
            pages.conversationListPage.apply {
                clickChannelConversation("Delete")
            }
        }

        step("And I tap on channel conversation title Delete to open group details") {
            pages.conversationViewPage.apply {
                UiWaitUtils.waitFor(1.seconds)
                clickOnChannelConversationDetails("Delete")
            }
        }

        step("And I open participants tab and start add participant flow") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                tapAddParticipantsButton()
            }
        }

        step("And I select Member1 and Member2 from participant suggestions") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameInSuggestionsListIs(member1.name ?: "")
                selectUserInSuggestionList(member1.name ?: "")
                assertUsernameInSuggestionsListIs(member2.name ?: "")
                selectUserInSuggestionList(member2.name ?: "")
                tapContinueButton()
            }
        }

        step("And I verify Member1 and Member2 are added to participants list and click close button") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(member1.name ?: "")
                assertUsernameIsAddedToParticipantsList(member2.name ?: "")
                tapCloseButtonOnChannelConversationDetailsPage()
            }
        }

        step("And I tap on channel conversation title Delete to open group details") {
            pages.conversationViewPage.apply {
                UiWaitUtils.waitFor(1.seconds)
                clickOnChannelConversationDetails("Delete")
            }
        }

        step("And I see Delete as channel name") {
            pages.groupConversationDetailsPage.apply {
                assertChannelNameVisible("Delete")
            }
        }

        step("And I tap on Delete channel name") {
            pages.groupConversationDetailsPage.apply {
                tapOnChannelName("Delete")
            }
        }

        // TC-8724- I want to be able to change channel group name as a Team owner

        step("When I change channel name to NewDelete as new channel name") {
            pages.groupConversationDetailsPage.apply {
                changeChannelName("NewDelete")
            }
        }

        step("Then I see NewDelete as channel name") {
            pages.groupConversationDetailsPage.apply {
                assertChannelNameVisible("NewDelete")
            }
        }

        step("And I see conversation is renamed toast message") {
            waitUntilToastIsDisplayed("Conversation renamed")
        }

        step("When I tap show more options button") {
            pages.groupConversationDetailsPage.apply {
                tapShowMoreOptionsButton()
            }
        }

        step("And I tap Delete Conversation button") {
            pages.groupConversationDetailsPage.apply {
                tapDeleteConversationButton()
            }
        }

        step("And I tap Remove Conversation button in remove conversation confirmation modal") {
            pages.groupConversationDetailsPage.apply {
                tapRemoveGroupButton()
            }
        }

        step("Then I see removed toast message for NewDelete") {
            waitUntilToastIsDisplayed("“NewDelete” removed")
        }

        step("Then I do not see conversation NewDelete in conversation list") {
            pages.conversationListPage.apply {
                assertConversationNotVisible("NewDelete")
            }
        }

        // TC-8727 I want to be able to receive and send messages after I deleted a channel conversation

        step("And I start a new conversation flow") {
            pages.conversationListPage.apply {
                tapStartNewConversationButton()
            }
        }

        step("And I search for Member1 and start 1:1 conversation") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(teamHelper, "user2Name")
                tapUsernameInSearchResult(member1.name ?: "")
            }
            pages.connectedUserProfilePage.apply {
                clickStartConversationButton()
            }
        }

        step("And I send message Hello Team member in the new 1:1 conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello Team member")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello Team member")
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("And I close the connected user profile page") {
            pages.connectedUserProfilePage.apply {
                tapCloseButtonOnConnectedUserProfilePage()
            }
        }

        step("And I close the search input field") {
            pages.searchPage.apply {
                clickCloseButtonOnSearchInputField()
            }
        }

        step("And I close new conversation flow and return to conversation list") {
            pages.conversationListPage.apply {
                clickCloseButtonOnNewConversationScreen()
            }
        }

        step("And Member1 sends message Hello team owner to TeamOwner via backend") {
            testServiceHelper.apply {
                userSendsGenericMessageToConversation(
                    "user2Name",
                    "user1Name",
                    "Device1",
                    "Hello team owner"
                )
            }
        }

        step("Then I see conversation with Member1 has 1 unread message in conversation list") {
            pages.conversationListPage.apply {
                assertConversationHasUnreadMessagesCount(member1.name ?: "", "1")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8722", "TC-8726", "TC-8729")
    @Category("channels", "regression", "RC")
    @Test
    fun givenChannelConversationMembersAreRemovedAndAdded_whenViewingParticipantList_thenParticipantListIsUpdatedCorrectly() {
        step("Given there is TeamOwner with team UpdateParticipantList on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "UpdateParticipantList",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner configures MLS for team UpdateParticipantList") {
            teamHelper.userConfiguresMLSForTeam("user1Name", "UpdateParticipantList", backendClient)
        }

        step("And TeamOwner enables channel feature for team UpdateParticipantList via backdoor") {
            teamHelper.userEnablesChannelFeatureForTeam("user1Name", "UpdateParticipantList", backendClient)
        }

        step("And User TeamOwner adds users Member1,Member2 to team UpdateParticipantList with role Member") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "UpdateParticipantList",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And Member1 adds a new device Device2 with label Device2") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device2")
            }
        }

        step("And TeamOwner has channel conversation UpdateList in team UpdateParticipantList") {
            testServiceHelper.userHasChannelConversationInTeam(
                "user1Name",
                "UpdateList",
                "UpdateParticipantList"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User Member1 and Member2 are available for channel participant selection") {
            member1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
            member2 = teamHelper.usersManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation UpdateList in conversation list") {
            pages.conversationListPage.apply {
                assertChannelConversationVisible("UpdateList")
            }
        }

        step("When I tap on conversation name UpdateList in conversation list") {
            pages.conversationListPage.apply {
                clickChannelConversation("UpdateList")
            }
        }

        step("And I tap on channel conversation title UpdateList to open group details") {
            pages.conversationViewPage.apply {
                UiWaitUtils.waitFor(1.seconds)
                clickOnChannelConversationDetails("UpdateList")
            }
        }

        step("And I open participants tab") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
            }
        }

        step("Then I do not see Member1 in the participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUserIsNotInParticipantsList(member1.name ?: "")
            }
        }

        step("When I start add participant flow") {
            pages.groupConversationDetailsPage.apply {
                tapAddParticipantsButton()
            }
        }

        step("And I select Member1 from participant suggestions") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameInSuggestionsListIs(member1.name ?: "")
                selectUserInSuggestionList(member1.name ?: "")
                tapContinueButton()
            }
        }

        step("Then I verify Member1 is added to participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(member1.name ?: "")
            }
        }

        step("And I tap on Member1 in participants list") {
            pages.groupConversationDetailsPage.apply {
                tapUserInParticipantsList(member1.name ?: "")
            }
        }

        // TC-8722 I want to remove a participant from a channel conversation

        step("And I tap Remove from conversation button") {
            pages.connectedUserProfilePage.apply {
                assertRemoveFromConversationButtonForParticipant()
                tapRemoveFromConversationButtonForParticipant()
            }
        }

        step("And I see alert asking if I want to remove Member1 and I tap Remove button on the modal") {
            pages.connectedUserProfilePage.apply {
                assetRemoveConversationButtonOnModal()
                tapRemoveConversationButtonOnModal()
            }
        }

        step("Then I see toast message that ${member1.uniqueUsername} was removed from the conversation") {
            waitUntilToastIsDisplayed("${member1.uniqueUsername} was removed from the conversation")
        }

        step("And I close the connected user profile page") {
            pages.connectedUserProfilePage.apply {
                tapCloseButtonOnConnectedUserProfilePage()
            }
        }

        step("Then I do not see Member1 in the participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUserIsNotInParticipantsList(member1.name ?: "")
            }
        }

        // TC-8726 I want to be able to leave a channel conversation from the channel details page

        step("When I tap show more options button") {
            pages.groupConversationDetailsPage.apply {
                tapShowMoreOptionsButton()
            }
        }
        step("And I see Leave Conversation button and tap it") {
            pages.conversationListPage.apply {
                assertLeaveConversationButtonVisibleInConversationActions()
                tapLeaveConversationButtonInConversationActions()
            }
        }

        step("And I see leave conversation confirmation modal for UpdateList") {
            pages.conversationListPage.apply {
                assertLeaveConversationConfirmationModalVisible("UpdateList")
            }
        }

        step("And I tap Leave Conversation button in leave conversation confirmation modal") {
            pages.conversationListPage.apply {
                tapLeaveConversationButtonOnModal()
            }
        }

        step("Then I see leave toast message for UpdateList") {
            waitUntilToastIsDisplayed("You left the conversation.")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-26060")
    @Category("channels", "regression", "RC")
    @Test
    fun givenExternalUserInTeam_whenAttemptingToCreateChannelConversation_thenChannelConversationCannotBeCreated() {
        step("There is TeamOwner with team CreateChannel on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "CreateChannel",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("User TeamOwner configures MLS for team CreateChannel") {
            teamHelper.userConfiguresMLSForTeam("user1Name", "CreateChannel", backendClient)
        }

        step("TeamOwner enables channel feature for team CreateChannel via backdoor") {
            teamHelper.userEnablesChannelFeatureForTeam("user1Name", "CreateChannel", backendClient)
        }

        step("User TeamOwner adds user Member1 to team CreateChannel with role External") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "CreateChannel",
                TeamRoles.External,
                backendClient,
                context,
                true
            )
        }

        step("Member1 adds a new device Device1 via backend") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
            }
        }

        step("User Member1 is available for login") {
            member1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as Member1") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(member1.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(member1.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I tap Start new conversation flow from conversation list") {
            pages.conversationListPage.apply {
                tapStartNewConversationButton()
            }
        }

        step("Then I do not see create new channel button") {
            pages.conversationListPage.apply {
                assertCreateNewChannelButtonNotVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-26086")
    @Category("channels", "regression", "RC")
    @Test
    fun givenUserIsNotCreatorOfChannelConversation_whenViewingChannelConversationOptions_thenDeleteConversationButtonIsNotVisible() {
        step("There is TeamOwner with team DeleteChannel on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "DeleteChannel",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("User TeamOwner configures MLS for team DeleteChannel") {
            teamHelper.userConfiguresMLSForTeam("user1Name", "DeleteChannel", backendClient)
        }

        step("TeamOwner enables channel feature for team DeleteChannel via backdoor") {
            teamHelper.userEnablesChannelFeatureForTeam("user1Name", "DeleteChannel", backendClient)
        }

        step("User TeamOwner adds users Member1 to team DeleteChannel with role Member") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "DeleteChannel",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Member1 adds a new device Device1 via backend") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
            }
        }

        step("TeamOwner has channel conversation UnableToDelete in team DeleteChannel") {
            testServiceHelper.userHasChannelConversationInTeam(
                "user1Name",
                "UnableToDelete",
                "DeleteChannel"
            )
        }

        step("User TeamOwner is me") {
            teamOwner = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("User Member1 is available for channel participant selection and login") {
            member1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation UnableToDelete in conversation list") {
            pages.conversationListPage.apply {
                assertChannelConversationVisible("UnableToDelete")
            }
        }

        step("When I tap on conversation name UnableToDelete in conversation list") {
            pages.conversationListPage.apply {
                clickChannelConversation("UnableToDelete")
            }
        }

        step("And I tap on channel conversation title UnableToDelete to open group details") {
            pages.conversationViewPage.apply {
                UiWaitUtils.waitFor(1.seconds)
                clickOnChannelConversationDetails("UnableToDelete")
            }
        }

        step("And I open participants tab and start add participant flow") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                tapAddParticipantsButton()
            }
        }

        step("And I select Member1 from participant suggestions") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameInSuggestionsListIs(member1.name ?: "")
                selectUserInSuggestionList(member1.name ?: "")
                tapContinueButton()
            }
        }

        step("And I verify Member1 is added to participants list and click close button") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(member1.name ?: "")
                tapCloseButtonOnChannelConversationDetailsPage()
            }
        }

        step("And I verify system message confirms Member1 was added") {
            iSeeSystemMessageContainingAll(
                "You added",
                member1.name ?: "",
                "to the conversation"
            )
        }

        step("Then I see channel conversation UnableToDelete is in foreground") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground("UnableToDelete")
            }
        }

        step("And I tap back button on conversationViewPage back to conversation list page") {
            pages.conversationViewPage.apply {
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("And I tap User Profile Button") {
            pages.conversationListPage.apply {
                clickUserProfileButton()
            }
        }

        step("And I see User Profile Page") {
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
            }
        }

        step("When I tap New Team or Account button") {
            pages.selfUserProfilePage.apply {
                tapNewTeamOrAddAccountButton()
            }
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as Member1") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(member1.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(member1.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation UnableToDelete in conversation list") {
            pages.conversationListPage.apply {
                assertChannelConversationVisible("UnableToDelete")
            }
        }

        step("And I long press on conversation name UnableToDelete in conversation list") {
            pages.conversationListPage.apply {
                longPressConversation("UnableToDelete")
            }
        }

        step("Then I do not see Delete Conversation button") {
            pages.conversationListPage.apply {
                assertDeleteConversationButtonNotVisibleInConversationActions()
            }
        }
    }
}
