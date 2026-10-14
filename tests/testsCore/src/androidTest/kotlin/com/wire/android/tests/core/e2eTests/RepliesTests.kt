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

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
class RepliesTests : BaseUiTest() {
    private var teamOwner: ClientUser? = null
    private var member1: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4495")
    @Category("regression", "RC", "replies")
    @Test
    fun givenGroupConversation_whenIReplyToReceivedMessage_thenReplyQuotesTheOriginalMessage() {
        step("Given There is a team owner TeamOwner with team Reply") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reply",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Reply with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Reply",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation Replies with Member1 in team Reply") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "Replies",
                "user2Name",
                "Reply"
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        loginAsMember1()

        step("And I see and open group conversation Replies") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Replies")
                tapConversationNameInConversationList("Replies")
            }
        }

        step("When User TeamOwner sends message What is your favorite pizza? to group conversation Replies") {
            testServiceHelper.userSendMessageToConversation(
                "user1Name",
                "What is your favorite pizza?",
                "Device1",
                "Replies"
            )
        }

        step("And I see and long tap message What is your favorite pizza? in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("What is your favorite pizza?")
                longPressOnMessage("What is your favorite pizza?")
            }
        }

        step("Then I see reply option") {
            pages.conversationViewPage.assertReplyOptionVisible()
        }

        step("When I tap reply option") {
            pages.conversationViewPage.tapReplyOption()
        }

        step("Then I see message What is your favorite pizza? as preview in message input field") {
            pages.conversationViewPage.assertReplyPreviewVisible("What is your favorite pizza?")
        }

        step("When I type message Tuna!, tap send and hide the keyboard") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Tuna!")
                clickSendButton()
            }
            closeKeyboardIfOpened()
        }

        step("Then I see message Tuna! as a reply to What is your favorite pizza? in conversation view") {
            pages.conversationViewPage.assertReplyToMessageVisible("Tuna!", "What is your favorite pizza?")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4496", "TC-4497")
    @Category("regression", "RC", "replies")
    @Test
    fun givenGroupConversation_whenIReceiveReplyToMyMessageAndReplyBack_thenBothRepliesQuoteTheCorrectMessages() {
        step("Given There is a team owner TeamOwner with team Reply") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reply",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Reply with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Reply",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation Replies with Member1 in team Reply") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "Replies",
                "user2Name",
                "Reply"
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        loginAsMember1()

        step("And I see and open group conversation Replies") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Replies")
                tapConversationNameInConversationList("Replies")
            }
        }

        step("When I type message What is your favorite pizza?, tap send and hide the keyboard") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("What is your favorite pizza?")
                clickSendButton()
            }
            closeKeyboardIfOpened()
        }

        step("Then I see message What is your favorite pizza? in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("What is your favorite pizza?")
        }

        step("When User TeamOwner sends Hawaii! as a reply to the last message in Replies via Device1") {
            testServiceHelper.userRepliesToLatestTextMessageInGroupConversation(
                senderAlias = "user1Name",
                conversationName = "Replies",
                deviceName = "Device1",
                reply = "Hawaii!"
            )
        }

        step("Then I see message Hawaii! as a reply to What is your favorite pizza? in conversation view") {
            pages.conversationViewPage.assertReplyToMessageVisible("Hawaii!", "What is your favorite pizza?")
        }

        // TC-4497 - I want to reply to a reply in a group conversation.
        step("When I long tap message Hawaii! and tap reply option") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hawaii!")
                tapReplyOption()
            }
        }

        step("And I see message Hawaii! as preview in message input field") {
            pages.conversationViewPage.assertReplyPreviewVisible("Hawaii!")
        }

        step("When I type message Wow!, tap send and hide the keyboard") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Wow!")
                clickSendButton()
            }
            closeKeyboardIfOpened()
        }

        step("Then I see message Wow! as a reply to Hawaii! in conversation view") {
            pages.conversationViewPage.assertReplyToMessageVisible("Wow!", "Hawaii!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4498")
    @Category("regression", "RC", "replies")
    @Test
    fun givenOneOnOneConversation_whenIReplyToReceivedMessage_thenISeeMyReplyInConversation() {
        step("Given There is a team owner TeamOwner with team Reply") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reply",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Reply with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Reply",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Reply") {
            // The 1:1 setup helper also registers Device1 for both users.
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Reply"
            )
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        loginAsMember1()

        step("And I see and open conversation TeamOwner") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwner?.name ?: "")
                tapConversationNameInConversationList(teamOwner?.name ?: "")
            }
        }

        step("When User TeamOwner sends message Hello to Member1 via Device1") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user1Name",
                "Hello",
                "Device1",
                "user2Name"
            )
        }

        step("And I see and long tap message Hello in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello")
                longPressOnMessage("Hello")
            }
        }

        step("When I see and tap reply option") {
            pages.conversationViewPage.apply {
                assertReplyOptionVisible()
                tapReplyOption()
            }
        }

        step("Then I see message Hello as preview in message input field") {
            pages.conversationViewPage.assertReplyPreviewVisible("Hello")
        }

        step("When I type message Bye, tap send and hide the keyboard") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Bye")
                clickSendButton()
            }
            closeKeyboardIfOpened()
        }

        step("Then I see message Bye as a reply to Hello in conversation view") {
            pages.conversationViewPage.assertReplyToMessageVisible("Bye", "Hello")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4499")
    @Category("regression", "RC", "replies")
    @Test
    fun givenOneOnOneConversation_whenTeamOwnerRepliesToMyMessage_thenISeeReplyToMyOwnMessage() {
        step("Given There is a team owner TeamOwner with team Reply") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reply",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Reply with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Reply",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Reply") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Reply"
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        loginAsMember1()

        step("And I see and open conversation TeamOwner") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwner?.name ?: "")
                tapConversationNameInConversationList(teamOwner?.name ?: "")
            }
        }

        step("When I type message How you doing? and tap send") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("How you doing?")
                clickSendButton()
            }
        }

        step("Then I see message How you doing? in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("How you doing?")
        }

        step("When User TeamOwner sends Fine as a reply to the last message from Member1 via Device1") {
            testServiceHelper.userRepliesToLatestTextMessageInPersonalMlsConversation(
                senderAlias = "user1Name",
                conversationWithAlias = "user2Name",
                deviceName = "Device1",
                reply = "Fine"
            )
        }

        step("Then I see message Fine as a reply to How you doing? in conversation view") {
            pages.conversationViewPage.assertReplyToMessageVisible("Fine", "How you doing?")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4500")
    @Category("regression", "RC", "replies")
    @Test
    fun givenOneOnOneConversation_whenIReplyToAReply_thenISeeMyReplyInConversation() {
        step("Given There is a team owner TeamOwner with team Reply") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reply",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Reply with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Reply",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Reply") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Reply"
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        loginAsMember1()

        step("And I see and open conversation TeamOwner") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwner?.name ?: "")
                tapConversationNameInConversationList(teamOwner?.name ?: "")
            }
        }

        step("When I type message VacationPlans, tap send and hide the keyboard") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("VacationPlans")
                clickSendButton()
            }
            closeKeyboardIfOpened()
        }

        step("Then I see message VacationPlans in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("VacationPlans")
        }

        step("When User TeamOwner sends London as a reply to the last message from Member1 via Device1") {
            testServiceHelper.userRepliesToLatestTextMessageInPersonalMlsConversation(
                senderAlias = "user1Name",
                conversationWithAlias = "user2Name",
                deviceName = "Device1",
                reply = "London"
            )
        }

        step("When I long tap message London and tap reply option") {
            pages.conversationViewPage.apply {
                longPressOnMessage("London")
                tapReplyOption()
            }
        }

        step("And I see message London as preview in message input field") {
            pages.conversationViewPage.assertReplyPreviewVisible("London")
        }

        step("When I type message Vancouver, tap send and hide the keyboard") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Vancouver")
                clickSendButton()
            }
            closeKeyboardIfOpened()
        }

        step("Then I see message Vancouver as a reply to London in conversation view") {
            pages.conversationViewPage.assertReplyToMessageVisible("Vancouver", "London")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4501")
    @Category("regression", "RC", "replies", "WPB-3525")
    @Test
    fun givenGroupConversationWithManyMessages_whenIReplyAfterScrolling_thenISeeMyReplyToTheCorrectMessage() {
        step("Given There is a team owner TeamOwner with team Reply") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reply",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Reply with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Reply",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation Replies with Member1 in team Reply") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "Replies",
                "user2Name",
                "Reply"
            )
        }

        step("And User Member1 adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
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
                enterTeamOwnerLoggingEmail(teamOwner?.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(teamOwner?.password ?: "")
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

        step("And I see and open group conversation Replies") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Replies")
                tapConversationNameInConversationList("Replies")
            }
        }

        step("And User Member1 sends 20 default messages to conversation Replies") {
            repeat(20) {
                testServiceHelper.userSendMessageToConversation(
                    "user2Name",
                    "1 message",
                    "Device1",
                    "Replies"
                )
            }
        }

        step("And User Member1 sends message That is a lot of messages to group conversation Replies via Device1") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "That is a lot of messages",
                "Device1",
                "Replies"
            )
        }

        step("And I scroll to the bottom and see message That is a lot of messages") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertReceivedMessageIsVisibleInCurrentConversation("That is a lot of messages")
            }
        }

        step("And I scroll to the top and return to the bottom of the conversation") {
            pages.conversationViewPage.apply {
                scrollToTopOfConversationScreen()
                scrollToBottomOfConversationScreen()
            }
        }

        step("When I long tap message That is a lot of messages and tap reply option") {
            pages.conversationViewPage.apply {
                longPressOnMessage("That is a lot of messages")
                tapReplyOption()
            }
        }

        step("And I see message That is a lot of messages as preview in message input field") {
            pages.conversationViewPage.assertReplyPreviewVisible("That is a lot of messages")
        }

        step("When I type message Yes! and tap send") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Yes!")
                clickSendButton()
            }
        }

        step("Then I see message Yes! as a reply to That is a lot of messages in conversation view") {
            pages.conversationViewPage.assertReplyToMessageVisible("Yes!", "That is a lot of messages")
        }
    }

    // Shared Member1 login: selects staging, signs in, and handles the post-login prompts.
    private fun loginAsMember1() {
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
                enterTeamOwnerLoggingEmail(member1?.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(member1?.password ?: "")
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
