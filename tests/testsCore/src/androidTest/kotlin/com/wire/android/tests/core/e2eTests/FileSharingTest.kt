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

import QrCodeTestUtils
import TextFileTestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import deleteDownloadedFilesContaining
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class FileSharingTest : BaseUiTest() {

    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, currentUser) }
        deleteDownloadedFilesContaining(QR_CODE_TEXT)
        deleteDownloadedFilesContaining(TEXT_DOCUMENT_FILE_NAME)
    }

    @TestCaseId("TC-4342")
    @Category("fileSharing", "regression", "RC")
    @Test
    fun givenGroupConversation_whenSendingImageFromDocumentsUi_thenImageIsVisibleInConversation() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupConversation()

        step("Create QR image in device Downloads folder") {
            QrCodeTestUtils.createQrImageInDeviceDownloadsFolder(QR_CODE_TEXT)
        }

        step("Open attachment sheet and verify sharing options") {
            pages.conversationViewPage.apply {
                iTapFileSharingButton()
                SHARING_OPTIONS.forEach(::assertSharingOptionVisible)
            }
        }

        step("Attach QR image from DocumentsUI") {
            pages.conversationViewPage.tapSharingOption(GALLERY_OPTION)
            pages.documentsUIPage.apply {
                selectFirstVisiblePhotoInPhotoPicker()
                confirmPhotoPickerSelection()
                iTapSendButtonOnPreviewImage()
            }
        }

        step("Verify sent image is visible in the conversation") {
            pages.conversationViewPage.iSeeSentQrCodeImageInCurrentConversation()
        }
    }

    @TestCaseId("TC-4339")
    @Category("fileSharing", "regression", "RC")
    @Test
    fun givenGroupConversation_whenReceivingImage_thenImageCanBeDownloaded() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupConversation()

        step("Receive image file and open download modal") {
            testServiceHelper.apply {
                addDevice(TEAM_OWNER_ALIAS, null, DEVICE_NAME)
                contactSendsLocalImageConversation(context, IMAGE_FILE_NAME, TEAM_OWNER_ALIAS, DEVICE_NAME, GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.apply {
                assertImageFileWithNameIsVisible(IMAGE_FILE_NAME)
                clickFileWithName(IMAGE_FILE_NAME)
                assertFileActionModalIsVisible()
                assertDownloadModalButtonsAreVisible_Open_Save_Cancel()
            }
        }

        step("Download image file and verify success toast") {
            pages.conversationViewPage.apply {
                waitForPreviousFileSavedToastToDisappear()
                clickSaveButtonOnDownloadModal()
                assertFileSavedToast("The file ImageFile.jpg was saved successfully to the Downloads folder")
            }
        }
    }

    @TestCaseId("TC-4340")
    @Category("fileSharing", "regression", "RC")
    @Test
    fun givenGroupConversation_whenReceivingTextFile_thenFileIsVisible() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupConversation()

        step("Receive text file and verify it is visible") {
            testServiceHelper.apply {
                addDevice(TEAM_OWNER_ALIAS, null, DEVICE_NAME)
                contactSendsLocalTextConversation(context, TEXT_FILE_NAME, TEAM_OWNER_ALIAS, DEVICE_NAME, GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.assertFileWithNameIsVisible(TEXT_FILE_NAME)
        }
    }

    @TestCaseId("TC-4341")
    @Category("fileSharing", "regression", "RC")
    @Test
    fun givenGroupConversation_whenReceivingVideo_thenVideoIsVisible() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupConversation()

        step("Receive video file and verify it is visible") {
            testServiceHelper.apply {
                addDevice(TEAM_OWNER_ALIAS, null, DEVICE_NAME)
                contactSendsLocalVideoConversation(context, VIDEO_FILE_NAME, TEAM_OWNER_ALIAS, DEVICE_NAME, GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.apply {
                assertFileWithNameIsVisible(VIDEO_FILE_NAME)
            }
        }
    }

    @TestCaseId("TC-4343")
    @Category("fileSharing", "regression", "RC", "smoke")
    @Test
    fun givenGroupConversation_whenSendingTextFileFromDocumentsUi_thenFileIsVisibleInConversation() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupConversation()

        step("Create text file in device Downloads folder") {
            TextFileTestUtils.createTextFileInDeviceDownloadsFolder(
                TEXT_DOCUMENT_FILE_NAME,
                "Wire UIAutomator text attachment"
            )
        }

        step("Attach text file from DocumentsUI") {
            pages.conversationViewPage.apply {
                iTapFileSharingButton()
                tapSharingOption(FILE_OPTION)
            }
            pages.documentsUIPage.apply {
                selectFileFromDownloads(TEXT_DOCUMENT_FILE_NAME)
                iTapSendButtonOnPreviewImage()
            }
        }

        step("Verify sent text file is visible in the conversation") {
            pages.conversationViewPage.assertFileWithNameIsVisible(TEXT_DOCUMENT_FILE_NAME)
        }
    }

    private fun prepareGroupConversation() {
        step("Prepare backend team owner, member, and group conversation") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                TEAM_OWNER_ALIAS,
                TEAM_NAME,
                "en_US",
                true,
                backendClient,
                context
            )
            teamHelper.userXAddsUsersToTeam(
                TEAM_OWNER_ALIAS,
                TEAM_MEMBER_ALIAS,
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                TEAM_MEMBER_ALIAS,
                TEAM_NAME
            )
            currentUser = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser() {
        step("Login team member via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamOwnerLoggingEmail(currentUser?.email.orEmpty())
                clickLoginButton()
                enterTeamOwnerLoggingPassword(currentUser?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("Complete login flow") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun openGroupConversation() {
        step("Open group conversation") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(GROUP_CONVERSATION_NAME)
                clickGroupConversation(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.assertConversationScreenVisible()
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "FileSharing"
        const val GROUP_CONVERSATION_NAME = "SendFilesHere"
        const val QR_CODE_TEXT = "Image"
        const val QR_CODE_FILE_NAME = "$QR_CODE_TEXT.png"
        const val TEXT_DOCUMENT_FILE_NAME = "WireTextAttachment.txt"
        const val FILE_OPTION = "File"
        const val GALLERY_OPTION = "Gallery"
        const val DEVICE_NAME = "Device1"
        const val IMAGE_FILE_NAME = "ImageFile"
        const val TEXT_FILE_NAME = "TextFile"
        const val VIDEO_FILE_NAME = "VideoFile"
        val SHARING_OPTIONS = listOf(FILE_OPTION, GALLERY_OPTION, "Camera", "Video", "Audio")
    }
}
