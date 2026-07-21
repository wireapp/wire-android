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
import uiautomatorutils.UiWaitUtils.VERY_SHORT_TIMEOUT
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class CallParticipantListTests : BaseCallUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member: ClientUser
    private lateinit var member2: ClientUser
    private lateinit var guest: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        initCallTestHelpers()
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-10873", "TC-10892")
    @Category("regression", "callParticipant", "RC", "smoke")
    @Test
    fun givenIAmInOneOnOneCall_whenIExpandParticipantSheet_thenParticipantListAndCallControlsAreVisible() {
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
                UiWaitUtils.waitFor(VERY_SHORT_TIMEOUT)
            }
        }

        step("When I expand the participant sheet") {
            pages.callingPage.apply {
                iExpandParticipantSheet()
            }
        }

        step("Then I see the 1:1 call participant list") {
            pages.callingPage.apply {
                iSeeParticipantsCount(2)
                iSeeUserInParticipantList(teamOwner.name ?: "")
                iSeeUserInParticipantList(member.name ?: "")
            }
        }

        // TC-10892 I want to be able to see the call controls while the participant sheet is expanded
        step("And I see call controls while the participant sheet is expanded") {
            pages.callingPage.apply {
                iSeeCallControls()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId(
        "TC-10877",
        "TC-10872",
        "TC-10879",
        "TC-10880",
        "TC-10874",
        "TC-10886",
        "TC-10881",
        "TC-10883",
        "TC-10884",
        "TC-10887"
    )
    @Category("regression", "callParticipant", "RC", "smoke")
    @Test
    fun givenIAmInGroupCall_whenIExpandParticipantSheet_thenParticipantCountOrderAndMediaStatesAreVisible() {
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
                UiWaitUtils.waitFor(VERY_SHORT_TIMEOUT)
            }
        }

        step("When I expand the participant sheet") {
            pages.callingPage.apply {
                iExpandParticipantSheet()
            }
        }
        // TC-10872 I want to be able to see the list of participants in a group call.
        // TC-10877 I want to be able to see the correct participant count in a call with multiple users.
        step("Then I see the correct group call participant count") {
            pages.callingPage.apply {
                iSeeParticipantsCount(3)
            }
        }

        // TC-10879 I want to be able to see participants ordered alphabetically.
        step("And I see participants ordered alphabetically") {
            pages.callingPage.apply {
                iSeeParticipantsOrderedAlphabetically(
                    listOf(
                        teamOwner.name ?: "",
                        member.name ?: "",
                        member2.name ?: ""
                    )
                )
            }
        }

        // TC-10880 I want to be able to see the muted state of participants
        // TC-10874 I want to be able to see the correct mute/unmute state of each participant in a call.
        step("And I see muted state for Member1 and Member2") {
            pages.callingPage.apply {
                iSeeParticipantMuted(member.name ?: "")
                iSeeParticipantMuted(member2.name ?: "")
            }
        }

        // TC-10886 I want to be able to see combined participant states correctly
        // TC-10887 I want to be able to see participant state updates in real time
        step("And I see camera-off state for Member1 and Member2") {
            pages.callingPage.apply {
                iSeeParticipantCameraOff(member.name ?: "")
                iSeeParticipantCameraOff(member2.name ?: "")
            }
        }

        step("When Member2 unmutes their microphone") {
            runBlocking {
                callingManager.unmuteMicrophone(
                    clientUserManager.splitAliases("user3Name")
                )
            }
        }

        // TC-10881 I want to be able to see the unmuted state of participants
        step("Then I see unmuted state for TeamOwner and Member2") {
            pages.callingPage.apply {
                iSeeParticipantUnmuted(teamOwner.name ?: "")
                iSeeParticipantUnmuted(member2.name ?: "")
            }
        }

        step("When I turn camera on and Member2 switches video on") {
            pages.callingPage.apply {
                iTurnCameraOn()
            }
            runBlocking {
                callingManager.switchVideoOn(
                    clientUserManager.splitAliases("user3Name")
                )
            }
        }

        // TC-10883 I want to be able to see the camera-on state of participants
        step("Then I see camera-on state for TeamOwner and Member2") {
            pages.callingPage.apply {
                iSeeParticipantCameraOn(teamOwner.name ?: "")
                iSeeParticipantCameraOn(member2.name ?: "")
            }
        }

        // TC-10884 I want to be able to see the camera-off state of participants
        step("And I see camera-off state for Member1") {
            pages.callingPage.apply {
                iSeeParticipantCameraOff(member.name ?: "")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-10878", "TC-10888", "TC-10889")
    @Category("regression", "callParticipant", "RC", "smoke")
    @Test
    fun givenParticipantSheetIsOpen_whenGuestIsPresentAndParticipantJoinsLeavesOrIsTapped_thenListUpdatesWithoutAction() {
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

        step("And TeamOwner adds Member1 to team") {
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
        }

        step("And guest user is created") {
            clientUserManager.createPersonalUsersByAliases(listOf("user4Name"), backendClient)
            guest = clientUserManager.findUserBy(
                "user4Name",
                ClientUserManager.FindBy.NAME_ALIAS
            )
        }

        step("And guest user has unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user4Name")
            }
        }

        step("And TeamOwner is connected to guest user") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user4Name")
        }

        step("And TeamOwner enables conference calling feature via backdoor") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "WeLikeCalls"
                )
            }
        }

        step("And TeamOwner has group conversation ParticipantGuest with Member1 and guest") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ParticipantGuest",
                "user2Name,user4Name",
                "WeLikeCalls"
            )
        }

        step("And Member1 and guest start Chrome calling instances") {
            runBlocking {
                callHelper.userXStartsInstance(
                    "user2Name,user4Name",
                    "Chrome"
                )
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see conversation ParticipantGuest in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("ParticipantGuest")
            }
        }

        step("And I tap on conversation name ParticipantGuest in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("ParticipantGuest")
            }
        }

        step("And Member1 accepts next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name")
            }
        }

        step("When I tap start call button") {
            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }
        }

        step("And Member1 verifies waiting instance status changes to active within 90 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user2Name",
                    "active",
                    90
                )
            }
        }

        step("Then I see ongoing group call") {
            pages.callingPage.apply {
                iSeeOngoingGroupCall()
                UiWaitUtils.waitFor(VERY_SHORT_TIMEOUT)
            }
        }

        step("When I expand the participant sheet") {
            pages.callingPage.apply {
                iExpandParticipantSheet()
            }
        }

        step("Then I see TeamOwner and Member1 in participant list") {
            pages.callingPage.apply {
                iSeeParticipantsCount(2, timeout = UiWaitUtils.MEDIUM_TIMEOUT)
                iSeeUserInParticipantList(teamOwner.name ?: "")
                iSeeUserInParticipantList(member.name ?: "")
                iDoNotSeeUserInParticipantList(guest.name ?: "")
            }
        }

        // TC-10889 I want to be able to see when a participant joins/leaves the call.
        step("When guest joins the call") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user4Name")
                callHelper.userVerifiesCallStatusToUserY(
                    "user4Name",
                    "active",
                    90
                )
            }
        }

        // TC-10878 I want to be able to see guest users included in the participant count.
        step("Then I see guest user included in participant count") {
            pages.callingPage.apply {
                iSeeParticipantsCount(3, timeout = UiWaitUtils.MEDIUM_TIMEOUT)
                iSeeUserInParticipantList(guest.name ?: "", timeout = UiWaitUtils.MEDIUM_TIMEOUT)
                iSeeGuestBadgeForUser(guest.name ?: "Guest")
            }
        }

        // TC-10888 I want to be able to tap on a participant without triggering any action.
        step("When I tap Member1 in participant list") {
            pages.callingPage.apply {
                iTapParticipantInList(member.name ?: "")
            }
        }

        step("Then participant list remains open without triggering an action") {
            pages.callingPage.apply {
                iSeeParticipantsCount(3)
                iSeeUserInParticipantList(member.name ?: "")
                iSeeCallControls()
            }
        }

        step("When guest leaves the call") {
            runBlocking {
                callingManager.stopIncomingCall(clientUserManager.splitAliases("user4Name"))
            }
        }

        step("Then I see guest leave the participant list") {
            pages.callingPage.apply {
                iSeeParticipantsCount(2, timeout = UiWaitUtils.MEDIUM_TIMEOUT)
                iDoNotSeeUserInParticipantList(guest.name ?: "")
            }
        }

        step("When I tap hang up button and do not see ongoing group call") {
            pages.callingPage.apply {
                iTapOnHangUpButton()
                iDoNotSeeOngoingGroupCall()
            }
        }
    }

    // Shared backend setup for 1:1 call tests: creates TeamOwner, adds Member to the team,
    // and creates their 1:1 conversation.
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

    // Shared backend setup for group call tests: creates TeamOwner, adds Member1 and Member2,
    // enables conference calling, and creates their group conversation.
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
            member = clientUserManager.findUserBy(
                "user2Name",
                ClientUserManager.FindBy.NAME_ALIAS
            )
            member2 = clientUserManager.findUserBy(
                "user3Name",
                ClientUserManager.FindBy.NAME_ALIAS
            )
        }

        step("And TeamOwner enables conference calling feature via backdoor") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "WeLikeCalls"
                )
            }
        }

        step("And TeamOwner has group conversation GroupCall with Member1 and Member2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "GroupCall",
                "user2Name,user3Name",
                "WeLikeCalls"
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
}
