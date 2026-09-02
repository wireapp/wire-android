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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
class MessagingTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private var wifiDisabledByTest = false

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_BETA)
    }

    @After
    fun tearDown() {
        if (wifiDisabledByTest) {
            pages.commonAppPage.enableWifi()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4450")
    @Category("regression", "RC", "messaging", "smoke", "smokeSchwarz")
    @Test
    fun givenOneOnOneConversationWithTeamMember_whenIExchangeMessages_thenMessagesAreSentAndReceived() {
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

        step("And User TeamOwner adds Member1 to team Messaging with role Member") {
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

        step("And I see and open conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                tapConversationNameInConversationList(member1.name ?: "")
            }
        }

        step("When I send message Hello!") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
            }
        }

        step("Then I see message Hello! in current conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("And User Member1 sends message Hello to you, too! to me via Device1") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "Hello to you, too!",
                "Device1",
                "user1Name"
            )
        }

        step("And I see message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4452")
    @Category("regression", "RC", "messaging")
    @Test
    fun givenOneOnOneConversationWithTeamMember_whenIExchangeVeryLongMessage_thenMessagesAreSentAndReceived() {
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

        step("And User TeamOwner adds Member1 to team Messaging with role Member") {
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

        step("And I see and open conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                tapConversationNameInConversationList(member1.name ?: "")
            }
        }

        step("When I send a generic message with 8000 characters") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Long message ".repeat(667).take(8000))
                clickSendButton()
            }
        }

        step("Then I see a message is displayed in the conversation view") {
            pages.conversationViewPage.assertMessageIsDisplayedInConversationView()
        }

        step("When User Member1 sends message Hello! to me via Device1") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "user1Name"
            )
        }

        step("Then I see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4451")
    @Category("regression", "RC", "messaging")
    @Test
    fun givenGroupConversationWithTeamMember_whenIExchangeVeryLongMessage_thenMessagesAreSentAndReceived() {
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

        step("And User TeamOwner adds Member1 to team Messaging with role Member") {
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

        step("And User TeamOwner has group conversation LongMessage with Member1 in team Messaging") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "LongMessage",
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

        step("And I see and open group conversation LongMessage") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("LongMessage")
                tapConversationNameInConversationList("LongMessage")
            }
        }

        step("When I send a generic message with 8000 characters") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Long message ".repeat(667).take(8000))
                clickSendButton()
            }
        }

        step("Then I see a message is displayed in the conversation view") {
            pages.conversationViewPage.assertMessageIsDisplayedInConversationView()
        }

        step("When User Member1 sends message Hello! to group conversation LongMessage via Device1") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "LongMessage"
            )
        }

        step("Then I see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4453")
    @Category("regression", "RC", "messaging")
    @Test
    fun givenGroupConversation_whenISendMessageLongerThan8000Characters_thenMessageIsNotSent() {
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

        step("And User TeamOwner adds Member1 to team Messaging with role Member") {
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

        step("And User TeamOwner has group conversation LongMessage with Member1 in team Messaging") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "LongMessage",
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

        step("And I see and open group conversation LongMessage") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("LongMessage")
                tapConversationNameInConversationList("LongMessage")
            }
        }

        step("When I try to send a generic message with 8002 characters") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Long message ".repeat(667).take(8002))
                clickSendButton()
            }
        }

        step("Then I do not see the 8002-character message in the conversation view") {
            pages.conversationViewPage.assertTextMessageNotVisibleInCurrentConversation(
                "Long message ".repeat(667).take(8002)
            )
        }

        step("When User Member1 sends message Hello! to group conversation LongMessage via Device1") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "LongMessage"
            )
        }

        step("Then I see message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }
    }

    // Removed regression tag because "Pending messages" feature is enabled in alpha builds
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4454", "TC-4456")
    @Category("RC", "messaging")
    @Test
    fun givenMessageWasNotSentDueToNetworkIssues_whenIRetryAfterNetworkIsRestored_thenMessageIsSent() {
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

        step("And User TeamOwner adds Member1 to team Messaging with role Member") {
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

        step("And User TeamOwner has group conversation GoingOffline with Member1 in team Messaging") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "GoingOffline",
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

        step("And I see and open group conversation GoingOffline") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GoingOffline")
                tapConversationNameInConversationList("GoingOffline")
            }
        }

        step("When I disable Wi-Fi on the device") {
            wifiDisabledByTest = true
            pages.commonAppPage.disableWifi()
        }

        step("And I send message Going offline is healthy from time to time.") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Going offline is healthy from time to time.")
                clickSendButton()
            }
        }

        step("Then I see the connectivity error with Retry and Cancel buttons") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation(
                    "Message could not be sent due to connectivity issues."
                )
                assertRetryButtonVisible()
                assertCancelButtonVisible()
            }
        }

        // TC-4456 - I want to see the retry button again when the message still cannot be sent due to network issues.
        step("When I retry sending the message while offline") {
            pages.conversationViewPage.tapRetryButton()
        }

        step("Then I still see the connectivity error with Retry and Cancel buttons") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation(
                    "Message could not be sent due to connectivity issues."
                )
                assertRetryButtonVisible()
                assertCancelButtonVisible()
            }
        }

        step("When I enable Wi-Fi and wait until it is connected again") {
            pages.commonAppPage.apply {
                enableWifi()
                waitUntilWifiIsEnabled()
            }
            pages.conversationListPage.apply {
                waitUntilWaitingForNetworkIsInvisible()
                waitUntilDecryptingMessagesBannerIsInvisible()
            }
            wifiDisabledByTest = false
        }

        step("And I retry sending the message") {
            pages.conversationViewPage.tapRetryButton()
        }

        step("Then I see the message is sent and the connectivity error is no longer visible") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Going offline is healthy from time to time.")
                assertMessageNotVisible("Message could not be sent due to connectivity issues.")
            }
        }
        step("When User Member1 sends message Welcome back online. to group conversation GoingOffline via Device1") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Welcome back online.",
                "Device1",
                "GoingOffline"
            )
        }

        step("Then I see message Welcome back online. in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Welcome back online.")
        }
    }

    // Removed regression tag because "Pending messages" feature is enabled in alpha builds
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4455")
    @Category("RC", "messaging")
    @Test
    fun givenMessageWasNotSentDueToNetworkIssues_whenICancelResending_thenMessageIsRemoved() {
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

        step("And User TeamOwner adds Member1 to team Messaging with role Member") {
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

        step("And User TeamOwner has group conversation GoingOffline with Member1 in team Messaging") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "GoingOffline",
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

        step("And I see and open group conversation GoingOffline") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GoingOffline")
                tapConversationNameInConversationList("GoingOffline")
            }
        }

        step("When I disable Wi-Fi on the device") {
            wifiDisabledByTest = true
            pages.commonAppPage.disableWifi()
        }

        step("And I send message Going offline is healthy from time to time.") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Going offline is healthy from time to time.")
                clickSendButton()
            }
        }

        step("Then I see the connectivity error with Retry and Cancel buttons") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation(
                    "Message could not be sent due to connectivity issues."
                )
                assertRetryButtonVisible()
                assertCancelButtonVisible()
            }
        }

        step("When I enable Wi-Fi and wait until it is connected again") {
            pages.commonAppPage.apply {
                enableWifi()
                waitUntilWifiIsEnabled()
            }
            pages.conversationListPage.apply {
                waitUntilWaitingForNetworkIsInvisible()
                waitUntilDecryptingMessagesBannerIsInvisible()
            }
            wifiDisabledByTest = false
        }

        step("And I cancel resending the message and tap Delete for Me") {
            pages.conversationViewPage.apply {
                tapCancelButton()
                tapDeleteForMeButton()
            }
        }

        step("Then I do not see the message or connectivity error") {
            pages.conversationViewPage.apply {
                assertTextMessageNotVisibleInCurrentConversation("Going offline is healthy from time to time.")
                assertMessageNotVisible("Message could not be sent due to connectivity issues.")
            }
        }
        step("When User Member1 sends message Welcome back online! to group conversation GoingOffline via Device1") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Welcome back online!",
                "Device1",
                "GoingOffline"
            )
        }

        step("Then I see message Welcome back online! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Welcome back online!")
        }
    }
}
