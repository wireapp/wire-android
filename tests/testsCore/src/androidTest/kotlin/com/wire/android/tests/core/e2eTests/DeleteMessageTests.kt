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
class DeleteMessageTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4316")
    @Category("regression", "RC", "conversationView", "deleteMessage")
    @Test
    fun givenOneOnOneConversation_whenIDeleteMyMessageForEveryone_thenMessageIsDeletedForEveryone() {
        givenTeamOwnerIsLoggedInWithOneOnOneConversationForDeleteMessage()

        step("And User Member1 sends message Hello! via Device1 to me") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "user1Name"
            )
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("And I open unread conversation Member1") {
            pages.conversationListPage.tapUnreadConversationNameInConversationList(member1.name ?: "")
        }

        step("And I send message Hello to you, too!") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello to you, too!")
                clickSendButton()
            }
            closeKeyboardIfOpened()
        }

        step("And I see message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("And User Member1 sees message Hello to you, too! via Device1") {
            testServiceHelper.assertMessageReceivedInPersonalMlsConversation(
                receiverAlias = "user2Name",
                deviceName = "Device1",
                conversationWithAlias = "user1Name",
                message = "Hello to you, too!"
            )
        }

        step("When I long tap message Hello to you, too! and tap delete") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello to you, too!")
                tapDeleteMessageOption()
            }
        }

        step("Then I see delete options") {
            pages.conversationViewPage.assertDeleteMessageOptionsVisible()
        }

        step("When I delete the message for everyone") {
            pages.conversationViewPage.tapDeleteForEveryoneButton()
        }

        step("Then I see Deleted message label") {
            pages.conversationViewPage.assertDeletedMessageLabelVisible()
        }

        step("And I do not see message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertMessageNotVisible("Hello to you, too!")
        }

        step("And User Member1 no longer sees message Hello to you, too! via Device1") {
            testServiceHelper.userXSeesNoMessageInPersonalMlsConversation(
                receiverAlias = "user2Name",
                deviceName = "Device1",
                conversationWithAlias = "user1Name",
                message = "Hello to you, too!"
            )
        }

        step("And I still see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4317")
    @Category("regression", "RC", "conversationView", "deleteMessage")
    @Test
    fun givenOneOnOneConversation_whenIDeleteMessagesForMyself_thenMessagesAreDeletedOnlyForMe() {
        givenTeamOwnerIsLoggedInWithOneOnOneConversationForDeleteMessage()

        step("And I open conversation Member1") {
            pages.conversationListPage.tapConversationNameInConversationList(member1.name ?: "")
        }

        step("And I send message Hello!") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
            }
        }

        step("And User Member1 sends message Hello to you, too! via Device1 to me") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "Hello to you, too!",
                "Device1",
                "user1Name"
            )
            closeKeyboardIfOpened()
        }

        step("And I see message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("And User Member1 sees message Hello to you, too! via Device1") {
            testServiceHelper.assertMessageReceivedInPersonalMlsConversation(
                receiverAlias = "user2Name",
                deviceName = "Device1",
                conversationWithAlias = "user1Name",
                message = "Hello to you, too!"
            )
        }

        step("When I long tap message Hello to you, too! and tap delete") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello to you, too!")
                tapDeleteMessageOption()
            }
        }

        step("Then I see Delete this Message for yourself confirmation") {
            pages.conversationViewPage.assertDeleteForMeConfirmationVisible()
        }

        step("When I confirm Delete for Me") {
            pages.conversationViewPage.tapDeleteForMeConfirmButton()
        }

        step("Then I see Deleted message label and no longer see message Hello to you, too!") {
            pages.conversationViewPage.apply {
                assertDeletedMessageLabelVisible()
                assertMessageNotVisible("Hello to you, too!")
            }
        }

        step("And I see message Hello! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I long tap message Hello! and tap delete") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                tapDeleteMessageOption()
            }
        }

        step("And I tap Delete for Me") {
            pages.conversationViewPage.tapDeleteForMeButton()
        }

        step("Then I see Delete this Message for yourself confirmation") {
            pages.conversationViewPage.assertDeleteForMeConfirmationVisible()
        }

        step("When I confirm Delete for Me") {
            pages.conversationViewPage.tapDeleteForMeConfirmButton()
        }

        step("Then I see Deleted message label and no longer see message Hello!") {
            pages.conversationViewPage.apply {
                assertDeletedMessageLabelVisible()
                assertMessageNotVisible("Hello!")
            }
        }

        step("And User Member1 still sees message Hello to you, too! via Device1") {
            testServiceHelper.assertMessageReceivedInPersonalMlsConversation(
                receiverAlias = "user2Name",
                deviceName = "Device1",
                conversationWithAlias = "user1Name",
                message = "Hello to you, too!"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4318")
    @Category("regression", "RC", "groups", "deleteMessage")
    @Test
    fun givenGroupConversation_whenIDeleteMyMessageForEveryone_thenMessageIsDeletedForEveryone() {
        givenTeamMemberIsLoggedInWithGroupConversationForDeleteMessage()

        step("And User Member2 sends message Hello! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello!",
                "Device1",
                "MyTeam"
            )
        }

        step("And I send message Hello to you, too!") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello to you, too!")
                clickSendButton()
            }
            closeKeyboardIfOpened()
        }

        step("And I see message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("And User Member2 sees message Hello to you, too! in MyTeam via Device1") {
            testServiceHelper.assertMessageReceivedInGroupConversation(
                receiverAlias = "user3Name",
                deviceName = "Device1",
                conversationName = "MyTeam",
                message = "Hello to you, too!"
            )
        }

        step("When I long tap message Hello to you, too! and tap delete") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello to you, too!")
                tapDeleteMessageOption()
            }
        }

        step("Then I see delete options") {
            pages.conversationViewPage.assertDeleteMessageOptionsVisible()
        }

        step("When I delete the message for everyone") {
            pages.conversationViewPage.tapDeleteForEveryoneButton()
        }

        step("Then I see Deleted message label and no longer see message Hello to you, too!") {
            pages.conversationViewPage.apply {
                assertDeletedMessageLabelVisible()
                assertMessageNotVisible("Hello to you, too!")
            }
        }

        step("And User Member2 no longer sees message Hello to you, too! in MyTeam via Device1") {
            testServiceHelper.userXSeesNoMessageInGroupConversation(
                receiverAlias = "user3Name",
                deviceName = "Device1",
                conversationName = "MyTeam",
                message = "Hello to you, too!"
            )
        }

        step("And I still see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4319")
    @Category("regression", "RC", "groups", "deleteMessage")
    @Test
    fun givenGroupConversation_whenIDeleteMessagesForMyself_thenMessagesAreDeletedOnlyForMe() {
        givenTeamMemberIsLoggedInWithGroupConversationForDeleteMessage()

        step("And I send message Hello!") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
            }
        }

        step("And User Member2 sends message Hello to you, too! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello to you, too!",
                "Device1",
                "MyTeam"
            )
            closeKeyboardIfOpened()
        }

        step("And I see message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("And User Member2 sees message Hello to you, too! in MyTeam via Device1") {
            testServiceHelper.assertMessageReceivedInGroupConversation(
                receiverAlias = "user3Name",
                deviceName = "Device1",
                conversationName = "MyTeam",
                message = "Hello to you, too!"
            )
        }

        step("When I long tap message Hello to you, too! and tap delete") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello to you, too!")
                tapDeleteMessageOption()
            }
        }

        step("Then I see Delete this Message for yourself confirmation") {
            pages.conversationViewPage.assertDeleteForMeConfirmationVisible()
        }

        step("When I confirm Delete for Me") {
            pages.conversationViewPage.tapDeleteForMeConfirmButton()
        }

        step("Then I see Deleted message label and no longer see message Hello to you, too!") {
            pages.conversationViewPage.apply {
                assertDeletedMessageLabelVisible()
                assertMessageNotVisible("Hello to you, too!")
            }
        }

        step("And I see message Hello! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("And User Member2 still sees message Hello to you, too! in MyTeam via Device1") {
            testServiceHelper.assertMessageReceivedInGroupConversation(
                receiverAlias = "user3Name",
                deviceName = "Device1",
                conversationName = "MyTeam",
                message = "Hello to you, too!"
            )
        }

        step("When I long tap message Hello! and tap delete") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                tapDeleteMessageOption()
            }
        }

        step("Then I see delete options") {
            pages.conversationViewPage.assertDeleteMessageOptionsVisible()
        }

        step("When I tap Delete for Me") {
            pages.conversationViewPage.tapDeleteForMeButton()
        }

        step("Then I see Delete this Message for yourself confirmation") {
            pages.conversationViewPage.assertDeleteForMeConfirmationVisible()
        }

        step("When I confirm Delete for Me") {
            pages.conversationViewPage.tapDeleteForMeConfirmButton()
        }

        step("Then I see Deleted message label and no longer see message Hello!") {
            pages.conversationViewPage.apply {
                assertDeletedMessageLabelVisible()
                assertMessageNotVisible("Hello!")
            }
        }

        step("And User Member2 still sees message Hello! in MyTeam via Device1") {
            testServiceHelper.assertMessageReceivedInGroupConversation(
                receiverAlias = "user3Name",
                deviceName = "Device1",
                conversationName = "MyTeam",
                message = "Hello!"
            )
        }
    }

    // Shared 1:1 setup: creates the team conversation, prepares Member1's device, and logs in as TeamOwner.
    @Suppress("LongMethod")
    private fun givenTeamOwnerIsLoggedInWithOneOnOneConversationForDeleteMessage() {
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

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And User Member1 adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
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

        step("And I see conversation Member1 in conversation list") {
            pages.conversationListPage.assertConversationVisible(member1.name ?: "")
        }
    }

    // Shared group setup: creates MyTeam, prepares Member2's device, and logs in as Member1 with MyTeam open.
    @Suppress("LongMethod")
    private fun givenTeamMemberIsLoggedInWithGroupConversationForDeleteMessage() {
        step("Given There is a team owner TeamOwner with team MessageDeleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "MessageDeleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 and Member2 to team MessageDeleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "MessageDeleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And User TeamOwner has group conversation MyTeam with Member1 and Member2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "MessageDeleting"
            )
        }

        step("And User Member2 adds a new device Device1 with label Device1") {
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

        step("And I open group conversation MyTeam and see it in foreground") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                tapConversationNameInConversationList("MyTeam")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }
    }
}
