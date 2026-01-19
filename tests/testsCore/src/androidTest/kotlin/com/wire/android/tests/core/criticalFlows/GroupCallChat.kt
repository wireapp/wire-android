/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

import QrCodeTestUtils.createQrImageInDeviceDownloadsFolder
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
import uiautomatorutils.PermissionUtils.grantRuntimePermsForForegroundApp
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.getValue
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.tags.Tag

@RunWith(AndroidJUnit4::class)
class GroupCallChat : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper
    private val callHelper by lazy { CallHelper() }
    private lateinit var callingManager: CallingManager
    private var teamOwner: ClientUser? = null

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
        runCatching { teamOwner?.deleteTeam(backendClient) }
        deleteDownloadedFilesContaining("my-test-qr.png")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8602")
    @Category("criticalFlow")
    @Tag(key = "criticalFlow", value = "groupCallChat")
    @Test
    fun givenIStartGroupCall_whenParticipantShareMessageFileAndLocation_thenAllVisibleAndCallContinues() {
        step("Prepare team via backend (owner + members + conversation)") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "WeLikeCalling",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "WeLikeCalling",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            testServiceHelper.userHasGroupConversationInTeam(
                "user1Name",
                "GroupCallChat",
                "user2Name,user3Name",
                "WeLikeCalling"
            )

            testServiceHelper.addDevice("user3Name", null, "Device1")
        }

        step("Enable conference calling & start browser instances for participants") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "WeLikeCalling"
                )
                callHelper.userXStartsInstance(
                    "user2Name, user3Name",
                    "Chrome"
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
                enterTeamOwnerLoggingEmail(teamOwner?.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner?.password ?: "")
                clickLoginButton()
            }
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }
        step("Open GroupCallChat conversation") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupCallChat")
                clickGroupConversation("GroupCallChat")
            }
        }

        step("Prepare participants to auto-accept next incoming call") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name, user3Name")
            }
        }

        step("Start group call from GroupCallChats conversation") {
            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }
        }

        step("Verify group call is active and participants joined") {
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

        step("Unmute participants and verify audio is sent & received") {
            runBlocking {
                val callParticipants =
                    teamHelper.usersManager.splitAliases("user2Name, user3Name")
                callingManager.unmuteMicrophone(callParticipants)

                val callParticipantsAudio =
                    teamHelper.usersManager.splitAliases("user2Name, user3Name")
                callingManager.verifySendAndReceiveAudio(callParticipantsAudio)
            }
        }
        step("Minimise ongoing group call") {
            pages.callingPage.apply {
                iMinimiseOngoingCall()
            }
        }
        step("Participant sends a message to the group conversation") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                userSendMessageToConversation(
                    "user2Name",
                    "Hello Friends",
                    "Device1",
                    "GroupCallChat",
                    false
                )
            }
        }
        step("Verify message is visible in conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello Friends")
            }
        }
        step("Share QR code file in conversation and verify it is sent") {
            pages.conversationViewPage.apply {
                iTapFileSharingButton()
                assertSharingOptionVisible("File")
                assertSharingOptionVisible("Gallery")
                assertSharingOptionVisible("Camera")
                assertSharingOptionVisible("Video")
                assertSharingOptionVisible("Audio")
                assertSharingOptionVisible("Location")
                createQrImageInDeviceDownloadsFolder("my-test-qr")
                tapSharingOption("File")
            }
            pages.documentsUIPage.apply {
                iSeeQrCodeImage()
                iOpenDisplayedQrCodeImage()
                iTapSendButtonOnPreviewImage()
            }

            pages.conversationViewPage.apply {
                iSeeSentQrCodeImageInCurrentConversation()
            }
        }
        step("User shares default location to group conversation while the call continues") {
            testServiceHelper.userXSharesLocationTo(
                "user3Name",
                "GroupCallChat",
                "Device1",
                false
            )

            pages.conversationViewPage.apply {
                iSeeLocationMapContainer()
            }

            pages.callingPage.apply {
                iRestoreOngoingCall()
                iSeeOngoingGroupCall()
            }
            callHelper.iSeeParticipantsInGroupCall("user2Name, user3Name")
        }
    }
}
