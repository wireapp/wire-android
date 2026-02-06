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
//    fun givenGroupCall_whenVideoIsEnabled_thenGroupVideoIsVisible() {
//        step("Prepare team via backend (WeLikeCalls team + IJoinCalls team) with owners and members") {
//            teamHelper.usersManager.createTeamOwnerByAlias(
//                "user1Name",
//                "WeLikeCalls",
//                "en_US",
//                true,
//                backendClient,
//                context
//            )
//
//            teamHelper.userXAddsUsersToTeam(
//                "user1Name",
//                "user2Name, user3Name,user4Name",
//                "WeLikeCalls",
//                TeamRoles.Member,
//                backendClient,
//                context,
//                true
//            )
//
//            teamHelper.usersManager.createTeamOwnerByAlias(
//                "user5Name",
//                "IJoinCalls",
//                "en_US",
//                true,
//                backendClient,
//                context
//            )
//
//            step("WeLikeCalls team owner creates a group conversation with team members") {
//                testServiceHelper.userHasGroupConversationInTeam(
//                    "user1Name",
//                    "GroupVideoCall",
//                    "user2Name, user3Name,user4Name",
//                    "WeLikeCalls"
//                )
//            }
//
//            step("Prepare devices and unique username for group call participants") {
//                testServiceHelper.apply {
//                    addDevice("user5Name", null, "Device1")
//                    addDevice("user4Name", null, "Device2")
//                    runBlocking {
//                        usersSetUniqueUsername("user3Name")
//                    }
//                }
//            }
//
//
//            teamOwnerA = teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
//            teamOwnerB = teamHelper.usersManager.findUserBy("user5Name", ClientUserManager.FindBy.NAME_ALIAS)
//
//            step("Enable conference calling feature for WeLikeCalls and IJoinCalls teams via backdoor") {
//                runBlocking {
//                    callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
//                        "user1Name",
//                        "WeLikeCalls"
//                    )
//                    callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
//                        "user5Name",
//                        "IJoinCalls"
//                    )
//                }
//            }
//
//                step("Login as team owner in Android app") {
//                    pages.registrationPage.apply {
//                        assertEmailWelcomePage()
//                    }
//                    pages.loginPage.apply {
//                        clickStagingDeepLink()
//                        clickProceedButtonOnDeeplinkOverlay()
//                    }
//                    pages.loginPage.apply {
//                        enterTeamOwnerLoggingEmail(teamOwnerA?.email ?: "")
//                        clickLoginButton()
//                        enterTeamOwnerLoggingPassword(teamOwnerA?.password ?: "")
//                        clickLoginButton()
//                    }
//                    pages.registrationPage.apply {
//                        waitUntilLoginFlowIsCompleted()
//                        clickAllowNotificationButton()
//                        clickDeclineShareDataAlert()
//
//                        waitFor(60)
//                    }
//                }
//
//            }
//        }
//    }


//    fun givenGroupCall_whenVideoIsEnabled_thenGroupVideoIsVisible() {
//
//        step("Prepare team via backend (WeLikeCalls team + IJoinCalls team) with owners and members") {
//            teamHelper.usersManager.createTeamOwnerByAlias(
//                "user1Name",
//                "WeLikeCalls",
//                "en_US",
//                true,
//                backendClient,
//                context
//            )
//
//            teamHelper.userXAddsUsersToTeam(
//                "user1Name",
//                "user2Name, user3Name,user4Name",
//                "WeLikeCalls",
//                TeamRoles.Member,
//                backendClient,
//                context,
//                true
//            )
//
//            teamHelper.usersManager.createTeamOwnerByAlias(
//                "user5Name",
//                "IJoinCalls",
//                "en_US",
//                true,
//                backendClient,
//                context
//            )
//        }
//
//        step("WeLikeCalls team owner creates a group conversation with team members") {
//            testServiceHelper.userHasGroupConversationInTeam(
//                "user1Name",
//                "GroupVideoCall",
//                "user2Name, user3Name,user4Name",
//                "WeLikeCalls"
//            )
//        }
//
//        step("Prepare devices and unique username for group call participants") {
//            testServiceHelper.apply {
//                addDevice("user5Name", null, "Device1")
//                addDevice("user4Name", null, "Device2")
//                runBlocking {
//                    usersSetUniqueUsername("user3Name")
//                }
//            }
//        }
//
//        step("Resolve team owners for WeLikeCalls and IJoinCalls") {
//            teamOwnerA = teamHelper.usersManager.findUserBy(
//                "user1Name",
//                ClientUserManager.FindBy.NAME_ALIAS
//            )
//            teamOwnerB = teamHelper.usersManager.findUserBy(
//                "user5Name",
//                ClientUserManager.FindBy.NAME_ALIAS
//            )
//        }
//
//        step("Enable conference calling feature for WeLikeCalls and IJoinCalls teams via backdoor") {
//            runBlocking {
//                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
//                    "user1Name",
//                    "WeLikeCalls"
//                )
//                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
//                    "user5Name",
//                    "IJoinCalls"
//                )
//            }
//        }
//
//        step("Login as team owner in Android app") {
//            pages.registrationPage.apply {
//                assertEmailWelcomePage()
//            }
//            pages.loginPage.apply {
//                clickStagingDeepLink()
//                clickProceedButtonOnDeeplinkOverlay()
//            }
//            pages.loginPage.apply {
//                enterTeamOwnerLoggingEmail(teamOwnerA?.email ?: "")
//                clickLoginButton()
//                enterTeamOwnerLoggingPassword(teamOwnerA?.password ?: "")
//                clickLoginButton()
//            }
//            pages.registrationPage.apply {
//                waitUntilLoginFlowIsCompleted()
//                clickAllowNotificationButton()
//                clickDeclineShareDataAlert()
//            }
//
//            step("Verify group conversation is visible and start a new conversation flow") {
//                pages.conversationListPage.apply {
//                    assertGroupConversationVisible("GroupVideoCall")
//                    tapStartNewConversationButton()
//                }
//
//                step("Search for team owner from another team") {
//                    pages.searchPage.apply {
//                        tapSearchPeopleField()
//                        typeUniqueUserNameInSearchField(teamHelper, "user5Name")
//                        assertUsernameInSearchResultIs(teamOwnerB?.name ?: "")
//                        tapUsernameInSearchResult(teamOwnerB?.name ?: "")
//                    }
//                    step("Send connection request to existing team owner") {
//                        pages.unconnectedUserProfilePage.apply {
//                            assertUserNameInUnconnectedUserProfilePage(teamOwnerB?.name ?: "")
//                            clickConnectionRequestButton()
//                            waitUntilToastIsDisplayed("Connection request sent")
//                            //connectedUserProfilePage.assertToastMessageIsDisplayed("Connection request sent")
//                            clickCloseButtonOnUnconnectedUserProfilePage()
//                        }
//                        pages.conversationListPage.apply {
//                            clickCloseButtonOnNewConversationScreen()
//                            assertConversationNameWithPendingStatusVisibleInConversationList(teamOwnerB?.name ?: "")
//                        }
//
//                        step("Accept connection request via backend and start conversation") {
//                            runBlocking {
//                                val user = teamHelper.usersManager.findUserByNameOrNameAlias("user5Name")
//                                backendClient.acceptAllIncomingConnectionRequests(user)
//                            }
//                            waitFor(1)
//                            pages.conversationListPage.apply {
//                                assertPendingStatusIsNoLongerVisible()
//                                assertGroupConversationVisible("GroupVideoCall")
//                                assertConversationIsVisibleWithTeamOwner(teamOwnerB?.name ?: "")
//                                tapConversationNameInConversationList("GroupVideoCall")
//
//                            }
//                            pages.conversationViewPage.apply {
//                                clickOnGroupConversationDetails("GroupVideoCall")
//                            }
//                            pages.groupConversationDetailsPage.apply {
//                                tapOnParticipantsTab()
//                                tapAddParticipantsButton()
//                                assertUsernameInSuggestionsListIs(teamOwnerB?.name ?: "")
//                                selectUserInSuggestionList(teamOwnerB?.name ?: "")
//                                tapContinueButton()
//                                assertUsernameIsAddedToParticipantsList(teamOwnerB?.name ?: "")
//                                tapCloseButtonOnGroupConversationDetailsPage()
//                                waitUntilToastIsDisplayed("You added ${teamOwnerB?.name ?: ""} to the conversation")
//
//                                runBlocking {
//                                    callHelper.userXStartsInstance(
//                                        "user2Name, user3Name, user4Name, user5Name",
//                                        "Chrome"
//                                    )
//
//                                    runBlocking {
//                                        callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name, user3Name, user4Name, user5Name")
//                                    }
//
//                                    pages.conversationViewPage.apply {
//                                        iTapStartCallButton()
//                                    }
//
//                                    step("Verify group call is active and participants joined") {
//                                        runBlocking {
//                                            callHelper.userVerifiesCallStatusToUserY(
//                                                "user2Name, user3Name, user4Name, user5Name",
//                                                "active",
//                                                90
//                                            )
//                                        }
//                                        pages.callingPage.apply {
//                                            iSeeOngoingGroupCall()
//
//                                        }
//                                        callHelper.iSeeParticipantsInGroupCall("user2Name, user3Name, user4Name, user5Name")
//                                    }
//                                    pages.callingPage.apply {
//                                        iTurnCameraOn()
//                                    }
//                                }
//                                waitFor(10)
//
//                                runBlocking {
//
//                                    val callParticipantsVideo =
//                                        teamHelper.usersManager.splitAliases("user2Name, user3Name, user4Name, user5Name")
//                                    callingManager.switchVideoOn(callParticipantsVideo)
//
//                                val callParticipantsAudioVideo =
//                                    teamHelper.usersManager.splitAliases("user2Name, user3Name, user4Name, user5Name")
//                                callingManager.verifyReceiveAudioAndVideo(callParticipantsAudioVideo)
//                            }
//                                waitFor(10)
//
//                            }
//
//                                }
//                            }
//
//
//                        }
//
//                    }
//                }
//
//            }
//        }

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
                "user2Name, user3Name,user4Name",
                "WeLikeCalls",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            teamHelper.usersManager.createTeamOwnerByAlias(
                "user5Name",
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
                "user2Name, user3Name,user4Name",
                "WeLikeCalls"
            )
        }

        step("Prepare devices and unique username for group call participants") {
            testServiceHelper.apply {
                addDevice("user5Name", null, "Device1")
                addDevice("user4Name", null, "Device2")
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
                "user5Name",
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
                    "user5Name",
                    "IJoinCalls"
                )
            }
        }

        step("Login as team owner in Android app") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwnerA?.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwnerA?.password ?: "")
                clickLoginButton()
            }
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

        step("Search for team owner from another team") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(teamHelper, "user5Name")
                assertUsernameInSearchResultIs(teamOwnerB?.name ?: "")
                tapUsernameInSearchResult(teamOwnerB?.name ?: "")
            }
        }

        step("Send connection request to existing team owner") {
            pages.unconnectedUserProfilePage.apply {
                assertUserNameInUnconnectedUserProfilePage(teamOwnerB?.name ?: "")
                clickConnectionRequestButton()
                waitUntilToastIsDisplayed("Connection request sent")
                //connectedUserProfilePage.assertToastMessageIsDisplayed("Connection request sent")
                clickCloseButtonOnUnconnectedUserProfilePage()
            }
            pages.conversationListPage.apply {
                clickCloseButtonOnNewConversationScreen()
                assertConversationNameWithPendingStatusVisibleInConversationList(
                    teamOwnerB?.name ?: ""
                )
            }
        }

        step("Accept connection request via backend and start conversation") {
            runBlocking {
                val user = teamHelper.usersManager.findUserByNameOrNameAlias("user5Name")
                backendClient.acceptAllIncomingConnectionRequests(user)
            }

            waitFor(1)

            pages.conversationListPage.apply {
                assertPendingStatusIsNoLongerVisible()
                assertGroupConversationVisible("GroupVideoCall")
                assertConversationIsVisibleWithTeamOwner(teamOwnerB?.name ?: "")
                tapConversationNameInConversationList("GroupVideoCall")
            }
        }

        step("Open group conversation details") {
            pages.conversationViewPage.apply {
                clickOnGroupConversationDetails("GroupVideoCall")
            }
        }

        step("Add TeamOwnerB to participants list") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                tapAddParticipantsButton()
                assertUsernameInSuggestionsListIs(teamOwnerB?.name ?: "")
                selectUserInSuggestionList(teamOwnerB?.name ?: "")
                tapContinueButton()
                assertUsernameIsAddedToParticipantsList(teamOwnerB?.name ?: "")
                tapCloseButtonOnGroupConversationDetailsPage()
                iSeeSystemMessage("You added ${teamOwnerB?.name ?: ""} to the conversation")
            }
        }

        step("Start call instances and auto-accept incoming call") {
            runBlocking {
                callHelper.userXStartsInstance(
                    "user2Name, user3Name, user4Name, user5Name",
                    "Chrome"
                )

//                callHelper.userXAcceptsNextIncomingCallAutomatically(
//                    "user2Name, user3Name, user4Name, user5Name"
                callHelper.userXAcceptsNextIncomingCallAutomatically(
                    "user2Name, user3Name"
                )
            }

            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }
        }

        step("Verify group call is active and participants joined") {
//            runBlocking {
//                callHelper.userVerifiesCallStatusToUserY(
//                    "user2Name, user3Name, user4Name, user5Name",
//                    "active",
//                    90
//                )
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user2Name, user3Name",
                    "active",
                    90
                )
            }

            pages.callingPage.apply {
                iSeeOngoingGroupCall()
            }

            callHelper.iSeeParticipantsInGroupCall("user2Name, user3Name")
        }

        step("Turn camera on") {
            pages.callingPage.apply {
                iTurnCameraOn()
            }
        }

        step("Switch video on for participants and verify audio/video") {
            waitFor(2)

            runBlocking {
                val callParticipantsSwitchVideoOn =
                    teamHelper.usersManager.splitAliases("user2Name, user3Name")
                callingManager.switchVideoOn(callParticipantsSwitchVideoOn)

                val assertCallParticipantsReceiveAudioVideo =
                    teamHelper.usersManager.splitAliases("user2Name, user3Name")
                callingManager.verifyReceiveAudioAndVideo(assertCallParticipantsReceiveAudioVideo)


                callHelper.iSeeParticipantsInGroupVideoCall("user2Name, user3Name")
            }

            pages.callingPage.apply {
                iMinimiseOngoingCall()
            }
            pages.conversationViewPage.apply {
                tapMessageInInputField()

                tapPingButton()

                iSeePingModalWithText("Are you sure you want to ping 5 people?")

                tapPingButtonModal()

                iSeeSystemMessage("You pinged")

                closeKeyboardIfOpened()

//                        iTapFileSharingButton()
//                    tapSharingOption("Audio")

//                assertToastDisplayed("You can't record an audio message during a call.", trigger = {
//                    iTapFileSharingButton()
//                    tapSharingOption("Audio")
//                })

                assertToastDisplayed("You can't record an audio message during a call") {
                    iTapFileSharingButton()
                    tapSharingOption("Audio")
                }


                waitFor(10)
            }
        }
    }
}
