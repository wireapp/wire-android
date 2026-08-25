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
class FileSharingRestrictionsTests : BaseUiTest() {
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8120")
    @Category("regression", "RC", "fileSharingRestrictions")
    @Test
    fun givenTeamMember_whenFileSharingIsDisabledForTeam_thenISeeTeamSettingsChangedAlertForFirstTime() {
        step("Given There is a team owner TeamOwner with team File sharing") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "File sharing",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner adds Member1 to team File sharing with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user2Name",
                "user1Name",
                "File sharing",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And I have a 1:1 conversation with TeamOwner in team File sharing") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "File sharing"
            )
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

        step("When TeamOwner disables File Sharing for team File sharing") {
            backendSetupHelper.userDisablesFileSharingForTeam(
                "user2Name",
                "File sharing",
                backendClient
            )
        }

        step("Then I see alert informing me that my Team settings have changed") {
            pages.commonAppPage.assertTeamSettingsChangedAlertVisible()
        }

        step("And I see File Sharing disabled subtext in the Team settings change alert") {
            pages.commonAppPage.assertTeamSettingsChangedAlertSubtextVisible(
                "Sharing and receiving files of any type is now disabled"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8121", "TC-8147", "TC-8148")
    @Category("regression", "RC", "fileSharingRestrictions")
    @Test
    fun givenFileSharingIsDisabledForTeam_whenContactSendsImageVideoAndAudio_thenIShouldNotBeAbleToReceiveThem() {
        givenFileSharingRestrictedGroupIsPrepared()
        givenMemberIsLoggedInAndRestrictedGroupIsOpen()

        step("When Contact sends image testing.jpg to conversation SendFilesHere") {
            testServiceHelper.contactSendsLocalImageConversation(
                context,
                "testing.jpg",
                "user3Name",
                "Device1",
                "SendFilesHere"
            )
        }

        step("Then I see receiving of images is prohibited in conversation view") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "Receiving images is prohibited"
            )
        }

        // TC-8147 - I should not to be able to receive Video message when File sharing is disabled for team
        step("When Contact sends video testing.mp4 via Device1 to conversation SendFilesHere") {
            testServiceHelper.contactSendsLocalVideoConversation(
                context,
                "testing.mp4",
                "user3Name",
                "Device1",
                "SendFilesHere"
            )
        }

        step("And I scroll to the bottom of conversation view") {
            pages.conversationViewPage.scrollToBottomOfConversationScreen()
        }

        step("Then I see receiving of video is prohibited in conversation view") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "Receiving videos is prohibited"
            )
        }

        // TC-8148 - I should not be able to receive Audio message when File sharing is disabled for team
        step("When Contact sends audio file test.m4a via Device1 to conversation SendFilesHere") {
            testServiceHelper.contactSendsLocalAudioConversation(
                context,
                "test.m4a",
                "user3Name",
                "Device1",
                "SendFilesHere"
            )
        }

        step("And I scroll to the bottom of conversation view") {
            pages.conversationViewPage.scrollToBottomOfConversationScreen()
        }

        step("Then I see receiving of audio messages is prohibited in conversation view") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "Receiving audio messages is prohibited"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8149", "TC-8047")
    @Category("regression", "RC", "fileSharingRestrictions")
    @Test
    fun givenFileSharingIsDisabledForTeam_whenContactSendsFile_thenReceivingFilesProhibitedPlaceholderIsVisible() {
        givenFileSharingRestrictedGroupIsPrepared()
        givenMemberIsLoggedInAndRestrictedGroupIsOpen()

        step("When Contact sends 1.00MB file qa_random.txt via Device1 to conversation SendFilesHere") {
            testServiceHelper.contactSendsOneMbTextFileConversation(
                context,
                "qa_random.txt",
                "user3Name",
                "Device1",
                "SendFilesHere"
            )
        }

        step("Then I see file qa_random in the conversation view") {
            pages.conversationViewPage.assertFileWithNameIsVisible("qa_random")
        }

        step("And I see receiving of files is prohibited for file qa_random") {
            pages.conversationViewPage.assertReceivingFilesProhibitedForFileVisible("qa_random")
        }

        step("When I open file qa_random") {
            pages.conversationViewPage.clickFileWithName("qa_random")
        }

        step("Then I do not see the file download alert") {
            pages.conversationViewPage.assertFileActionModalNotVisible()
        }

        // TC-8047 - I should not see image share button when File sharing is disabled for team
        step("When I tap file sharing button") {
            pages.conversationViewPage.iTapFileSharingButton()
        }

        step("Then I do not see File, Gallery, Camera, Video or Audio sharing options") {
            pages.conversationViewPage.apply {
                assertSharingOptionNotVisible("File")
                assertSharingOptionNotVisible("Gallery")
                assertSharingOptionNotVisible("Camera")
                assertSharingOptionNotVisible("Video")
                assertSharingOptionNotVisible("Audio")
            }
        }
    }

    // Shared backend setup: prepares TeamOwner, Member1, Contact and SendFilesHere with File Sharing disabled.
    private fun givenFileSharingRestrictedGroupIsPrepared() {
        step("Given There is a team owner TeamOwner with team FileSharing") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "FileSharing",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner adds Member1 to team FileSharing with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "FileSharing",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And There is a personal user Contact") {
            clientUserManager.createPersonalUsersByAliases(listOf("user3Name"), backendClient)
        }

        step("And Contact is connected to TeamOwner and Member1") {
            backendSetupHelper.userIsConnectedTo("user3Name", "user1Name,user2Name")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And Contact adds new device Device1 with label Device1") {
            testServiceHelper.addDevice("user3Name", null, "Device1")
        }

        step("And TeamOwner has group conversation SendFilesHere with Member1 and Contact") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SendFilesHere",
                "user2Name,user3Name",
                "FileSharing"
            )
        }

        step("And TeamOwner disables File Sharing for team FileSharing") {
            backendSetupHelper.userDisablesFileSharingForTeam(
                "user1Name",
                "FileSharing",
                backendClient
            )
        }
    }

    // Shared app flow: logs in Member1 through staging and opens the restricted SendFilesHere conversation.
    private fun givenMemberIsLoggedInAndRestrictedGroupIsOpen() {
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

        step("And I see conversation SendFilesHere in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("SendFilesHere")
        }

        step("And I open conversation SendFilesHere") {
            pages.conversationListPage.clickGroupConversation("SendFilesHere")
        }
    }
}
