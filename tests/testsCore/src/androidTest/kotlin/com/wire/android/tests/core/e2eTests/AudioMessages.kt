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

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.PermissionUtils.grantRuntimePermsForForegroundApp
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class AudioMessages : BaseUiTest() {
    private var member1: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        grantRuntimePermsForForegroundApp(device, Manifest.permission.RECORD_AUDIO)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4081")
    @Category("audioMessages", "regression", "RC")
    @Test
    fun givenUserReceivesAudioFile_whenPlayingAndPausingAudioMessage_thenAudioPlaybackTimeIsUpdated() {
        step("Given There is a team owner with team Audio") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Audio",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Audio with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Audio",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("And User TeamOwner has group conversation SendYourAudioHere with Member1 in team Audio") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SendYourAudioHere",
                "user2Name",
                "Audio"
            )
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
                enterTeamMemberLoggingEmail(member1?.email ?: "")
                clickLoginButton()
                enterTeamMemberLoggingPassword(member1?.password ?: "")
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

        step("And I see conversation SendYourAudioHere in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SendYourAudioHere")
            }
        }

        step("And I tap on conversation name SendYourAudioHere in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("SendYourAudioHere")
            }
        }

        step("When User TeamOwner sends local audio file named test.m4a via device Device1 to group conversation SendYourAudioHere") {
            testServiceHelper.contactSendsLocalAudioConversation(
                context,
                "test.m4a",
                "user1Name",
                "Device1",
                "SendYourAudioHere"
            )
        }

        step("Then I see an audio file in the conversation view") {
            pages.conversationViewPage.apply {
                assertAudioMessageIsVisible()
            }
        }

        step("And I see the time played in the audio file is 00:00") {
            pages.conversationViewPage.apply {
                assertAudioTimeStartsAtZero()
            }
        }

        step("When I tap play button on the audio file") {
            pages.conversationViewPage.apply {
                clickPlayButtonOnAudioMessage()
            }
        }

        step("And I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("And I tap pause button on the audio file") {
            pages.conversationViewPage.apply {
                clickPauseButtonOnAudioMessage()
            }
        }

        step("Then I see the time played in the audio file is not 00:00") {
            pages.conversationViewPage.apply {
                assertAudioTimeIsNotZeroAnymore()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4082")
    @Category("audioMessages", "regression", "RC")
    @Test
    fun givenUserRecordsAudioMessage_whenSendingRecordedAudioMessage_thenAudioFileIsVisibleInConversation() {
        step("Given There is a team owner with team Audio") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Audio",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Audio with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Audio",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("And User TeamOwner has group conversation SendYourAudioHere with Member1 in team Audio") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SendYourAudioHere",
                "user2Name",
                "Audio"
            )
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
                enterTeamMemberLoggingEmail(member1?.email ?: "")
                clickLoginButton()
                enterTeamMemberLoggingPassword(member1?.password ?: "")
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

        step("And I see conversation SendYourAudioHere in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SendYourAudioHere")
            }
        }

        step("And I tap on conversation name SendYourAudioHere in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("SendYourAudioHere")
            }
        }

        step("When I tap file sharing button") {
            pages.conversationViewPage.apply {
                iTapFileSharingButton()
            }
        }

        step("And I tap on Attach Audio option") {
            pages.conversationViewPage.apply {
                tapSharingOption("Audio")
            }
        }

        step("And I tap on start recording audio button") {
            pages.conversationViewPage.apply {
                tapRecordAudioButton()
            }
        }

        step("And I wait for 5 seconds") {
            UiWaitUtils.waitFor(5.seconds)
        }

        step("And I tap on stop recording audio button") {
            pages.conversationViewPage.apply {
                tapStopRecordingAudioButton()
            }
        }

        step("Then I see that my audio message was recorded") {
            pages.conversationViewPage.apply {
                assertAudioMessageWasRecorded()
            }
        }

        step("When I send my recorded audio message") {
            pages.conversationViewPage.apply {
                sendRecordedAudioMessage()
            }
        }

        step("And I tap file sharing button") {
            pages.conversationViewPage.apply {
                iTapFileSharingButton()
            }
        }

        step("Then I see an audio file in the conversation view") {
            pages.conversationViewPage.apply {
                assertAudioMessageIsVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4083", "TC-4084")
    @Category("audioMessages", "regression", "RC")
    @Test
    fun givenUserRecordsLongAudioMessage_whenPlayingAndApplyingFilterBeforeSending_thenAudioFileIsVisibleInConversation() {
        step("Given There is a team owner with team Audio") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Audio",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Audio with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Audio",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("And User TeamOwner has group conversation SendYourAudioHere with Member1 in team Audio") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SendYourAudioHere",
                "user2Name",
                "Audio"
            )
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
                enterTeamMemberLoggingEmail(member1?.email ?: "")
                clickLoginButton()
                enterTeamMemberLoggingPassword(member1?.password ?: "")
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

        step("And I see conversation SendYourAudioHere in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SendYourAudioHere")
            }
        }

        step("And I tap on conversation name SendYourAudioHere in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("SendYourAudioHere")
            }
        }

        step("When I tap file sharing button") {
            pages.conversationViewPage.apply {
                iTapFileSharingButton()
            }
        }

        step("And I tap on Attach Audio option") {
            pages.conversationViewPage.apply {
                tapSharingOption("Audio")
            }
        }

        step("And I tap on start recording audio button") {
            pages.conversationViewPage.apply {
                tapRecordAudioButton()
            }
        }

        step("And I wait for 20 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("And I tap on stop recording audio button") {
            pages.conversationViewPage.apply {
                tapStopRecordingAudioButton()
            }
        }

        step("Then I see that my audio message was recorded") {
            pages.conversationViewPage.apply {
                assertAudioMessageWasRecorded()
            }
        }

        step("When I tap on play button on recorded audio message") {
            pages.conversationViewPage.apply {
                clickPlayButtonOnAudioMessage()
            }
        }

        step("And I wait for 3 seconds") {
            UiWaitUtils.waitFor(3.seconds)
        }

        step("Then I see the time played in the audio file is not 00:00") {
            pages.conversationViewPage.apply {
                assertAudioTimeIsNotZeroAnymore()
            }
        }

        step("And I tap on pause button on recorded audio message") {
            pages.conversationViewPage.apply {
                clickPauseButtonOnAudioMessage()
            }
        }

        // TC-4084 - I want to apply an audio filter to my audio message before I send it

        step("When I tap on apply audio filter checkbox") {
            pages.conversationViewPage.apply {
                tapApplyAudioFilterCheckbox()
            }
        }

        step("Then I see audio filter is applied") {
            pages.conversationViewPage.apply {
                assertAudioFilterIsApplied()
            }
        }

        step("And I send my recorded audio message") {
            pages.conversationViewPage.apply {
                sendRecordedAudioMessage()
            }
        }

        step("And I tap file sharing button") {
            pages.conversationViewPage.apply {
                iTapFileSharingButton()
            }
        }

        step("And I see an audio file in the conversation view") {
            pages.conversationViewPage.apply {
                assertAudioMessageIsVisible()
            }
        }
    }
}
