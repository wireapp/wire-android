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
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import deleteDownloadedFilesContaining
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import service.TestServiceHelper
import uiautomatorutils.PermissionUtils.grantRuntimePermsForForegroundApp
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.getValue

@RunWith(AndroidJUnit4::class)
class GroupCallChat : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.Companion.create {
        modules(testModule)
    }
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice

    lateinit var callingManager: CallingManager

    lateinit var context: Context
    var teamOwner: ClientUser? = null
    var backendClient: BackendClient? = null
    var teamHelper: TeamHelper? = null
    val testServiceHelper by lazy {
        TestServiceHelper()
    }
    val callHelper by lazy {
        CallHelper()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()

        teamHelper?.let {
            callHelper.init(it.usersManager)
            callingManager = callHelper.callingManager
        } ?: throw IllegalArgumentException("Team Helper not initialized")

        grantRuntimePermsForForegroundApp(
            device,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )
    }

    @After
    fun tearDown() {
        // To delete team
        teamOwner?.deleteTeam(backendClient!!)
        deleteDownloadedFilesContaining("my-test-qr.png")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @Test
    fun givenIStartGroupCall_whenParticipantShareMessageFileAndLocation_thenAllVisibleAndCallContinues() {

        teamHelper?.usersManager!!.createTeamOwnerByAlias(
            "user1Name",
            "WeLikeCalling",
            "en_US",
            true,
            backendClient!!,
            context
        )
        teamOwner = teamHelper?.usersManager!!.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        teamHelper?.userXAddsUsersToTeam(
            "user1Name",
            "user2Name,user3Name",
            "WeLikeCalling",
            TeamRoles.Member,
            backendClient!!,
            context,
            true
        )

        testServiceHelper.userHasGroupConversationInTeam(
            "user1Name",
            "GroupCallChat",
            "user2Name,user3Name",
            "WeLikeCalling"
        )

        testServiceHelper.apply {
            addDevice("user3Name", null, "Device1")
        }

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
        pages.conversationListPage.apply {
            assertGroupConversationVisible("GroupCallChat")
            clickGroupConversation("GroupCallChat")
        }

        runBlocking {
            callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name, user3Name")
            pages.conversationViewPage.apply {
                iTapStartCallButton()
            }

            callHelper.userVerifiesCallStatusToUserY("user2Name, user3Name", "active", 90)
            pages.callingPage.apply {
                iSeeOngoingGroupCall()
            }
            callHelper.iSeeParticipantsInGroupCall("user2Name, user3Name")

            runBlocking {
                val callParticipants = teamHelper!!.usersManager.splitAliases("user2Name, user3Name")
                callingManager.unmuteMicrophone(callParticipants)

                val callParticipantsAudio = teamHelper!!.usersManager.splitAliases("user2Name, user3Name")
                callingManager.verifySendAndReceiveAudio(callParticipantsAudio)
            }
            pages.callingPage.apply {
                iMinimiseOngoingCall()
            }

            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                userSendMessageToConversation("user2Name", "Hello Friends", "Device1", "GroupCallChat", false)
            }
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello Friends")
                iTapFileSharingButton()
                assertSharingOptionVisible("File")
                assertSharingOptionVisible("Gallery")
                assertSharingOptionVisible("Camera")
                assertSharingOptionVisible("Video")
                assertSharingOptionVisible("Audio")
                assertSharingOptionVisible("Location")
                createQrImageInDeviceDownloadsFolder("my-test-qr")
                tapSharingOption("File")
                pages.documentsUIPage.apply {
                    iSeeQrCodeImage()
                    iOpenDisplayedQrCodeImage()
                    iTapSendButtonOnPreviewImage()
                }
                pages.conversationViewPage.apply {
                    iSeeSentQrCodeImageInCurrentConversation()
                }
                testServiceHelper.userXSharesLocationTo("user3Name", "GroupCallChat", "Device1", false)
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
}
