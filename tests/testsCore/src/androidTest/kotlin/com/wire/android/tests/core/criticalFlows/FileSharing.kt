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
import backendconnections.team.TeamHelper
import com.wire.android.testSupport.backendConnections.BackendClient
import com.wire.android.testSupport.backendConnections.team.TeamRoles
import com.wire.android.testSupport.uiautomatorutils.UiAutomatorSetup
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import deleteDownloadedFilesByBaseName
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
import user.usermanager.ClientUserManager
import user.utils.ClientUser


@RunWith(AndroidJUnit4::class)
class FileSharing : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.Companion.create {
        modules(testModule)
    }
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
        // device = UiAutomatorSetup.start(UiAutomatorSetup.APP_DEV)
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_STAGING)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
    }

    @After
    fun tearDown() {
        //  UiAutomatorSetup.stopApp()
        // To delete team member
        // registeredUser?.deleteTeamMember(backendClient!!, teamMember?.getUserId().orEmpty())
        // To delete team
        // teamOwner2?.deleteTeam(backendClient!!)
        // teamOwner1?.deleteTeam(backendClient!!)
        deleteDownloadedFilesByBaseName("FileName")


    }

    @Suppress("LongMethod")
    @Test
    fun fileSharingFeature() {

        teamHelper?.usersManager!!.createTeamOwnerByAlias(
            "user3Name",
            "receiveTeam",
            "en_US",
            true,
            backendClient!!,
            context
        )

        teamHelper?.usersManager!!.createTeamOwnerByAlias(
            "user1Name",
            "sendTeam",
            "en_US",
            true,
            backendClient!!,
            context
        )

        teamOwner2 = teamHelper?.usersManager!!.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)

        teamHelper?.userXAddsUsersToTeam(
            "user3Name",
            "user4Name",
            "receiveTeam",
            TeamRoles.Member,
            backendClient!!,
            context,
            true
        )

        teamOwner1 = teamHelper?.usersManager!!.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)

        teamHelper?.userXAddsUsersToTeam(
            "user1Name",
            "user2Name",
            "sendTeam",
            TeamRoles.Member,
            backendClient!!,
            context,
            true
        )

        val connectionSenderFromSendTeam = teamHelper?.usersManager!!.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
        val connectionReceiverFromReceiveTeam = teamHelper?.usersManager!!.findUserBy("user4Name", ClientUserManager.FindBy.NAME_ALIAS)

        pages.registrationPage.apply {
            assertEmailWelcomePage()
        }
        pages.loginPage.apply {
            enterPersonalUserLoggingEmail(connectionReceiverFromReceiveTeam.email ?: "")
            clickLoginButton()
            enterPersonalUserLoginPassword(connectionReceiverFromReceiveTeam.password ?: "")
            clickLoginButton()
        }
        pages.registrationPage.apply {

            waitUntilLoginFlowIsComplete()
            clickAllowNotificationButton()

            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                connectionRequestIsSentTo("user2Name", "user4Name")
                runBlocking { usersSetUniqueUsername("user4Name") }
            }

            clickDeclineShareDataAlert()
            pages.conversationPage.apply {
                assertConnectionRequestNameIs(connectionSenderFromSendTeam.name ?: "")
                clickConnectionRequestOfUser(connectionSenderFromSendTeam.name ?: "")
            }
            pages.unconnectedUserProfilePage.apply {
                assertConnectionRequestNotificationTextIsDisplayed()
                assertAcceptButtonIsDisplayed()
                assertIgnoreButtonIsDisplayed()
                clickAcceptButton()
            }
            pages.connectedUserProfilePage.apply {
                Thread.sleep(1000)
                assertToastMessageIsDisplayed("Connection request accepted")
                //Thread.sleep(5000)
                clickStartConversationButton()
                pages.conversationPage.apply {
                    assertConversationIsVisibleWithTeamMember(connectionSenderFromSendTeam.name ?: "")

                    //send Audio File
                    testServiceHelper.contactSendsLocalAudioPersonalMLSConversation(
                        context,
                        "FileName",
                        "user2Name",
                        "Device1",
                        "user4Name"
                    )
                    //Thread.sleep(20000)

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
                    assertFileSavedToastContain("The file FileName.mp3 was saved successfully to the Downloads folder")

                    pages.conversationPage.apply {

                        //send image File
                        testServiceHelper.contactSendsLocalImagePersonalMLSConversation(
                            context,
                            "FileName2",
                            "user2Name",
                            "Device1",
                            "user4Name"
                        )

                        assertImageFileWithNameIsVisible("FileName2")
                        clickFileWithName("FileName2")
                        assertFileActionModalIsVisible()
                        assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
                        clickSaveButtonOnDownloadModal()
                        assertFileSavedToastContain("The file FileName2.jpg was saved successfully to the Downloads folder")
                        //send text File
                        testServiceHelper.contactSendsLocalTextPersonalMLSConversation(
                            context,
                            "FileName3",
                            "user2Name",
                            "Device1",
                            "user4Name"
                        )
                    }
                    assertTextFileWithNameIsVisible("FileName3")
                    clickTextFileWithName("FileName3")
                    assertFileActionModalIsVisible()
                    assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
                    clickSaveButtonOnDownloadModal()
                    assertFileSavedToastContain("The file FileName3.txt was saved successfully to the Downloads folder")

                    pages.connectedUserProfilePage.apply {

                        //send video File
                        testServiceHelper.contactSendsLocalVideoPersonalMLSConversation(
                            context,
                            "FileName4",
                            "user2Name",
                            "Device1",
                            "user4Name"
                        )
                    }
                    scrollToBottomOfConversationScreen()
                    assertTextFileWithNameIsVisible("FileName4")
                    tapDownloadButtonOnVideoFile()
                   // clickTextFileWithName("Tap to download")

                  //  Thread.sleep(7_000)

                    assertFileActionModalIsVisible()
                    assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
                    clickSaveButtonOnDownloadModal()
                    assertFileSavedToastContain("The file FileName4.mp4 was saved successfully to the Downloads folder")

                }
            }
        }
    }
}


