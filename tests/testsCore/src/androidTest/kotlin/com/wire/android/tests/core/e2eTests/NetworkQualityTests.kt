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
import com.wire.android.tests.core.BaseCallUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.SHORT_TIMEOUT
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class NetworkQualityTests : BaseCallUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        initCallTestHelpers()
    }

    @TestCaseId("TC-9705", "TC-9706", "TC-9708")
    @Category("regression", "networkQuality", "RC")
    @Test
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun givenIAmInOneOnOneCall_whenIOpenNetworkQuality_thenNetworkQualityDetailsAreVisible() {
        givenTeamOwnerMemberAndOneOnOneConversationArePrepared()

        step("And Member starts Chrome calling instance") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("When User Member calls me") {
            runBlocking {
                callingManager.callConversation("user2Name", "user1Name")
            }
        }

        step("Then I see 1:1 incoming call notification and open it to bring call to foreground") {
            pages.notificationsPage.apply {
                iSeeOneOnOneIncomingCallNotification(member.name ?: "")
                iOpenCallNotificationToBringCallToForeground()
            }
        }

        step("And I accept the call and see member in ongoing 1:1 call") {
            pages.callingPage.apply {
                iAcceptCall()
                iSeeParticipantInOngoingOneOnOneCall(member.name ?: "")
            }
        }

        step("And Member verifies to send and receive audio") {
            runBlocking {
                callingManager.verifySendAndReceiveAudio(
                    clientUserManager.splitAliases("user2Name")
                )
            }
        }

        step("When I turn camera on") {
            pages.callingPage.apply {
                iTurnCameraOn()
            }
        }

        step("And Member switches video on") {
            runBlocking {
                callingManager.switchVideoOn(
                    clientUserManager.splitAliases("user2Name")
                )
            }
        }

        step("Then I see member video is enabled") {
            callHelper.iSeeVideoForUsersEnabled("user2Name")
        }

        step("When I open call details and see encrypted call details") {
            pages.callingPage.apply {
                iOpenCallDetails()
                iSeeCallDetails()
            }
        }

        step("And I open Network quality details") {
            pages.callingPage.apply {
                iOpenNetworkQualityDetails()
            }
        }
        // TC-9708 Verify behaviour for the poor threshold showing marker in the Quality handler.

        step("Then I see Network quality dashboard values") {
            pages.callingPage.apply {
                iSeeNetworkQualityDetails()
                iSeeNetworkQualityMetrics()
                iSeeNetworkQualityLearnMoreButton()
            }
        }

        step("When I tap back button on Network quality dashboard and see call details") {
            pages.callingPage.apply {
                iTapBackButtonOnNetworkQualityDetails()
                iSeeCallDetails()
            }
        }

        // TC-9706 Verify behaviour when the network quality dashboard is closed by clicking into call or swiping down dashboard.

        step("And I close call details") {
            pages.callingPage.apply {
                iCloseCallDetailsByTappingCallArea()
            }
        }

        step("When I tap hang up button") {
            pages.callingPage.apply {
                iTapOnHangUpButton()
            }
        }

        step("Then I do not see ongoing 1:1 call") {
            pages.callingPage.apply {
                iDoNotSeeOngoingOneOnOneCall()
            }
        }
    }

    @TestCaseId("TC-9718")
    @Category("regression", "networkQuality", "RC")
    @Test
    fun givenNetworkQualityDetailsAreOpen_whenIOpenLearnMore_thenSupportArticleIsShown() {
        givenTeamOwnerMemberAndOneOnOneConversationArePrepared()

        step("And Member starts Chrome calling instance") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("When User Member calls me") {
            runBlocking {
                callingManager.callConversation("user2Name", "user1Name")
            }
        }

        step("Then I see 1:1 incoming call notification and open it to bring call to foreground") {
            pages.notificationsPage.apply {
                iSeeOneOnOneIncomingCallNotification(member.name ?: "")
                iOpenCallNotificationToBringCallToForeground()
            }
        }

        step("And I accept the call and see member in ongoing 1:1 call") {
            pages.callingPage.apply {
                iAcceptCall()
                iSeeParticipantInOngoingOneOnOneCall(member.name ?: "")
            }
        }

        step("And Member verifies to send and receive audio") {
            runBlocking {
                callingManager.verifySendAndReceiveAudio(
                    clientUserManager.splitAliases("user2Name")
                )
            }
        }

        step("When I open call details and Network quality details") {
            pages.callingPage.apply {
                iOpenCallDetails()
                iOpenNetworkQualityDetails()
            }
        }

        step("And I tap Learn more about quality details") {
            pages.callingPage.apply {
                iTapLearnMoreAboutQualityDetails()
            }
        }

        step("Then I see Check network quality support article") {
            pages.chromePage.apply {
                assertNetworkQualitySupportArticleVisible()
            }
        }
    }

    @TestCaseId("TC-9720")
    @Category("regression", "networkQuality", "RC")
    @Test
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun givenOneOnOneVideoCall_whenITurnOffOtherVideos_thenMemberVideoIsDisabled() {
        givenTeamOwnerMemberAndOneOnOneConversationArePrepared()

        step("And Member starts Chrome calling instance") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("When User Member calls me") {
            runBlocking {
                callingManager.callConversation("user2Name", "user1Name")
            }
        }

        step("Then I see 1:1 incoming call notification and open it to bring call to foreground") {
            pages.notificationsPage.apply {
                iSeeOneOnOneIncomingCallNotification(member.name ?: "")
                iOpenCallNotificationToBringCallToForeground()
            }
        }

        step("And I accept the call and see member in ongoing 1:1 call") {
            pages.callingPage.apply {
                iAcceptCall()
                iSeeParticipantInOngoingOneOnOneCall(member.name ?: "")
            }
        }

        step("And Member verifies to send and receive audio") {
            runBlocking {
                callingManager.verifySendAndReceiveAudio(
                    clientUserManager.splitAliases("user2Name")
                )
            }
        }

        step("When I turn camera on") {
            pages.callingPage.apply {
                iTurnCameraOn()
            }
        }

        step("And Member switches video on") {
            runBlocking {
                callingManager.switchVideoOn(
                    clientUserManager.splitAliases("user2Name")
                )
            }
        }

        step("Then I see Member video QR code in call tile") {
            UiWaitUtils.waitFor(SHORT_TIMEOUT) // wait for video field to ON
            pages.callingPage.apply {
                iSeeQrCodeContaining(member.name ?: "", member.email ?: "")
            }
        }

        step("When I open and see call details") {
            pages.callingPage.apply {
                iOpenCallDetails()
                iSeeCallDetails()
            }
        }

        step("And I turn off other participant video field and close call details") {
            pages.callingPage.apply {
                iTurnOffOtherParticipantVideoField()
                iCloseCallDetailsByTappingCallArea()
            }
        }

        step("Then I do not see Member video QR code in call tile") {
            pages.callingPage.apply {
                iDoNotSeeQrCodeContaining(member.name ?: "", member.email ?: "")
            }
        }
    }

    // Shared backend setup for 1:1 call tests: creates TeamOwner, adds Member,
    // and creates their 1:1 conversation.
    private fun givenTeamOwnerMemberAndOneOnOneConversationArePrepared() {
        step("Given team owner, member, and 1:1 conversation are prepared via backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "NetworkQuality",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserBy(
                "user1Name",
                ClientUserManager.FindBy.NAME_ALIAS
            )

            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "NetworkQuality",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member = clientUserManager.findUserBy(
                "user2Name",
                ClientUserManager.FindBy.NAME_ALIAS
            )

            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "NetworkQuality"
            )
        }
    }

    // Shared app login flow: opens staging, signs in as TeamOwner, and clears post-login prompts.
    private fun givenILoginAsTeamOwnerThroughStagingDeepLink() {
        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
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
    }
}
