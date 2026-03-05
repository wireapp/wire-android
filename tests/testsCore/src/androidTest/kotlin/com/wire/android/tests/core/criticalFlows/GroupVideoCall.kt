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
import deleteDownloadedFilesContaining
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

        step("Prepare team via backend (WeLikeCalls team + IJoinCalls team) with owners and members") {
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

        step("WeLikeCalls team owner creates a group conversation with team members") {
            testServiceHelper.userHasGroupConversationInTeam(
                "user1Name",
                "GroupVideoCall",
                "user2Name, user3Name",
                "WeLikeCalls"
            )
        }

        step("Prepare devices and unique username for group call participants") {
            testServiceHelper.apply {
                addDevice("user4Name", null, "Device2")
                addDevice("user3Name", null, "Device1")
                runBlocking {
                    usersSetUniqueUsername("user3Name")
                }
            }
        }

        step("Resolve team owners for WeLikeCalls and IJoinCalls") {
            teamOwnerA = teamHelper.usersManager.findUserBy(
                "user1Name",
                ClientUserManager.FindBy.NAME_ALIAS
            )
            teamOwnerB = teamHelper.usersManager.findUserBy(
                "user4Name",
                ClientUserManager.FindBy.NAME_ALIAS
            )
        }

        step("Enable conference calling feature for WeLikeCalls and IJoinCalls teams via backdoor") {
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

        step("Verify welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("Open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("Login as WeLikeCalls team owner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwnerA?.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwnerA?.password ?: "")
                clickLoginButton()
            }
        }

        step("Complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Verify group conversation is visible and start a new conversation flow") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupVideoCall")
                tapStartNewConversationButton()
            }
        }

        step("Open people search to find owner from another team") {
            pages.searchPage.apply {
                tapSearchPeopleField()
            }
        }

        step("Search TeamOwnerB by unique username") {
            pages.searchPage.apply {
                typeUniqueUserNameInSearchField(teamHelper, "user4Name")
            }
        }

        step("Verify TeamOwnerB appears in search results and open profile") {
            pages.searchPage.apply {
                assertUsernameInSearchResultIs(teamOwnerB?.name ?: "")
                tapUsernameInSearchResult(teamOwnerB?.name ?: "")
            }
        }

        step("Verify unconnected profile belongs to TeamOwnerB") {
            pages.unconnectedUserProfilePage.apply {
                assertUserNameInUnconnectedUserProfilePage(teamOwnerB?.name ?: "")
            }
        }

        step("Send connection request to TeamOwnerB and verify confirmation toast") {
            pages.unconnectedUserProfilePage.apply {
                clickConnectionRequestButton()
                waitUntilToastIsDisplayed("Connection request sent")
            }
        }

        step("Close unconnected profile and return to conversation list") {
            pages.unconnectedUserProfilePage.apply {
                clickCloseButtonOnUnconnectedUserProfilePage()
            }
            pages.conversationListPage.apply {
                clickCloseButtonOnNewConversationScreen()
            }
        }

        step("Verify pending conversation is visible for TeamOwnerB") {
            pages.conversationListPage.apply {
                assertConversationNameWithPendingStatusVisibleInConversationList(
                    teamOwnerB?.name ?: ""
                )
            }
        }

        step("Accept TeamOwnerB connection request via backend") {
            runBlocking {
                val user = teamHelper.usersManager.findUserByNameOrNameAlias("user4Name")
                backendClient.acceptAllIncomingConnectionRequests(user)
            }
        }

        step("Verify pending status is removed and GroupVideoCall remains visible") {
            pages.conversationListPage.apply {
                assertPendingStatusIsNoLongerVisible()
                assertGroupConversationVisible("GroupVideoCall")
            }
        }

        step("Verify TeamOwnerB conversation is visible and open GroupVideoCall") {
            pages.conversationListPage.apply {
                assertConversationIsVisibleWithTeamOwner(teamOwnerB?.name ?: "")
                tapConversationNameInConversationList("GroupVideoCall")
            }
        }

        step("Open group conversation details") {
            pages.conversationViewPage.apply {
                clickOnGroupConversationDetails("GroupVideoCall")
            }
        }

        step("Open participants tab and start add participant flow") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                tapAddParticipantsButton()
            }
        }

        step("Select TeamOwnerB from participant suggestions") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameInSuggestionsListIs(teamOwnerB?.name ?: "")
                selectUserInSuggestionList(teamOwnerB?.name ?: "")
                tapContinueButton()
            }
        }

        step("Verify TeamOwnerB is added to participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(teamOwnerB?.name ?: "")
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("Verify system message confirms TeamOwnerB was added") {
            iSeeSystemMessage("You added ${teamOwnerB?.name ?: ""} to the conversation")
        }

        step("Start browser call instances for user2Name, user3Name, and user4Name") {
            runBlocking {
                callHelper.userXStartsInstance(
                    "user2Name, user3Name, user4Name",
                    "Chrome"
                )
            }
        }

        step("Enable auto-accept for the next incoming call on user2Name, user3Name, and user4Name") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically(
                    "user2Name, user3Name, user4Name"
                )
            }
        }

        step("Start group call from GroupVideoCall conversation") {
            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }
        }

        step("Verify group call is active for all participants via backend") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user2Name, user3Name, user4Name",
                    "active",
                    90
                )
            }
        }

        step("Verify ongoing group call UI and participant list") {
            pages.callingPage.apply {
                iSeeOngoingGroupCall()
            }
            callHelper.iSeeParticipantsInGroupCall("user2Name, user3Name, user4Name")
        }

        step("Turn camera on for local participant") {
            pages.callingPage.apply {
                iTurnCameraOn()
            }
        }

        step("Switch video on for participants") {
            waitFor(2)
            runBlocking {
                val callParticipantsSwitchVideoOn =
                    teamHelper.usersManager.splitAliases("user2Name, user3Name, user4Name")
                callingManager.switchVideoOn(callParticipantsSwitchVideoOn)
            }
        }

        step("Verify participants receive audio/video in group call") {
            runBlocking {
                val assertCallParticipantsReceiveAudioVideo =
                    teamHelper.usersManager.splitAliases("user2Name, user3Name, user4Name")
                callingManager.verifyReceiveAudioAndVideo(assertCallParticipantsReceiveAudioVideo)
                callHelper.iSeeParticipantsInGroupVideoCall("user2Name, user3Name, user4Name")
            }
        }

        step("Minimise ongoing call to continue conversation actions") {
            pages.callingPage.apply {
                iMinimiseOngoingCall()
            }
        }

        step("Ping all call participants from conversation") {
            pages.conversationViewPage.apply {
                tapMessageInInputField()
                tapPingButton()
                iSeePingModalWithText("Are you sure you want to ping 4 people?")
                tapPingButtonModal()
                iSeeSystemMessage("You pinged")
                closeKeyboardIfOpened()
            }
        }

        step("Verify audio recording is blocked during ongoing call") {
            pages.conversationViewPage.apply {
                assertToastDisplayed("You can't record an audio message during a call.", trigger = {
                    iTapFileSharingButton()
                    tapSharingOption("Audio")
                    iTapFileSharingButton()
                })
            }
        }

        step("Receive and assert audio file message in conversation") {
//            waitFor(2)
            pages.conversationViewPage.apply {
                testServiceHelper.contactSendsLocalAudioConversation(
                    context,
                    "AudioFile",
                    "user3Name",
                    "Device1",
                    "GroupVideoCall"
                )
                assertAudioMessageIsVisible()
                assertAudioTimeStartsAtZero()
            }
        }

        step("Play audio message and verify playback time progresses") {
            pages.conversationViewPage.apply {
                clickPlayButtonOnAudioMessage()
                waitFor(10)
                clickPauseButtonOnAudioMessage()
                assertAudioTimeIsNotZeroAnymore()
            }
        }

        step("Restore ongoing group call and verify participants remain connected") {
            pages.callingPage.apply {
                iRestoreOngoingCall()
            }
            callHelper.iSeeParticipantsInGroupCall("user2Name, user3Name, user4Name")
        }

        step("Hang up group call and verify call is ended") {
            pages.callingPage.apply {
                iTapOnHangUpButton()
                iDoNotSeeOngoingGroupCall()
            }
        }
    }
}
