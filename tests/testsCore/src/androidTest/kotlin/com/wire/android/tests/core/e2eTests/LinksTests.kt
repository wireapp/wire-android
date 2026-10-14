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
class LinksTests : BaseUiTest() {

    private lateinit var teamOwner: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4433")
    @Category("regression", "RC", "links")
    @Test
    fun givenGroupConversationWithLink_whenITapReceivedLink_thenWebpageOpensInBrowser() {
        step("Given There is a team owner TeamOwner with team Linking") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Linking",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Linking with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Linking",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner has group conversation WeLikeLinks with Member1 in team Linking") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "WeLikeLinks",
                "user2Name",
                "Linking"
            )
        }

        step("And User TeamOwner is me") {
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

        step("And I see and open group conversation WeLikeLinks") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("WeLikeLinks")
                tapConversationNameInConversationList("WeLikeLinks")
            }
        }

        step("When User Member1 sends link www.github.com to group conversation WeLikeLinks") {
            testServiceHelper.userSendsGenericMessageToConversation(
                "user2Name",
                "WeLikeLinks",
                "Device1",
                "www.github.com"
            )
        }

        step("And I see and tap link www.github.com in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("www.github.com")
                tapLinkInCurrentConversation("www.github.com")
            }
        }

        step("Then I see an alert informing me that I will be forwarded to www.github.com in my browser") {
            pages.conversationViewPage.assertLinkOpeningAlertVisible("www.github.com")
        }

        step("When I tap open button on the link alert") {
            pages.conversationViewPage.tapOpenButtonOnLinkAlert()
        }

        step("Then I see the Wire app is not in foreground") {
            pages.commonAppPage.assertWireAppIsNotInForeground()
        }

        step("And I dismiss Chrome prompts and see webpage with github in foreground") {
            pages.chromePage.apply {
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
            }
            pages.settingsPage.assertChromeUrlIsDisplayed("github")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4434")
    @Category("regression", "RC", "links", "WPB-3518")
    @Test
    fun givenConversationWithManyMessages_whenITapLinkAfterScrolling_thenWebpageOpensInBrowser() {
        step("Given There is a team owner TeamOwner with team Linking") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Linking",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team Linking with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Linking",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation WeLikeLinks with Member1 in team Linking") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "WeLikeLinks",
                "user2Name",
                "Linking"
            )
        }

        step("And User Member1 adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner is me") {
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

        step("And I see and open group conversation WeLikeLinks") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("WeLikeLinks")
                tapConversationNameInConversationList("WeLikeLinks")
            }
        }

        step("When User Member1 sends link www.github.com to group conversation WeLikeLinks") {
            testServiceHelper.userSendsGenericMessageToConversation(
                "user2Name",
                "WeLikeLinks",
                "Device1",
                "www.github.com"
            )
        }

        step("And I see and tap link www.github.com in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("www.github.com")
                tapLinkInCurrentConversation("www.github.com")
                assertLinkOpeningAlertVisible("www.github.com")
                tapOpenButtonOnLinkAlert()
            }
        }

        step("And I dismiss Chrome prompts and see the Wire app is not in foreground") {
            pages.chromePage.dismissFirstRunIfVisible()
            pages.commonAppPage.assertWireAppIsNotInForeground()
        }

        step("And I restart Wire and see the Wire app is in foreground") {
            pages.commonAppPage.apply {
                restartWireApp()
                assertWireAppIsInForeground()
            }
        }

        step("And User Member1 sends 20 default messages to conversation WeLikeLinks") {
            repeat(20) {
                testServiceHelper.userSendMessageToConversation(
                    "user2Name",
                    "1 message",
                    "Device1",
                    "WeLikeLinks"
                )
            }
        }

        step("And I send message That is a lot of messages and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("That is a lot of messages")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("That is a lot of messages")
            }
        }

        step("And User Member1 sends message Yes! to conversation WeLikeLinks") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Yes!",
                "Device1",
                "WeLikeLinks"
            )
        }

        step("And I see message Yes! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Yes!")
        }

        step("When I scroll to the top, bottom and top of conversation view") {
            closeKeyboardIfOpened()
            pages.conversationViewPage.apply {
                scrollToTopOfConversationScreen()
                scrollToBottomOfConversationScreen()
                scrollToTopOfConversationScreen()
            }
        }

        step("And I tap link www.github.com and open it in my browser") {
            pages.conversationViewPage.apply {
                tapLinkInCurrentConversation("www.github.com")
                assertLinkOpeningAlertVisible("www.github.com")
                tapOpenButtonOnLinkAlert()
            }
        }

        step("Then I see the Wire app is not in foreground and webpage with github is open") {
            pages.commonAppPage.assertWireAppIsNotInForeground()
            pages.chromePage.dismissNotificationsPromptIfVisible()
            pages.settingsPage.assertChromeUrlIsDisplayed("github")
        }
    }
}
