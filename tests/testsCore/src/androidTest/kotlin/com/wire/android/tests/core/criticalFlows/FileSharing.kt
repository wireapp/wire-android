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

@RunWith(AndroidJUnit4::class)
class FileSharing : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    lateinit var context: Context
    var teamOwner2: ClientUser? = null
    var teamOwner1: ClientUser? = null
    var backendClient: BackendClient? = null
    var teamHelper: TeamHelper? = null
    val testServiceHelper by lazy {
        TestServiceHelper()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
    }

    @After
    fun tearDown() {
        //  UiAutomatorSetup.stopApp()
        // To delete team
        teamOwner2?.deleteTeam(backendClient!!)
        teamOwner1?.deleteTeam(backendClient!!)
        deleteDownloadedFilesContaining("File")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8603")
    @Category("criticalFlow")
    @Test
    fun givenUserInAnotherTeam_whenFileIsSent_thenRecipientCanReceivePlayAndDownloadIt() {

        teamHelper?.usersManager!!.createTeamOwnerByAlias(
            "user1Name",
            "sendTeam",
            "en_US",
            true,
            backendClient!!,
            context
        )

        teamHelper?.usersManager!!.createTeamOwnerByAlias(
            "user3Name",
            "receiveTeam",
            "en_US",
            true,
            backendClient!!,
            context
        )

        teamHelper?.userXAddsUsersToTeam(
            "user3Name",
            "user4Name",
            "receiveTeam",
            TeamRoles.Member,
            backendClient!!,
            context,
            true
        )

        teamHelper?.userXAddsUsersToTeam(
            "user1Name",
            "user2Name",
            "sendTeam",
            TeamRoles.Member,
            backendClient!!,
            context,
            true
        )

        teamOwner1 = teamHelper?.usersManager!!.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        teamOwner2 = teamHelper?.usersManager!!.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)

        val connectionSenderFromSendTeam =
            teamHelper?.usersManager!!.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
        val connectionReceiverFromReceiveTeam =
            teamHelper?.usersManager!!.findUserBy("user4Name", ClientUserManager.FindBy.NAME_ALIAS)

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
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                connectionRequestIsSentTo("user2Name", "user4Name")
                runBlocking { usersSetUniqueUsername("user4Name") }
            }

            pages.conversationListPage.apply {
                assertConnectionRequestNameIs(connectionSenderFromSendTeam.name ?: "")
                clickConnectionRequestOfUser(connectionSenderFromSendTeam.name ?: "")
            }
        }
        pages.unconnectedUserProfilePage.apply {
            assertConnectionRequestNotificationTextIsDisplayed()
            assertAcceptButtonIsDisplayed()
            assertIgnoreButtonIsDisplayed()
            clickAcceptButton()
        }

        pages.connectedUserProfilePage.apply {
            assertToastMessageIsDisplayed("Connection request accepted")
            clickStartConversationButton()
            pages.conversationViewPage.apply {
                assertConversationIsVisibleWithTeamMember(connectionSenderFromSendTeam.name ?: "")

                // send Audio File
                testServiceHelper.contactSendsLocalAudioPersonalMLSConversation(
                    context,
                    "AudioFile",
                    "user2Name",
                    "Device1",
                    "user4Name"
                )
                assertAudioMessageIsVisible()
                assertAudioTimeStartsAtZero()
                clickPlayButtonOnAudioMessage()
                Thread.sleep(16_000)
                clickPauseButtonOnAudioMessage()
                assertAudioTimeIsNotZeroAnymore()
                longPressOnAudioSeekBar()
                assertBottomSheetIsVisible()
                assertBottomSheetButtonsVisible_ReactionsDetailsReplyDownloadShareOpenDelete()
                tapDownloadButton()
                assertFileActionModalIsVisible()
                tapSaveButtonOnModal()
                assertFileSavedToastContain("The file AudioFile.mp3 was saved successfully to the Downloads folder")
            }
            pages.conversationViewPage.apply {
                // send image File
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
                clickSaveButtonOnDownloadModal()
                assertFileSavedToastContain("The file ImageFile.jpg was saved successfully to the Downloads folder")

                // send text File
                testServiceHelper.contactSendsLocalTextPersonalMLSConversation(
                    context,
                    "TextFile",
                    "user2Name",
                    "Device1",
                    "user4Name"
                )

                assertTextFileWithNameIsVisible("TextFile")
                clickTextFileWithName("TextFile")
                assertFileActionModalIsVisible()
                assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
                clickSaveButtonOnDownloadModal()
                assertFileSavedToastContain("The file TextFile.txt was saved successfully to the Downloads folder")

                // send video File
                testServiceHelper.contactSendsLocalVideoPersonalMLSConversation(
                    context,
                    "VideoFile",
                    "user2Name",
                    "Device1",
                    "user4Name"
                )

                scrollToBottomOfConversationScreen()
                assertTextFileWithNameIsVisible("VideoFile")
                tapDownloadButtonOnVideoFile()
                assertFileActionModalIsVisible()
                assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
                clickSaveButtonOnDownloadModal()
                assertFileSavedToastContain("The file VideoFile.mp4 was saved successfully to the Downloads folder")
                tapToPlayVideoFile()
                clickOpenButtonOnDownloadModal()
                assertWireAppIsNotInForeground()
            }
        }
    }
}
