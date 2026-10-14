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
class EditMessageTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var rememberedMessageId: String

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4330", "TC-4331", "TC-4332", "TC-4333")
    @Category("regression", "RC", "editMessage", "smoke",)
    @Test
    fun givenOneOnOneConversation_whenIEditSentMessageAndReceiveEditedMessage_thenEditsAndTimestampsAreDisplayed() {
        step("Given There is a team owner TeamOwner with team Messaging") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Messaging",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Messaging with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Messaging",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Messaging") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Messaging"
            )
        }

        step("And User Member1 adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
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

        step("And I see and open conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                tapConversationNameInConversationList(member1.name ?: "")
            }
        }

        step("When I send message Hello! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And Member1 sees and remembers message Hello! via Device1") {
            testServiceHelper.assertMessageReceivedInPersonalMlsConversation(
                receiverAlias = "user2Name",
                deviceName = "Device1",
                conversationWithAlias = "user1Name",
                message = "Hello!"
            )
            rememberedMessageId = testServiceHelper.getRecentMessageIdInPersonalMlsConversation(
                userAlias = "user2Name",
                deviceName = "Device1",
                conversationWithAlias = "user1Name"
            )
        }

        step("When I long tap message Hello! and tap Edit") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                tapEditMessageOption()
            }
        }

        step("And I replace it with Good Morning! and send the edited message") {
            pages.conversationViewPage.apply {
                editMessage("Good Morning!")
                tapSendEditedMessageButton()
            }
            closeKeyboardIfOpened()
        }

        step("Then I see message Good Morning! and no longer see message Hello!") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Good Morning!")
                assertTextMessageNotVisibleInCurrentConversation("Hello!")
            }
        }

        // TC-4331 - I want to see the edited label with timestamp for a message I edited
        step("And I see the Edited label and edit time for message Good Morning!") {
            pages.conversationViewPage.assertEditedLabelAndTimestampVisible("Good Morning!")
        }

        step("And Member1 sees the recent message changed to Good Morning! via Device1") {
            testServiceHelper.assertMessageReceivedInPersonalMlsConversation(
                receiverAlias = "user2Name",
                deviceName = "Device1",
                conversationWithAlias = "user1Name",
                message = "Good Morning!"
            )
            val editedMessageId = testServiceHelper.getRecentMessageIdInPersonalMlsConversation(
                userAlias = "user2Name",
                deviceName = "Device1",
                conversationWithAlias = "user1Name"
            )
            if (editedMessageId == rememberedMessageId) {
                throw AssertionError("Member1's recent message ID did not change after the message was edited.")
            }
        }

        // TC-4332 - I want to receive an edited message in a 1:1 conversation
        step("When Member1 sends message Hi! to me via Device1") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "Hi!",
                "Device1",
                "user1Name"
            )
        }

        step("Then I see message Hi! in the conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hi!")
        }

        step("When Member1 edits the recent message to Good Evening! via Device1") {
            testServiceHelper.userEditsLatestMessageInPersonalMlsConversation(
                senderAlias = "user2Name",
                newMessage = "Good Evening!",
                deviceName = "Device1",
                conversationWithAlias = "user1Name"
            )
        }

        step("Then I see message Good Evening! and no longer see message Hi!") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Good Evening!")
                assertTextMessageNotVisibleInCurrentConversation("Hi!")
            }
        }

        // TC-4333 - I want to see the edited label with timestamp for a message I received
        step("And I see the Edited label and edit time for message Good Evening!") {
            pages.conversationViewPage.assertEditedLabelAndTimestampVisible("Good Evening!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4334", "TC-4335", "TC-4336", "TC-4337")
    @Category("regression", "RC", "editMessage", "smoke")
    @Test
    fun givenGroupConversation_whenIEditSentMessageAndReceiveEditedMessage_thenEditsAndTimestampsAreDisplayed() {
        prepareGroupConversationAndLogin()

        step("When I send message Hello! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And Member1 sees and remembers message Hello! from group conversation EditMe via Device1") {
            testServiceHelper.assertMessageReceivedInGroupConversation(
                receiverAlias = "user2Name",
                deviceName = "Device1",
                conversationName = "EditMe",
                message = "Hello!"
            )
            rememberedMessageId = testServiceHelper.getRecentMessageIdInGroupConversation(
                userAlias = "user2Name",
                deviceName = "Device1",
                conversationName = "EditMe"
            )
        }

        step("When I long tap message Hello! and tap Edit") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                tapEditMessageOption()
            }
        }

        step("And I replace it with Good Morning! and send the edited message") {
            pages.conversationViewPage.apply {
                editMessage("Good Morning!")
                tapSendEditedMessageButton()
            }
            closeKeyboardIfOpened()
        }

        step("Then I see message Good Morning! and no longer see message Hello!") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Good Morning!")
                assertTextMessageNotVisibleInCurrentConversation("Hello!")
            }
        }

        // TC-4335 - I want to see the edited label with timestamp for a message I edited
        step("And I see the Edited label and edit time for message Good Morning!") {
            pages.conversationViewPage.assertEditedLabelAndTimestampVisible("Good Morning!")
        }

        step("And Member1 sees the recent message changed to Good Morning! in group conversation EditMe via Device1") {
            testServiceHelper.assertMessageReceivedInGroupConversation(
                receiverAlias = "user2Name",
                deviceName = "Device1",
                conversationName = "EditMe",
                message = "Good Morning!"
            )
            val editedMessageId = testServiceHelper.getRecentMessageIdInGroupConversation(
                userAlias = "user2Name",
                deviceName = "Device1",
                conversationName = "EditMe"
            )
            if (editedMessageId == rememberedMessageId) {
                throw AssertionError("Member1's recent group message ID did not change after the message was edited.")
            }
        }

        // TC-4336 - I want to receive an edited message in a group conversation
        step("When Member1 sends message Hi! to group conversation EditMe via Device1") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hi!",
                "Device1",
                "EditMe"
            )
        }

        step("Then I see message Hi! in the conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hi!")
        }

        step("When Member1 edits the recent message to Good Evening! in group conversation EditMe via Device1") {
            testServiceHelper.userEditsLatestMessageInGroupConversation(
                senderAlias = "user2Name",
                newMessage = "Good Evening!",
                deviceName = "Device1",
                conversationName = "EditMe"
            )
        }

        step("Then I see message Good Evening! and no longer see message Hi!") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Good Evening!")
                assertTextMessageNotVisibleInCurrentConversation("Hi!")
            }
        }

        // TC-4337 - I want to see the edited label with timestamp for a message I received
        step("And I see the Edited label and edit time for message Good Evening!") {
            pages.conversationViewPage.assertEditedLabelAndTimestampVisible("Good Evening!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4338")
    @Category("regression", "RC", "editMessage", "WPB-3525")
    @Test
    fun givenGroupConversationWithManyMessages_whenIScrollAwayAndBack_thenEditOptionRemainsVisible() {
        prepareGroupConversationAndLogin()

        step("And I send message Hello! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("When Member1 sends 20 default messages to group conversation EditMe via Device1") {
            repeat(20) {
                testServiceHelper.userSendMessageToConversation(
                    "user2Name",
                    "1 message",
                    "Device1",
                    "EditMe"
                )
            }
        }

        step("And I send message That is a lot of messages and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("That is a lot of messages")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("That is a lot of messages")
            }
        }

        step("And I scroll to the top, return to the bottom and long tap the message") {
            pages.conversationViewPage.apply {
                scrollToTopOfConversationScreen()
                scrollToBottomOfConversationScreen()
                longPressOnMessage("That is a lot of messages")
            }
        }

        step("Then I see the Edit message option") {
            pages.conversationViewPage.assertEditMessageOptionVisible()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8151")
    @Category("regression", "RC", "editMessage", "WPB-10773")
    @Test
    fun givenMemberMessageInGroupConversation_whenMemberEditsItMultipleTimes_thenEveryEditIsReceived() {
        prepareGroupConversationAndLogin()

        step("And Member1 sends message Hello! and I see it in the conversation") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "EditMe"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When Member1 edits the recent message to Good Morning and I see the edit") {
            testServiceHelper.userEditsLatestMessageInGroupConversation(
                senderAlias = "user2Name",
                newMessage = "Good Morning",
                deviceName = "Device1",
                conversationName = "EditMe"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Good Morning")
        }

        step("And Member1 edits the recent message to Good Day and I see the edit") {
            testServiceHelper.userEditsLatestMessageInGroupConversation(
                senderAlias = "user2Name",
                newMessage = "Good Day",
                deviceName = "Device1",
                conversationName = "EditMe"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Good Day")
        }

        step("And Member1 edits the recent message to Good Evening! and I see the edit") {
            testServiceHelper.userEditsLatestMessageInGroupConversation(
                senderAlias = "user2Name",
                newMessage = "Good Evening!",
                deviceName = "Device1",
                conversationName = "EditMe"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Good Evening!")
        }

        step("And Member1 edits the recent message to Good Night! and I see the edit") {
            testServiceHelper.userEditsLatestMessageInGroupConversation(
                senderAlias = "user2Name",
                newMessage = "Good Night!",
                deviceName = "Device1",
                conversationName = "EditMe"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Good Night!")
        }
    }

    // Creates the shared EditMe group setup and signs in TeamOwner before each group edit-message scenario.
    @Suppress("LongMethod")
    private fun prepareGroupConversationAndLogin() {
        step("Given There is a team owner TeamOwner with team Messaging") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Messaging",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Messaging with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Messaging",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation EditMe with Member1 in team Messaging") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "EditMe",
                "user2Name",
                "Messaging"
            )
        }

        step("And User Member1 adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
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

        step("And I see and open group conversation EditMe") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("EditMe")
                tapConversationNameInConversationList("EditMe")
            }
        }
    }
}
