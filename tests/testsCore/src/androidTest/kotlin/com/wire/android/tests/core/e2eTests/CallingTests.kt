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
import uiautomatorutils.UiWaitUtils.MEDIUM_TIMEOUT
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
class CallingTests : BaseCallUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        initCallTestHelpers()
    }

    // Shared backend setup for 1:1 call tests: creates TeamOwner, adds Member to the team, and creates their 1:1 conversation.
    private fun givenTeamOwnerMemberAndOneOnOneConversationArePrepared() {
        step("Given team owner, member, and 1:1 conversation are prepared via backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "WeLikeCalls",
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
                "WeLikeCalls",
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
                "WeLikeCalls"
            )
        }
    }

    // Shared backend setup for group call tests: creates TeamOwner, members, enables conference calling,
    // and creates the group conversation.
    private fun givenTeamOwnerMembersAndGroupConversationArePrepared() {
        step("Given team owner is prepared via backend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "WeLikeCalls",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserBy(
                "user1Name",
                ClientUserManager.FindBy.NAME_ALIAS
            )
        }

        andTeamOwnerAddsMembersToTeam()
        andTeamOwnerEnablesConferenceCallingFeature()

        step("And TeamOwner has group conversation GroupCall with Member1 and Member2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "GroupCall",
                "user2Name,user3Name",
                "WeLikeCalls"
            )
        }
    }

    private fun andTeamOwnerEnablesConferenceCallingFeature() {
        step("And TeamOwner enables conference calling feature via backdoor") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "WeLikeCalls"
                )
            }
        }
    }

    private fun andTeamOwnerAddsMembersToTeam() {
        step("And TeamOwner adds Member1 and Member2 to team") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "WeLikeCalls",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }
    }

    // Shared app login flow: opens the staging deep link, signs in as TeamOwner, and clears post-login prompts.
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

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4258", "TC-4268")
    @Category("regression", "calling", "RC", "smoke", "smokeSchwarz",)
    @Test
    fun givenMemberCallsMeInForeground_whenIAccept_thenOneOnOneCallIsEstablished() {
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

        step("Then I see 1:1 incoming call notification") {
            pages.notificationsPage.apply {
                iSeeOneOnOneIncomingCallNotification(member.name ?: "")
            }
        }

        step("When I open the call notification to bring call to foreground") {
            pages.notificationsPage.apply {
                iOpenCallNotificationToBringCallToForeground()
            }
        }

        step("And I accept the call") {
            pages.callingPage.apply {
                iAcceptCall()
            }
        }

        step("Then I see member in ongoing 1:1 call") {
            pages.callingPage.apply {
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

//        TC-4268 I want to be able to maximize and minimize tiles by double tapping on

        step("When I double tap my call tile to maximize it") {
            pages.callingPage.apply {
                iDoubleTapToMaximizeCallTile(teamOwner.name ?: "")
            }
        }

        step("Then I do not see member tile in ongoing 1:1 call") {
            pages.callingPage.apply {
                iDoNotSeeUserInCallTile(member.name ?: "")
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

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4262")
    @Category("regression", "calling", "RC")
    @Test
    fun givenMemberCallsMeInForeground_whenITurnCameraOnBeforeAccepting_thenOneOnOneVideoCallIsEstablished() {
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

        step("Then I see 1:1 incoming call notification") {
            pages.notificationsPage.apply {
                iSeeOneOnOneIncomingCallNotification(member.name ?: "")
            }
        }

        step("When I open the call notification to bring call to foreground") {
            pages.notificationsPage.apply {
                iOpenCallNotificationToBringCallToForeground()
            }
        }

        step("And I turn camera on") {
            pages.callingPage.apply {
                iTurnCameraOn()
            }
        }

        step("And I accept the call") {
            pages.callingPage.apply {
                iAcceptCall()
            }
        }

        step("Then I see member in ongoing 1:1 call") {
            pages.callingPage.apply {
                iSeeParticipantInOngoingOneOnOneCall(member.name ?: "")
            }
        }

        step("When User Member switches video on") {
            val switchedVideoOn = UiWaitUtils.retryUntilTimeout(
                timeout = MEDIUM_TIMEOUT,
                pollingInterval = UiWaitUtils.POLLING_DEFAULT
            ) {
                runCatching {
                    runBlocking {
                        val callParticipantsSwitchVideoOn = clientUserManager.splitAliases("user2Name")
                        callingManager.switchVideoOn(callParticipantsSwitchVideoOn)
                    }
                }.isSuccess
            }

            if (!switchedVideoOn) {
                throw AssertionError("Member could not switch video on")
            }

            step("Then Member verifies to send and receive audio and video") {
                runBlocking {
                    val callParticipantsSendAndReceiveAudioAndVideo = clientUserManager.splitAliases("user2Name")
                    callingManager.verifySendAndReceiveAudioAndVideo(callParticipantsSendAndReceiveAudioAndVideo)
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
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4261")
    @Category("regression", "calling", "smoke")
    @Test
    fun givenMemberCallsMeWithAppInBackground_whenIAccept_thenOneOnOneCallIsEstablished() {
        givenTeamOwnerMemberAndOneOnOneConversationArePrepared()

        step("And Member starts Chrome calling instance") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("When I minimise Wire") {
            device.pressHome()
        }

        step("And User Member calls me") {
            runBlocking {
                callingManager.callConversation("user2Name", "user1Name")
            }
        }

        step("Then I see 1:1 incoming call notification") {
            pages.notificationsPage.apply {
                iSeeOneOnOneIncomingCallNotification(member.name ?: "")
            }
        }

        step("When I open the call notification to bring call to foreground") {
            pages.notificationsPage.apply {
                iOpenCallNotificationToBringCallToForeground()
            }
        }

        step("And I accept the call") {
            pages.callingPage.apply {
                iAcceptCall()
            }
        }

        step("Then I see member in ongoing 1:1 call") {
            pages.callingPage.apply {
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

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4259")
    @Category("regression", "RC", "calling")
    @Test
    fun givenIAmATeamOwner_whenIStartOneOnOneCallWithMemberOfMyTeam_thenOneOnOneCallIsEstablished() {
        givenTeamOwnerMemberAndOneOnOneConversationArePrepared()

        step("And Member starts Chrome calling instance") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see conversation Member in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(member.name ?: "")
            }
        }

        step("And I tap on conversation name Member in conversation list") {
            pages.conversationListPage.apply {
                tapConversationNameInConversationList(member.name ?: "")
            }
        }

        step("And Member accepts next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name")
            }
        }

        step("And I wait until Wire service notification disappears") {
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }

        step("When I tap start call button") {
            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }
        }

        step("Then Member verifies that waiting instance status is changed to active in 30 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user2Name",
                    "active",
                    30
                )
            }
        }

        step("And I see member in ongoing 1:1 call") {
            pages.callingPage.apply {
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

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4260")
    @Category("regression", "RC", "calling")
    @Test
    fun givenIAmInOneOnOneCallWithMemberOfMyTeam_whenIEnableVideo_thenOneOnOneVideoCallIsEstablished() {
        givenTeamOwnerMemberAndOneOnOneConversationArePrepared()

        step("And Member starts Chrome calling instance") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see conversation Member in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(member.name ?: "")
            }
        }

        step("And I tap on conversation name Member in conversation list") {
            pages.conversationListPage.apply {
                tapConversationNameInConversationList(member.name ?: "")
            }
        }

        step("And Member accepts next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name")
            }
        }

        step("And I wait until Wire service notification disappears") {
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }

        step("When I tap start call button") {
            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }
        }

        step("Then Member verifies that waiting instance status is changed to active in 30 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user2Name",
                    "active",
                    30
                )
            }
        }

        step("And I see member in ongoing 1:1 call") {
            pages.callingPage.apply {
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

        step("And User Member switches video on") {
            runBlocking {
                val callParticipantsSwitchVideoOn = clientUserManager.splitAliases("user2Name")
                callingManager.switchVideoOn(callParticipantsSwitchVideoOn)
            }
        }

        step("Then Member verifies to send and receive audio and video") {
            runBlocking {
                val callParticipantsSendAndReceiveAudioAndVideo = clientUserManager.splitAliases("user2Name")
                callingManager.verifySendAndReceiveAudioAndVideo(callParticipantsSendAndReceiveAudioAndVideo)
            }
        }

        step("And I see user Member in ongoing 1:1 video call") {
            pages.callingPage.apply {
                iSeeParticipantInOngoingOneOnOneVideoCall(member.name ?: "")
            }
        }

        step("And I see QR codes containing Member email in video grid") {
            pages.callingPage.apply {
                iSeeQrCodeContaining(clientUserManager.findUserByEmailOrEmailAlias("user2Email").email ?: "")
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

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4263")
    @Category("regression", "RC", "calling", "groupCalling", "smoke", "smokeSchwarz", "smokeSTACKIT")
    @Test
    fun givenAppIsInForeground_whenIAcceptIncomingGroupCall_thenGroupCallIsEstablished() {
        givenTeamOwnerMembersAndGroupConversationArePrepared()

        step("And Member1 and Member2 start Chrome calling instances") {
            runBlocking {
                callHelper.userXStartsInstance(
                    "user2Name, user3Name",
                    "Chrome"
                )
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see conversation GroupCall in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupCall")
            }
        }

        step("And Member2 accepts next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user3Name")
            }
        }

        step("When User Member1 calls GroupCall") {
            runBlocking {
                callingManager.callGroupConversation("user2Name", "GroupCall")
            }
        }

        step("Then I see incoming group call from group GroupCall") {
            pages.notificationsPage.apply {
                iSeeIncomingGroupCallNotification("GroupCall")
            }
        }

        step("When I open the call notification to bring call to foreground") {
            pages.notificationsPage.apply {
                iOpenCallNotificationToBringCallToForeground()
            }
        }

        step("When I accept the call and see ongoing group call") {
            pages.callingPage.apply {
                iAcceptCall()
                iSeeOngoingGroupCall()
            }
        }

        step("And I see users Member1 and Member2 in ongoing group call") {
            callHelper.iSeeParticipantsInGroupCall("user2Name,user3Name")
        }

        step("When I unmute myself") {
            pages.callingPage.apply {
                iUnmuteMyself()
            }
        }

        step("And Member2 unmutes their microphone") {
            runBlocking {
                callingManager.unmuteMicrophone(
                    clientUserManager.splitAliases("user3Name")
                )
            }
        }

        step("Then Member1 and Member2 verify to receive audio") {
            runBlocking {
                callingManager.verifyReceiveAudio(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("When I tap hang up button and do not see ongoing group call") {
            pages.callingPage.apply {
                iTapOnHangUpButton()
                iDoNotSeeOngoingGroupCall()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4269")
    @Category("regression", "calling", "groupCalling", "RC")
    @Test
    fun givenAppIsInForeground_whenIEnableVideoBeforeAcceptingIncomingGroupCall_thenGroupCallIsEstablished() {
        givenTeamOwnerMembersAndGroupConversationArePrepared()

        step("And Member1 and Member2 start Chrome calling instances") {
            runBlocking {
                callHelper.userXStartsInstance(
                    "user2Name, user3Name",
                    "Chrome"
                )
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see conversation GroupCall in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupCall")
            }
        }

        step("And Member2 accepts next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user3Name")
            }
        }

        step("When User Member1 calls GroupCall") {
            runBlocking {
                callingManager.callGroupConversation("user2Name", "GroupCall")
            }
        }

        step("And I see incoming group call from group GroupCall") {
            pages.notificationsPage.apply {
                iSeeIncomingGroupCallNotification("GroupCall")
            }
        }

        step("And I open the call notification to bring call to foreground") {
            pages.notificationsPage.apply {
                iOpenCallNotificationToBringCallToForeground()
            }
        }

        step("And I turn camera on") {
            pages.callingPage.apply {
                iTurnCameraOn()
            }
        }

        step("And I accept the call and see ongoing group call") {
            pages.callingPage.apply {
                iAcceptCall()
                iSeeOngoingGroupCall()
            }
        }

        step("And I see users Member1 and Member2 in ongoing group call") {
            callHelper.iSeeParticipantsInGroupCall("user2Name,user3Name")
        }

        step("When I unmute myself") {
            pages.callingPage.apply {
                iUnmuteMyself()
            }
        }

        step("And Member2 unmutes their microphone") {
            runBlocking {
                callingManager.unmuteMicrophone(
                    clientUserManager.splitAliases("user3Name")
                )
            }
        }

        step("And Member1 and Member2 switch video on") {
            runBlocking {
                callingManager.switchVideoOn(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("Then Member1 and Member2 verify to send and receive audio") {
            runBlocking {
                callingManager.verifySendAndReceiveAudio(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("When I tap hang up button and do not see ongoing group call") {
            pages.callingPage.apply {
                iTapOnHangUpButton()
                iDoNotSeeOngoingGroupCall()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4266")
    @Category("regression", "calling", "groupCalling", "smoke")
    @Test
    fun givenAppIsInBackground_whenIAcceptIncomingGroupCall_thenGroupCallIsEstablished() {
        givenTeamOwnerMembersAndGroupConversationArePrepared()

        step("And Member1 and Member2 start Chrome calling instances") {
            runBlocking {
                callHelper.userXStartsInstance(
                    "user2Name, user3Name",
                    "Chrome"
                )
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see conversation GroupCall in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupCall")
            }
        }

        step("And Member2 accepts next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user3Name")
            }
        }

        step("When I minimise Wire") {
            device.pressHome()
        }

        step("And User Member1 calls GroupCall") {
            runBlocking {
                callingManager.callGroupConversation("user2Name", "GroupCall")
            }
        }

        step("Then I see incoming group call from group GroupCall") {
            pages.notificationsPage.apply {
                iSeeIncomingGroupCallNotification("GroupCall")
            }
        }

        step("When I open the call notification to bring call to foreground") {
            pages.notificationsPage.apply {
                iOpenCallNotificationToBringCallToForeground()
            }
        }

        step("When I accept the call and see ongoing group call") {
            pages.callingPage.apply {
                iAcceptCall()
                iSeeOngoingGroupCall()
            }
        }

        step("And I see users Member1 and Member2 in ongoing group call") {
            callHelper.iSeeParticipantsInGroupCall("user2Name,user3Name")
        }

        step("When I unmute myself") {
            pages.callingPage.apply {
                iUnmuteMyself()
            }
        }

        step("Then Member1 and Member2 verify to send and receive audio") {
            runBlocking {
                callingManager.verifySendAndReceiveAudio(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("When I tap hang up button and do not see ongoing group call") {
            pages.callingPage.apply {
                iTapOnHangUpButton()
                iDoNotSeeOngoingGroupCall()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4264")
    @Category("regression", "RC", "calling", "groupCalling")
    @Test
    fun givenIAmATeamOwner_whenIStartGroupCallWithMembersOfMyTeam_thenGroupCallIsEstablishedAndJoinButtonIsVisible() {
        givenTeamOwnerMembersAndGroupConversationArePrepared()

        step("And Member1 and Member2 start Chrome calling instances") {
            runBlocking {
                callHelper.userXStartsInstance(
                    "user2Name, user3Name",
                    "Chrome"
                )
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see conversation GroupCall in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupCall")
            }
        }

        step("And I tap on conversation name GroupCall in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("GroupCall")
            }
        }

        step("And Member1 and Member2 accept next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name,user3Name")
            }
        }

        step("When I tap start call button") {
            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }
        }

        step("And Member1 and Member2 verify waiting instance status changes to active within 90 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user2Name,user3Name",
                    "active",
                    90
                )
            }
        }

        step("Then I see ongoing group call") {
            pages.callingPage.apply {
                iSeeOngoingGroupCall()
            }
        }

        step("And I see users Member1 and Member2 in ongoing group call") {
            callHelper.iSeeParticipantsInGroupCall("user2Name,user3Name")
        }

        step("And Member1 and Member2 verify to send and receive audio") {
            runBlocking {
                callingManager.verifySendAndReceiveAudio(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("When I tap hang up button and do not see ongoing group call") {
            pages.callingPage.apply {
                iTapOnHangUpButton()
                iDoNotSeeOngoingGroupCall()
            }
        }

        step("And I see join button in group conversation view") {
            pages.callingPage.apply {
                iSeeJoinButtonInGroupConversationView()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4265")
    @Category("regression", "RC", "calling", "groupCalling", "smoke", "smokeSchwarz")
    @Test
    fun givenIAmInGroupCall_whenIEnableVideo_thenGroupVideoIsVisible() {
        givenTeamOwnerMembersAndGroupConversationArePrepared()

        step("And Member1 and Member2 start Chrome calling instances") {
            runBlocking {
                callHelper.userXStartsInstance(
                    "user2Name, user3Name",
                    "Chrome"
                )
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see conversation GroupCall in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupCall")
            }
        }

        step("And I tap on conversation name GroupCall in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("GroupCall")
            }
        }

        step("And Member1 and Member2 accept next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name,user3Name")
            }
        }

        step("And I wait until Wire service notification disappears") {
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }

        step("When I tap start call button") {
            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }
        }

        step("And Member1 and Member2 verify waiting instance status changes to active within 90 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user2Name,user3Name",
                    "active",
                    90
                )
            }
        }

        step("Then I see ongoing group call") {
            pages.callingPage.apply {
                iSeeOngoingGroupCall()
            }
        }

        step("And I see users Member1 and Member2 in ongoing group call") {
            callHelper.iSeeParticipantsInGroupCall("user2Name,user3Name")
        }

        step("And Member1 and Member2 verify to receive audio") {
            runBlocking {
                callingManager.verifyReceiveAudio(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("When I turn camera on") {
            pages.callingPage.apply {
                iTurnCameraOn()
            }
        }

        step("And Member1 and Member2 switch video on") {
            runBlocking {
                callingManager.switchVideoOn(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("And Member1 and Member2 verify to receive audio and video") {
            runBlocking {
                callingManager.verifyReceiveAudioAndVideo(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("Then I see users Member1 and Member2 in ongoing group video call") {
            callHelper.iSeeParticipantsInGroupVideoCall("user2Name,user3Name")
        }

        step("And I see video for users Member1 and Member2 is enabled") {
            callHelper.iSeeVideoForUsersEnabled("user2Name,user3Name")
        }

        step("And I see QR codes containing Member1 and Member2 emails") {
            pages.callingPage.apply {
                val member1 = clientUserManager.findUserByEmailOrEmailAlias("user2Email")
                val member2 = clientUserManager.findUserByEmailOrEmailAlias("user3Email")
                iSeeQrCodeContaining(member1.name ?: "", member1.email ?: "")
                iSeeQrCodeContaining(member2.name ?: "", member2.email ?: "")
            }
        }

        step("When I tap hang up button and do not see ongoing group call") {
            pages.callingPage.apply {
                iTapOnHangUpButton()
                iDoNotSeeOngoingGroupCall()
            }
        }

        step("And I see join button in group conversation view") {
            pages.callingPage.apply {
                iSeeJoinButtonInGroupConversationView()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4270")
    @Category("regression", "RC", "calling", "groupCalling")
    @Test
    fun givenMyVideoIsEnabledInGroupCall_whenIMinimiseAndRestoreCall_thenVideoRemainsEnabled() {
        givenTeamOwnerMembersAndGroupConversationArePrepared()

        step("And Member1 and Member2 start Chrome calling instances") {
            runBlocking {
                callHelper.userXStartsInstance(
                    "user2Name, user3Name",
                    "Chrome"
                )
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see conversation GroupCall in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupCall")
            }
        }

        step("And I tap on conversation name GroupCall in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("GroupCall")
            }
        }

        step("And Member1 and Member2 accept next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name,user3Name")
            }
        }

        step("And I wait until Wire service notification disappears") {
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }

        step("When I tap start call button") {
            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }
        }

        step("And Member1 and Member2 verify waiting instance status changes to active within 90 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user2Name,user3Name",
                    "active",
                    90
                )
            }
        }

        step("Then I see ongoing group call") {
            pages.callingPage.apply {
                iSeeOngoingGroupCall()
            }
        }

        step("And I see users Member1 and Member2 in ongoing group call") {
            callHelper.iSeeParticipantsInGroupCall("user2Name,user3Name")
        }

        step("And Member1 and Member2 unmute their microphone") {
            runBlocking {
                callingManager.unmuteMicrophone(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("And Member1 and Member2 verify to send and receive audio") {
            runBlocking {
                callingManager.verifySendAndReceiveAudio(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("When I turn camera on") {
            pages.callingPage.apply {
                iTurnCameraOn()
            }
        }

        step("And Member1 and Member2 switch video on") {
            runBlocking {
                callingManager.switchVideoOn(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("And Member1 and Member2 verify to receive audio and video") {
            runBlocking {
                callingManager.verifyReceiveAudioAndVideo(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("Then I see users Member1 and Member2 in ongoing group video call") {
            callHelper.iSeeParticipantsInGroupVideoCall("user2Name,user3Name")
        }

        step("And I see video for users Member1 and Member2 is enabled") {
            callHelper.iSeeVideoForUsersEnabled("user2Name,user3Name")
        }

        step("When I minimise the ongoing call") {
            pages.callingPage.apply {
                iMinimiseOngoingCall()
            }
        }

                step("And I wait for 1 seconds") {
            UiWaitUtils.waitFor(1.seconds)
        }

        step("And I restore the ongoing call") {
            pages.callingPage.apply {
                iRestoreOngoingCall()
            }
        }

        step("Then Member1 and Member2 verify to receive audio and video") {
            runBlocking {
                callingManager.verifyReceiveAudioAndVideo(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("And I see users Member1 and Member2 in ongoing group video call") {
            callHelper.iSeeParticipantsInGroupVideoCall("user2Name,user3Name")
        }

        step("And I see video for users Member1 and Member2 is enabled") {
            callHelper.iSeeVideoForUsersEnabled("user2Name,user3Name")
        }
    }
}
