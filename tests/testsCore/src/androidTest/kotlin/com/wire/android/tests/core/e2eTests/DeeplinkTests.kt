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

@RunWith(AndroidJUnit4::class)
class DeeplinkTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12802", "TC-12805")
    @Category("regression", "RC", "userProfileLinks")
    @Test
    fun givenTeamOwnerHasProfileQrCode_whenMemberOpensReceivedProfileLink_thenTeamOwnerProfileIsDisplayed() {
        step("Given There is a team owner TeamOwner with team ProfileLinks") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ProfileLinks",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner configures MLS for ProfileLinks") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "ProfileLinks", backendClient)
        }

        step("And TeamOwner adds Member1 to team ProfileLinks with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ProfileLinks",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
            }
        }

        step("And TeamOwner creates MLS group Conversation ProfileLinks with Member1") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "ProfileLinks",
                deviceName = "Device1"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And TeamOwner signs in to staging and completes the login prompts") {
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(teamOwner)
        }

        // TC-12802: verify the displayed QR code encodes TeamOwner's qualified user ID.
        step("When TeamOwner opens User Profile and taps Share Profile Link") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapShareProfileLinkButton()
            }
        }

        lateinit var profileLink: String
        step("Then Share Profile shows TeamOwner's username, profile link and matching QR code") {
            profileLink = pages.profileQrCodePage.assertProfileQrCodeAndReadLink(
                requireNotNull(teamOwner.id),
                requireNotNull(teamOwner.uniqueUsername)
            )
        }

        step("And TeamOwner returns to User Profile and logs out with cleared data") {
            pages.profileQrCodePage.tapBackButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                logoutWithClearedData()
            }
        }

        step("And User Member1 is me") {
            clientUserManager.setSelfUser(member1)
        }

        step("And Member1 signs in to staging and completes the login prompts") {
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }

        step("And Member1 opens Conversation ProfileLinks") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("ProfileLinks")
                tapConversationNameInConversationList("ProfileLinks")
            }
        }

        // TC-12805: open the actual profile link received in the conversation.
        step("When TeamOwner sends their profile link to Conversation ProfileLinks") {
            testServiceHelper.userSendsGenericMessageToConversation(
                "user1Name",
                "ProfileLinks",
                "Device1",
                profileLink
            )
        }

        step("And Member1 sees and taps TeamOwner's received profile link") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation(profileLink)
                tapLinkInCurrentConversation(profileLink)
            }
        }

        step("And Member1 confirms Open on the Visit Link dialog for TeamOwner's profile link") {
            pages.conversationViewPage.apply {
                assertLinkOpeningAlertVisible(profileLink)
                tapOpenButtonOnLinkAlert()
            }
        }

        step("And Member1 taps Open in App on the profile link webpage") {
            pages.chromePage.apply {
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
                tapOpenInAppOnProfileLinkPage()
            }
        }

        step("Then Member1 sees TeamOwner's profile with the correct name and username") {
            pages.connectedUserProfilePage.assertConnectedUserProfileVisible(requireNotNull(teamOwner.name))
            pages.connectedUserProfilePage.assertProfileUsername(requireNotNull(teamOwner.uniqueUsername))
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12803", "TC-12804")
    @Category("regression", "RC", "userProfileLinks")
    @Test
    fun givenTeamOwnerHasProfileQrCode_whenSharingProfileLinkAndQrImage_thenReceivingAppGetsCorrectProfileContent() {
        step("Given There is a team owner TeamOwner with team ProfileSharing") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ProfileSharing",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner configures MLS for ProfileSharing") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "ProfileSharing", backendClient)
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And TeamOwner signs in to staging and completes the login prompts") {
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(teamOwner)
        }

        step("And TeamOwner opens User Profile and taps Share Profile Link") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapShareProfileLinkButton()
            }
        }

        lateinit var profileLink: String
        step("And Share Profile shows TeamOwner's username, profile link and matching QR code") {
            profileLink = pages.profileQrCodePage.assertProfileQrCodeAndReadLink(
                requireNotNull(teamOwner.id),
                requireNotNull(teamOwner.uniqueUsername)
            )
        }

        // TC-12804: another app receives the exact profile URL through Android's share sheet.
        step("When TeamOwner taps Share Profile Link and selects Profile Test Receiver") {
            pages.profileQrCodePage.tapShareProfileLinkButton()
            pages.profileShareReceiverPage.selectFromShareSheet()
        }

        step("Then Profile Test Receiver reads TeamOwner's exact profile link") {
            pages.profileShareReceiverPage.assertReceivedProfileLink(profileLink)
        }

        step("And TeamOwner returns from Profile Test Receiver to Share Profile") {
            pages.profileShareReceiverPage.returnToShareProfile()
        }

        // TC-12803: another app reads the shared image and decodes TeamOwner's profile QR code.
        step("When TeamOwner taps Share QR Code and selects Profile Test Receiver") {
            pages.profileQrCodePage.tapShareQrCodeButton()
            pages.profileShareReceiverPage.selectFromShareSheet()
        }

        step("Then Profile Test Receiver reads the QR image for TeamOwner's profile") {
            pages.profileShareReceiverPage.assertReceivedProfileQrCode(profileLink)
        }

        step("And TeamOwner returns from Profile Test Receiver to Share Profile") {
            pages.profileShareReceiverPage.returnToShareProfile()
        }
    }

    // Shared staging login flow, including post-login prompts and the service notification wait.
    private fun loginToStagingAs(user: ClientUser) {
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
            clickContinueButtonOnBackendConfigSuccess()
            enterUserIdentifier(user.email.orEmpty())
            clickLoginButton()
            enterUserPassword(user.password.orEmpty())
            clickLoginButton()
        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsCompleted()
            clickAllowNotificationButton()
            clickDeclineShareDataAlert()
        }
        pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
    }
}
