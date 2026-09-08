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

import QrCodeTestUtils.createQrImageInDeviceDownloadsFolder
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import createOneKbFileInDeviceDownloadsFolder
import deleteDownloadedFilesContaining
import getDownloadedFileNames
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser
import waitForNewDownloadedFileName

@RunWith(AndroidJUnit4::class)
class FileSharingTests : BaseUiTest() {
    private lateinit var member1: ClientUser
    private var downloadedFilesBeforeTest = emptySet<String>()

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        downloadedFilesBeforeTest = getDownloadedFileNames()
    }

    @After
    fun tearDown() {
        deleteDownloadedFilesContaining("File")
        deleteDownloadedFilesContaining("image")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4339")
    @Category("regression", "RC", "fileSharing")
    @Test
    fun givenTeamMember_whenReceivingAndDownloadingImage_thenImageIsSavedToDownloads() {
        var downloadedImageFileName = ""

        givenTeamMemberIsPreparedForFileSharing()
        loginAsTeamMemberAndOpenFileSharingConversation()

        step("When TeamOwner sends image testing.jpg to conversation SendFilesHere") {
            testServiceHelper.contactSendsLocalImageConversation(
                context,
                "testing.jpg",
                "user1Name",
                null,
                "SendFilesHere"
            )
        }

        step("Then I see an image in the conversation view") {
            pages.conversationViewPage.assertImageIsVisible()
        }

        step("When I open the image context menu") {
            pages.conversationViewPage.apply {
                tapImageMessage()
                assertImageContextMenuButtonVisible()
                tapImageContextMenuButton()
            }
        }

        step("Then I see Download and Delete options") {
            pages.conversationViewPage.assertImageContextMenuOptionsVisible()
        }

        step("When I tap Download option") {
            pages.conversationViewPage.tapDownloadButton()
        }

        step("Then I see the download confirmation and the image appears on the device") {
            pages.conversationViewPage.assertImageSavedToDownloadsToastVisible()
            downloadedImageFileName = waitForNewDownloadedFileName("image", downloadedFilesBeforeTest)
        }

        step("And I remove the downloaded image from the device") {
            deleteDownloadedFilesContaining(downloadedImageFileName)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4340")
    @Category("regression", "RC", "fileSharing")
    @Test
    fun givenTeamMember_whenReceivingTxtFile_thenICanDownloadIt() {
        var downloadedTextFileName = ""

        givenTeamMemberIsPreparedForFileSharing()

        step("And TeamOwner adds new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        loginAsTeamMemberAndOpenFileSharingConversation()

        step("When TeamOwner sends 1.00MB text file TextFile.txt via Device1 to conversation SendFilesHere") {
            testServiceHelper.contactSendsOneMbTextFileConversation(
                context,
                "TextFile.txt",
                "user1Name",
                "Device1",
                "SendFilesHere"
            )
        }

        step("Then I see file TextFile.txt in the conversation view") {
            pages.conversationViewPage.assertFileWithNameIsVisible("TextFile.txt")
        }

        step("When I open file TextFile.txt") {
            pages.conversationViewPage.clickFileWithName("TextFile.txt")
        }

        step("Then I see the file download alert with Open, Save and Cancel buttons") {
            pages.conversationViewPage.apply {
                assertFileActionModalIsVisible()
                assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
            }
        }

        step("When I save file TextFile.txt") {
            pages.conversationViewPage.clickSaveButtonOnDownloadModal()
        }

        step("Then I see the download confirmation and file TextFile.txt appears on the device") {
            pages.conversationViewPage.assertFileSavedToast(
                "The file TextFile.txt was saved successfully to the Downloads folder"
            )
            downloadedTextFileName = waitForNewDownloadedFileName("TextFile", downloadedFilesBeforeTest)
        }

        step("And I remove file TextFile.txt from the device") {
            deleteDownloadedFilesContaining(downloadedTextFileName)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4341")
    @Category("regression", "RC", "fileSharing")
    @Test
    fun givenTeamMember_whenReceivingVideo_thenICanDownloadIt() {
        var downloadedVideoFileName = ""

        givenTeamMemberIsPreparedForFileSharing()

        step("And TeamOwner adds new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        loginAsTeamMemberAndOpenFileSharingConversation()

        step("When TeamOwner sends video VideoFile.mp4 via Device1 to conversation SendFilesHere") {
            testServiceHelper.contactSendsLocalVideoConversation(
                context,
                "VideoFile.mp4",
                "user1Name",
                "Device1",
                "SendFilesHere"
            )
        }

        step("Then I see video file VideoFile.mp4 in the conversation view") {
            pages.conversationViewPage.assertFileWithNameIsVisible("VideoFile.mp4")
        }

        step("When I tap the download button on video file VideoFile.mp4") {
            pages.conversationViewPage.tapDownloadButtonOnVideoFile()
        }

        step("Then I see the file download alert with Open, Save and Cancel buttons") {
            pages.conversationViewPage.apply {
                assertFileActionModalIsVisible()
                assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
            }
        }

        step("When I save video file VideoFile.mp4") {
            pages.conversationViewPage.clickSaveButtonOnDownloadModal()
        }

        step("Then I see the download confirmation and video VideoFile.mp4 appears on the device") {
            pages.conversationViewPage.assertFileSavedToast(
                "The file VideoFile.mp4 was saved successfully to the Downloads folder"
            )
            downloadedVideoFileName = waitForNewDownloadedFileName("VideoFile", downloadedFilesBeforeTest)
        }

        step("And I remove video file VideoFile.mp4 from the device") {
            deleteDownloadedFilesContaining(downloadedVideoFileName)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4342")
    @Category("regression", "RC", "fileSharing")
    @Test
    fun givenTeamMember_whenSendingImageInGroupConversation_thenImageIsSent() {
        givenTeamMemberIsPreparedForFileSharing()

        step("And TeamOwner adds new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        loginAsTeamMemberAndOpenFileSharingConversation()

        step("When I open the file sharing options") {
            pages.conversationViewPage.iTapFileSharingButton()
        }

        step("Then I see File, Gallery, Camera, Video and Audio sharing options") {
            pages.conversationViewPage.apply {
                assertSharingOptionVisible("File")
                assertSharingOptionVisible("Gallery")
                assertSharingOptionVisible("Camera")
                assertSharingOptionVisible("Video")
                assertSharingOptionVisible("Audio")
            }
        }

        step("When I create an image containing an Image QR code and open Gallery") {
            createQrImageInDeviceDownloadsFolder("ImageFile")
            pages.conversationViewPage.tapSharingOption("Gallery")
        }

        step("And I select the most recent image in the photo picker") {
            pages.documentsUIPage.apply {
                selectMostRecentImageInPhotoPicker()
                tapAddOrDoneButtonIfVisible()
            }
        }

        step("And I see the image preview page and send the image") {
            pages.documentsUIPage.apply {
                assertImagePreviewPageVisible()
                iTapSendButtonOnPreviewImage()
            }
        }

        step("Then I see the sent Image QR code in the conversation view") {
            pages.conversationViewPage.iSeeSentQrCodeImageInCurrentConversation()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4343")
    @Category("regression", "RC", "smoke", "fileSharing", "smokeSchwarz", "smokeSTACKIT")
    @Test
    fun givenTeamMember_whenSendingFileInGroupConversation_thenFileIsSent() {
        givenTeamMemberIsPreparedForFileSharing()

        step("And TeamOwner adds new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        loginAsTeamMemberAndOpenFileSharingConversation()

        step("And I open the file sharing options") {
            pages.conversationViewPage.iTapFileSharingButton()
        }

        step("When I create a 1 KB TextFile.txt in device storage and open the File picker") {
            createOneKbFileInDeviceDownloadsFolder("TextFile.txt")
            pages.conversationViewPage.tapSharingOption("File")
        }

        step("And I select TextFile.txt in DocumentsUI") {
            pages.documentsUIPage.selectFileInDocumentsUI("TextFile.txt")
        }

        step("And I see TextFile.txt on the preview page and send it") {
            pages.documentsUIPage.apply {
                assertFilePreviewPageVisible("TextFile.txt")
                iTapSendButtonOnPreviewImage()
            }
        }

        step("Then I see file TextFile.txt in the conversation view") {
            pages.conversationViewPage.assertFileWithNameIsVisible("TextFile.txt")
        }
    }

    // Shared backend setup for file-sharing tests: prepares TeamOwner, Member1 and their group conversation.
    private fun givenTeamMemberIsPreparedForFileSharing() {
        step("Given There is a team owner TeamOwner with team FileSharing") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "FileSharing",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner adds Member1 to team FileSharing with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "FileSharing",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And TeamOwner has group conversation SendFilesHere with Member1 in team FileSharing") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SendFilesHere",
                "user2Name",
                "FileSharing"
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }
    }

    // Shared app flow for file-sharing tests: logs in as Member1 and opens the SendFilesHere conversation.
    private fun loginAsTeamMemberAndOpenFileSharingConversation() {
        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(member1.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(member1.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation SendFilesHere in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("SendFilesHere")
        }

        step("And I open conversation SendFilesHere") {
            pages.conversationListPage.clickGroupConversation("SendFilesHere")
        }
    }
}
