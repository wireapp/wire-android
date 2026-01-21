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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import backendUtils.team.deleteTeam
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.core.pages.AllPages
import deleteDownloadedFilesContaining
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import service.TestServiceHelper
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import uiautomatorutils.UiWaitUtils.WaitUtils.waitFor

@RunWith(AndroidJUnit4::class)
class FileSharingBetweenTeams : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper
    private var teamOwner1: ClientUser? = null
    private var teamOwner2: ClientUser? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
        testServiceHelper = TestServiceHelper(teamHelper.usersManager)
    }

    @After
    fun tearDown() {
        runCatching { teamOwner1?.deleteTeam(backendClient) }
        runCatching { teamOwner2?.deleteTeam(backendClient) }
        deleteDownloadedFilesContaining("File")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8603")
    @Category("criticalFlow")
    @Test
    fun givenUserInAnotherTeam_whenFileIsSent_thenRecipientCanReceivePlayAndDownloadIt() {
        step("Prepare team via backend (sender team + receiver team) with owners and members") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "sendTeam",
                "en_US",
                true,
                backendClient,
                context
            )

            teamHelper.usersManager.createTeamOwnerByAlias(
                "user3Name",
                "receiveTeam",
                "en_US",
                true,
                backendClient,
                context
            )

            teamHelper.userXAddsUsersToTeam(
                "user3Name",
                "user4Name",
                "receiveTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "sendTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner1 = teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            teamOwner2 = teamHelper.usersManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        val connectionSenderFromSendTeam =
            teamHelper.usersManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
        val connectionReceiverFromReceiveTeam =
            teamHelper.usersManager.findUserBy("user4Name", ClientUserManager.FindBy.NAME_ALIAS)

        step("Login as receiver team member in Android app") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(connectionReceiverFromReceiveTeam.email ?: "")
                clickLoginButton()
                enterPersonalUserLoginPassword(connectionReceiverFromReceiveTeam.password ?: "")
                clickLoginButton()
            }
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Register sender device and send connection request to receiver via backend") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                connectionRequestIsSentTo("user2Name", "user4Name")
                runBlocking { usersSetUniqueUsername("user4Name") }
            }
        }

        step("Assert sender connection request is visible and open the request from conversation list") {
            pages.conversationListPage.apply {
                assertConnectionRequestNameIs(connectionSenderFromSendTeam.name ?: "")
                clickConnectionRequestOfUser(connectionSenderFromSendTeam.name ?: "")
            }
        }

        step("Verify connection request UI and accept the request") {
            pages.unconnectedUserProfilePage.apply {
                assertConnectionRequestNotificationTextIsDisplayed()
                assertAcceptButtonIsDisplayed()
                assertIgnoreButtonIsDisplayed()
                clickAcceptButton()
            }
        }

        step("Verify connection accepted toast and start a conversation with sender") {
            pages.connectedUserProfilePage.apply {
                assertToastMessageIsDisplayed("Connection request accepted")
                clickStartConversationButton()
            }
        }

        step("Assert conversation with sender is visible in conversation view") {
            pages.conversationViewPage.apply {
                assertConversationIsVisibleWithTeamMember(connectionSenderFromSendTeam.name ?: "")
            }
        }

        // ---------- AUDIO ----------

        step("Receive and assert audio file message in conversation") {
            pages.conversationViewPage.apply {
                testServiceHelper.contactSendsLocalAudioPersonalMLSConversation(
                    context,
                    "AudioFile",
                    "user2Name",
                    "Device1",
                    "user4Name"
                )
                waitFor(5)
                assertAudioMessageIsVisible()
                assertAudioTimeStartsAtZero()
            }
        }

        step("Play audio message and verify playback time progresses") {
            pages.conversationViewPage.apply {
                clickPlayButtonOnAudioMessage()
                waitFor(18)
                clickPauseButtonOnAudioMessage()
                assertAudioTimeIsNotZeroAnymore()
            }
        }

        step("Open audio message actions and verify bottom sheet options") {
            pages.conversationViewPage.apply {
                longPressOnAudioSeekBar()
                assertBottomSheetIsVisible()
                assertBottomSheetButtonsVisible_ReactionsDetailsReplyDownloadShareOpenDelete()
            }
        }

        step("Download audio file and verify success toast") {
            pages.conversationViewPage.apply {
                tapDownloadButton()
                assertFileActionModalIsVisible()
                tapSaveButtonOnModal()
                assertFileSavedToastContain(
                    "The file AudioFile.mp3 was saved successfully to the Downloads folder"
                )
            }
        }

        // ---------- IMAGE ----------

        step("Receive image file and open download modal") {
            pages.conversationViewPage.apply {
                testServiceHelper.contactSendsLocalImagePersonalMLSConversation(
                    context,
                    "ImageFile",
                    "user2Name",
                    "Device1",
                    "user4Name"
                )
                assertImageFileWithNameIsVisible("ImageFile")
                clickFileWithName("ImageFile")
                assertFileActionModalIsVisible()
                assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
            }
        }

        step("Download image file and verify success toast") {
            pages.conversationViewPage.apply {
                clickSaveButtonOnDownloadModal()
                assertFileSavedToastContain(
                    "The file ImageFile.jpg was saved successfully to the Downloads folder"
                )
            }
        }

        // ---------- TEXT ----------

        step("Receive text file and open download modal") {
            pages.conversationViewPage.apply {
                testServiceHelper.contactSendsLocalTextPersonalMLSConversation(
                    context,
                    "TextFile",
                    "user2Name",
                    "Device1",
                    "user4Name"
                )
                assertFileWithNameIsVisible("TextFile")
                clickTextFileWithName("TextFile")
                assertFileActionModalIsVisible()
                assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
            }
        }

        step("Download text file and verify success toast") {
            pages.conversationViewPage.apply {
                clickSaveButtonOnDownloadModal()
                assertFileSavedToastContain(
                    "The file TextFile.txt was saved successfully to the Downloads folder"
                )
            }
        }

        // ---------- VIDEO ----------

        step("Receive video file message in conversation") {
            pages.conversationViewPage.apply {
                testServiceHelper.contactSendsLocalVideoPersonalMLSConversation(
                    context,
                    "VideoFile",
                    "user2Name",
                    "Device1",
                    "user4Name"
                )
            }
        }

        step("Scroll to latest messages and verify video file is visible") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertFileWithNameIsVisible("VideoFile")
            }
        }

        step("Open video download modal and verify available actions") {
            pages.conversationViewPage.apply {
                tapDownloadButtonOnVideoFile()
                assertFileActionModalIsVisible()
                assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
            }
        }

        step("Save video file and verify success toast") {
            pages.conversationViewPage.apply {
                clickSaveButtonOnDownloadModal()
                assertFileSavedToastContain(
                    "The file VideoFile.mp4 was saved successfully to the Downloads folder"
                )
            }
        }

        step("Play video file and verify it opens outside Wire") {
            pages.conversationViewPage.apply {
                tapToPlayVideoFile()
                clickOpenButtonOnDownloadModal()
                assertWireAppIsNotInForeground()
            }
        }
    }
}
