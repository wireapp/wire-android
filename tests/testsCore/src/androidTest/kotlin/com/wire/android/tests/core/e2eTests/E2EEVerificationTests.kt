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
import createOneKbFileInDeviceDownloadsFolder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class E2EEVerificationTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8116", "TC-8117", "TC-8119")
    @Category("regression", "RC", "E2EEVerification")
    @Test
    fun givenOtherUserHasDevices_whenIVerifyThem_thenGroupConversationIsVerifiedAndDegradationAlertsAreDisplayed() {
        step("Given There is a team owner TeamOwner with team ProteusVerification") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ProteusVerification",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team ProteusVerification with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ProteusVerification",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team ProteusVerification") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "ProteusVerification"
            )
        }

        step("And User TeamOwner has group conversation Verification with Member1") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "Verification",
                "user2Name",
                "ProteusVerification"
            )
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

        step("And I complete login and handle the permission prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see group conversation Verification in the list and open it") {
            pages.conversationListPage.apply {
                assertConversationVisible("Verification")
                tapConversationNameInConversationList("Verification")
            }
        }

        // To receive Device Key Fingerprint for verifying devices, we need to send a message from a device first
        step("And I send and see message Hello!") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("When I close conversation Verification and open Settings") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("And I open Manage your Devices and select Device1") {
            pages.settingsPage.tapManageDevicesMenu()
            pages.devicesPage.tapDevice("Device1")
        }

        step("When I see Device1 is not verified and tap the verify button") {
            pages.devicesPage.assertDeviceNotVerified()
            pages.devicesPage.tapVerifyDeviceButton()
        }

        step("Then I see Device1 is verified and close the device details screen") {
            pages.devicesPage.assertDeviceVerified()
            pages.devicesPage.closeDevicesScreen()
            UiWaitUtils.waitFor(1.seconds)
        }

        step("And I close the devices screen and open the main navigation menu") {
            pages.devicesPage.closeDevicesScreen()
            pages.conversationListPage.clickConversationsMenuEntry()
        }

        step("And I open Conversations and see the conversation list") {
            pages.conversationListPage.apply {
                clickConversationsButtonOnMenuEntry()
                assertConversationListVisible()
            }
        }

        step("When I tap Member1 in the conversation list and see the 1:1 conversation in foreground") {
            pages.conversationListPage.tapConversationNameInConversationList(member1.name ?: "")
            pages.conversationViewPage.assertOneOnOneConversationInForeground(member1.name ?: "")
        }

        step("And I open the 1:1 conversation details and see Member1's profile") {
            pages.conversationViewPage.click1On1ConversationDetails(member1.name ?: "")
            pages.connectedUserProfilePage.assertConnectedUserProfileVisible(member1.name ?: "")
        }

        step("And I tap the devices tab and see Desktop listed in Member1's profile") {
            pages.connectedUserProfilePage.apply {
                tapDevicesTab()
                assertDeviceVisible("Desktop")
            }
        }

        step("And I tap Desktop and see Member1's device is not verified") {
            pages.connectedUserProfilePage.tapDevice("Desktop")
            pages.devicesPage.assertDeviceNotVerified()
        }

        step("When I verify Member1's Desktop device") {
            pages.devicesPage.tapVerifyDeviceButton()
        }

        step("Then I see Member1's Desktop device and shield are verified") {
            pages.devicesPage.assertDeviceVerified()
            pages.connectedUserProfilePage.assertVerifiedShieldVisible()
        }

        step("And I close Member1's device details, profile and conversation") {
            pages.devicesPage.closeDevicesScreen()
            pages.connectedUserProfilePage.tapCloseButtonOnConnectedUserProfilePage()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("When I open group conversation Verification") {
            pages.conversationListPage.tapConversationNameInConversationList("Verification")
        }

        step("Then I see the group conversation is verified with the Proteus system message") {
            pages.conversationViewPage.apply {
                assertConversationVerified()
                assertSystemMessageVisible("All fingerprints are verified (Proteus)")
            }
        }

        step("When I send message Verfication worked! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Verfication worked!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Verfication worked!")
            }
        }

        step("And I close group conversation Verification") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        // TC-8117 - I should see degradation alert when I try to send a message in a degraded group conversation
        step("When User Member1 adds a new device Device3 with label Device3") {
            testServiceHelper.addDevice("user2Name", null, "Device3")
        }

        step("And I tap Member1 in the conversation list and see the 1:1 conversation in foreground") {
            pages.conversationListPage.tapConversationNameInConversationList(member1.name ?: "")
            pages.conversationViewPage.assertOneOnOneConversationInForeground(member1.name ?: "")
        }

        step("And I open the 1:1 conversation details and see Member1's profile") {
            pages.conversationViewPage.click1On1ConversationDetails(member1.name ?: "")
            pages.connectedUserProfilePage.assertConnectedUserProfileVisible(member1.name ?: "")
        }

        step("And I close Member1's profile and the conversation") {
            pages.connectedUserProfilePage.tapCloseButtonOnConnectedUserProfilePage()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I open group conversation Verification") {
            pages.conversationListPage.tapConversationNameInConversationList("Verification")
        }

        step("Then I see the conversation no longer verified system message") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "This conversation is no longer verified, as at least one participant started using a new device " +
                        "or has an invalid certificate."
            )
        }

        step("When I try to send message New device, conversation degraded!") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("New device, conversation degraded!")
                clickSendButton()
            }
        }

        step("And I see conversation no longer verified alert and tap Cancel") {
            pages.conversationViewPage.apply {
                assertConversationNoLongerVerifiedAlertVisible()
                tapCancelButton()
            }
        }

        step("Then I do not see message New device, conversation degraded! in the conversation") {
            pages.conversationViewPage.assertTextMessageNotVisibleInCurrentConversation(
                "New device, conversation degraded!"
            )
        }

        // TC-8119 - I should see degradation alert when I try to send a file to a degraded group conversation
        step("When I create a 1 KB textfile.txt and open the File picker") {
            createOneKbFileInDeviceDownloadsFolder("textfile.txt")
            pages.conversationViewPage.apply {
                iTapFileSharingButton()
                tapSharingOption("File")
            }
        }

        step("And I select textfile.txt in DocumentsUI") {
            pages.documentsUIPage.selectFileInDocumentsUI("textfile.txt")
        }

        step("And I see textfile.txt on the preview page and try to send it") {
            pages.documentsUIPage.apply {
                assertFilePreviewPageVisible("textfile.txt")
                iTapSendButtonOnPreviewImage()
            }
        }

        step("And I see conversation no longer verified alert and tap Send anyway") {
            pages.conversationViewPage.apply {
                assertConversationNoLongerVerifiedAlertVisible()
                tapSendAnywayButtonOnDegradationAlert()
            }
        }

        step("Then I see file textfile.txt in the conversation") {
            pages.conversationViewPage.assertFileWithNameIsVisible("textfile.txt")
        }
    }
}
