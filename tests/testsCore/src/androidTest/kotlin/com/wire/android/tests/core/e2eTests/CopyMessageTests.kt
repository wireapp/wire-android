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
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class CopyMessageTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4315")
    @Category("regression", "RC", "copyMessage")
    @Test
    fun givenGroupMessage_whenICopyAndPasteIt_thenCopiedMessageIsSentSuccessfully() {
        givenTeamOwnerIsLoggedInWithCopyMeGroupConversation()

        step("And I see and open conversation CopyMe") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("CopyMe")
                tapConversationNameInConversationList("CopyMe")
            }
        }

        step("And User user2Name sends message Good day! to group conversation CopyMe") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Good day!",
                "Device1",
                "CopyMe"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Good day!")
        }

        step("When I long tap message Good day! and copy it") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Good day!")
                tapCopyMessageOption()
                dismissClipboardOverlay()
            }
        }

        step("Then I see Message copied toast message") {
            waitUntilToastIsDisplayed("Message copied")
        }

        step("When I paste the copied text into the input field and send it") {
            pages.conversationViewPage.apply {
                pasteCopiedTextIntoMessageInputField("Good day!")
                clickSendButton()
            }
        }

        step("Then I see sent message Good day! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Good day!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4314")
    @Category("regression", "RC", "copyMessage", "WPB-3525")
    @Test
    fun givenConversationWithManyMessages_whenICopyMessageAfterScrolling_thenCorrectMessageIsCopied() {
        givenTeamOwnerIsLoggedInWithCopyMeGroupConversation()

        step("And I see and open conversation CopyMe") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("CopyMe")
                tapConversationNameInConversationList("CopyMe")
            }
        }

        step("And User user2Name sends 20 default messages to conversation CopyMe") {
            repeat(20) {
                testServiceHelper.userSendMessageToConversation(
                    "user2Name",
                    "1 message",
                    "Device1",
                    "CopyMe"
                )
            }
        }

        step("And User user2Name sends message That is a lot of messages to group conversation CopyMe") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "That is a lot of messages",
                "Device1",
                "CopyMe"
            )
        }

        step("And I scroll to the bottom and see message That is a lot of messages") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertReceivedMessageIsVisibleInCurrentConversation("That is a lot of messages")
            }
        }

        step("When I scroll to the top, return to the bottom and copy the message") {
            pages.conversationViewPage.apply {
                scrollToTopOfConversationScreen()
                scrollToBottomOfConversationScreen()
                longPressOnMessage("That is a lot of messages")
                tapCopyMessageOption()
                dismissClipboardOverlay()
            }
        }

        step("Then I see Message copied toast message") {
            waitUntilToastIsDisplayed("Message copied")
        }

        step("When I paste the copied text into the input field and send it") {
            pages.conversationViewPage.apply {
                pasteCopiedTextIntoMessageInputField("That is a lot of messages")
                clickSendButton()
            }
        }

        step("Then I see sent message That is a lot of messages in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("That is a lot of messages")
        }
    }

    // Shared copy-message setup: creates CopyMe, prepares Member1's device, and logs in as TeamOwner.
    @Suppress("LongMethod")
    private fun givenTeamOwnerIsLoggedInWithCopyMeGroupConversation() {
        step("Given There is a team owner user1Name with team CopyCats") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "CopyCats",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User user1Name adds user user2Name to team CopyCats with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "CopyCats",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversation CopyMe with user2Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "CopyMe",
                "user2Name",
                "CopyCats"
            )
        }

        step("And User user2Name adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
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
    }
}
