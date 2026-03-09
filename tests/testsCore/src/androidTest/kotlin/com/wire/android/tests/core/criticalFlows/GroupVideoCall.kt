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
package com.wire.android.tests.core.criticalFlows

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import backendUtils.team.deleteTeam
import call.CallHelper
import call.CallingManager
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import service.TestServiceHelper
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import uiautomatorutils.PermissionUtils.grantRuntimePermsForForegroundApp
import uiautomatorutils.UiWaitUtils.WaitUtils.waitFor
import uiautomatorutils.UiWaitUtils.assertToastDisplayed
import uiautomatorutils.UiWaitUtils.iSeeSystemMessage
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.getValue

@RunWith(AndroidJUnit4::class)
class GroupVideoCall : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper
    private val callHelper by lazy { CallHelper() }
    private var teamOwnerA: ClientUser? = null
    private var teamOwnerB: ClientUser? = null
    private lateinit var callingManager: CallingManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
        testServiceHelper = TestServiceHelper(teamHelper.usersManager)
        callHelper.init(teamHelper.usersManager)
        callingManager = callHelper.callingManager
        grantRuntimePermsForForegroundApp(
            device,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )
    }

    @After
    fun tearDown() {
        runCatching { teamOwnerA?.deleteTeam(backendClient) }
        runCatching { teamOwnerB?.deleteTeam(backendClient) }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8608")
    @Category("criticalFlow")
    @Test
    fun givenGroupCall_whenVideoIsEnabled_thenGroupVideoIsVisible() {

        step("Given backend teams are prepared (WeLikeCalls + IJoinCalls) with owners and members") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "WeLikeCalls",
                "en_US",
                true,
                backendClient,
                context
            )

            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name, user3Name",
                "WeLikeCalls",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            teamHelper.usersManager.createTeamOwnerByAlias(
                "user4Name",
                "IJoinCalls",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And WeLikeCalls team owner creates GroupVideoCall conversation with team members") {
            testServiceHelper.userHasGroupConversationInTeam(
                "user1Name",
                "GroupVideoCall",
                "user2Name, user3Name",
                "WeLikeCalls"
            )
        }

        step("And participant devices and unique username are prepared for group call") {
            testServiceHelper.apply {
                addDevice("user4Name", null, "Device2")
                addDevice("user3Name", null, "Device1")
                runBlocking {
                    usersSetUniqueUsername("user3Name")
                }
            }
        }

        step("And team owners for WeLikeCalls and IJoinCalls are resolved") {
            teamOwnerA = teamHelper.usersManager.findUserBy(
                "user1Name",
                ClientUserManager.FindBy.NAME_ALIAS
            )
            teamOwnerB = teamHelper.usersManager.findUserBy(
                "user4Name",
                ClientUserManager.FindBy.NAME_ALIAS
            )
        }

        step("And conference calling is enabled for WeLikeCalls and IJoinCalls via backdoor") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "WeLikeCalls"
                )
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user4Name",
                    "IJoinCalls"
                )
            }
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

        step("And I login as WeLikeCalls team owner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwnerA?.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwnerA?.password ?: "")
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

        step("And I verify GroupVideoCall conversation is visible and start new conversation flow") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupVideoCall")
                tapStartNewConversationButton()
            }
        }

        step("And I open people search to find TeamOwnerB") {
            pages.searchPage.apply {
                tapSearchPeopleField()
            }
        }

        step("And I search TeamOwnerB by unique username") {
            pages.searchPage.apply {
                typeUniqueUserNameInSearchField(teamHelper, "user4Name")
            }
        }

        step("And I verify TeamOwnerB appears in search results and open profile") {
            pages.searchPage.apply {
                assertUsernameInSearchResultIs(teamOwnerB?.name ?: "")
                tapUsernameInSearchResult(teamOwnerB?.name ?: "")
            }
        }

        step("And I verify unconnected profile belongs to TeamOwnerB") {
            pages.unconnectedUserProfilePage.apply {
                assertUserNameInUnconnectedUserProfilePage(teamOwnerB?.name ?: "")
            }
        }

        step("And I send connection request to TeamOwnerB and verify confirmation toast") {
            pages.unconnectedUserProfilePage.apply {
                clickConnectionRequestButton()
                waitUntilToastIsDisplayed("Connection request sent")
            }
        }

        step("And I close unconnected profile and return to conversation list") {
            pages.unconnectedUserProfilePage.apply {
                clickCloseButtonOnUnconnectedUserProfilePage()
            }
            pages.conversationListPage.apply {
                clickCloseButtonOnNewConversationScreen()
            }
        }

        step("And I verify pending status is visible for TeamOwnerB") {
            pages.conversationListPage.apply {
                assertConversationNameWithPendingStatusVisibleInConversationList(
                    teamOwnerB?.name ?: ""
                )
            }
        }

        step("And TeamOwnerB connection request is accepted via backend") {
            runBlocking {
                val user = teamHelper.usersManager.findUserByNameOrNameAlias("user4Name")
                backendClient.acceptAllIncomingConnectionRequests(user)
            }
        }

        step("And I verify pending status is removed and GroupVideoCall conversation remains visible") {
            pages.conversationListPage.apply {
                assertPendingStatusIsNoLongerVisible()
                assertGroupConversationVisible("GroupVideoCall")
            }
        }

        step("And I verify TeamOwnerB conversation is visible and open GroupVideoCall") {
            pages.conversationListPage.apply {
                assertConversationIsVisibleWithTeamOwner(teamOwnerB?.name ?: "")
                tapConversationNameInConversationList("GroupVideoCall")
            }
        }

        step("And I open GroupVideoCall conversation details") {
            pages.conversationViewPage.apply {
                clickOnGroupConversationDetails("GroupVideoCall")
            }
        }

        step("And I open participants tab and start add participant flow") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                tapAddParticipantsButton()
            }
        }

        step("And I select TeamOwnerB from participant suggestions") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameInSuggestionsListIs(teamOwnerB?.name ?: "")
                selectUserInSuggestionList(teamOwnerB?.name ?: "")
                tapContinueButton()
            }
        }

        step("And I verify TeamOwnerB is added to participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(teamOwnerB?.name ?: "")
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("And I verify system message confirms TeamOwnerB was added") {
            iSeeSystemMessage("You added ${teamOwnerB?.name ?: ""} to the conversation")
        }

        step("And <Member1>, <Member2>, and <TeamOwnerB> start instances using Chrome") {
            runBlocking {
                callHelper.userXStartsInstance(
                    "user2Name, user3Name, user4Name",
                    "Chrome"
                )
            }
        }

        step("And <Member1>, <Member2>, and <TeamOwnerB> auto-accept the next incoming call") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically(
                    "user2Name, user3Name, user4Name"
                )
            }
        }

        step("When I start group call from GroupVideoCall conversation") {
            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }
        }

        step("Then <Member1>, <Member2>, and <TeamOwnerB> verify waiting instance status changes to active within 90 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user2Name, user3Name, user4Name",
                    "active",
                    90
                )
            }
        }

        step("And I see ongoing group call") {
            pages.callingPage.apply {
                iSeeOngoingGroupCall()
            }
        }

        step("And I see users <Member1>, <Member2>, and <TeamOwnerB> in ongoing group call") {
            callHelper.iSeeParticipantsInGroupCall("user2Name, user3Name, user4Name")
        }

        step("And I turn camera on") {
            pages.callingPage.apply {
                iTurnCameraOn()
            }
        }

        step("And users <Member1>, <Member2>, and <TeamOwnerB> switch video on") {
            runBlocking {
                val callParticipantsSwitchVideoOn =
                    teamHelper.usersManager.splitAliases("user2Name, user3Name, user4Name")
                callingManager.switchVideoOn(callParticipantsSwitchVideoOn)
            }
        }

        step("And users <Member1>, <Member2>, and <TeamOwnerB> verify audio and video are received") {
            runBlocking {
                val assertCallParticipantsReceiveAudioVideo =
                    teamHelper.usersManager.splitAliases("user2Name, user3Name, user4Name")
                callingManager.verifyReceiveAudioAndVideo(assertCallParticipantsReceiveAudioVideo)
            }
        }

        step("And I see users <Member1>, <Member2>, and <TeamOwnerB> in ongoing group video call") {
            callHelper.iSeeParticipantsInGroupVideoCall("user2Name, user3Name, user4Name")
        }

        step("And I minimise ongoing call to continue conversation actions") {
            pages.callingPage.apply {
                iMinimiseOngoingCall()
            }
        }

        step("And I tap ping button in conversation view") {
            pages.conversationViewPage.apply {
                tapMessageInInputField()
                tapPingButton()
            }
        }

        step("And I see confirmation alert with text \"Are you sure you want to ping 4 people?\" in conversation view") {
            pages.conversationViewPage.apply {
                iSeePingModalWithText("Are you sure you want to ping 4 people?")
            }
        }

        step("And I confirm ping and see system message 'You pinged'") {
            pages.conversationViewPage.apply {
                tapPingButtonModal()
                iSeeSystemMessage("You pinged")
                closeKeyboardIfOpened()
            }
        }

        step("And I attempt to start audio recording during ongoing call") {
            pages.conversationViewPage.apply {
                // `assertToastDisplayed` starts an accessibility-event listener before running `trigger`.
                // We must perform the tap/share actions inside `trigger`; otherwise the transient toast can appear and disappear before observation starts.
                assertToastDisplayed("You can't record an audio message during a call.", trigger = {
                    iTapFileSharingButton()
                    tapSharingOption("Audio")
                    iTapFileSharingButton()
                })
            }
        }

        step("And <Member2> sends audio file message via device Device1 to GroupVideoCall conversation") {
            pages.conversationViewPage.apply {
                testServiceHelper.contactSendsLocalAudioConversation(
                    context,
                    "AudioFile",
                    "user3Name",
                    "Device1",
                    "GroupVideoCall"
                )
            }
        }

        step("And I see audio file message in conversation") {
            pages.conversationViewPage.apply {
                assertAudioMessageIsVisible()
            }
        }

        step("And I see audio playback time starts at zero") {
            pages.conversationViewPage.apply {
                assertAudioTimeStartsAtZero()
            }
        }
        waitFor(1)
        step("And I play audio message") {
            pages.conversationViewPage.apply {
                clickPlayButtonOnAudioMessage()
            }
        }

        step("And I pause audio message after 10 seconds") {
            pages.conversationViewPage.apply {
                waitFor(10) // wait to allow an audio file to play
                clickPauseButtonOnAudioMessage()
            }
        }

        step("Then I verify audio playback time is no longer zero") {
            pages.conversationViewPage.apply {
                assertAudioTimeIsNotZeroAnymore()
            }
        }

        step("And I restore ongoing group call and verify users <Member1>, <Member2>, and <TeamOwnerB> remain connected") {
            pages.callingPage.apply {
                iRestoreOngoingCall()
            }
            callHelper.iSeeParticipantsInGroupCall("user2Name, user3Name, user4Name")
        }

        step("And I hang up group call and verify call is ended") {
            pages.callingPage.apply {
                iTapOnHangUpButton()
                iDoNotSeeOngoingGroupCall()
            }
        }
    }
}
