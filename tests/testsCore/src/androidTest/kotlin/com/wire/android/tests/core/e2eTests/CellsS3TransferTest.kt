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

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import createDeterministicFile
import deleteAppExternalFilesContaining
import java.io.File
import java.time.Duration as JavaDuration
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import waitUntilAppExternalFileIsAbsent
import waitUntilAppExternalFileIsPartial
import waitUntilAppExternalFileMatches
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class CellsS3TransferTest : BaseUiTest() {
    private lateinit var receiver: ClientUser
    private var sourceFile: File? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @After
    fun tearDown() {
        sourceFile?.delete()
        deleteAppExternalFilesContaining(UiAutomatorSetup.appPackage, FILE_PREFIX)
    }

    @Category("cells", "regression", "RC")
    @Test
    fun givenSmallCellsAttachment_whenReceiverDownloadsIt_thenContentMatchesExactly() {
        val conversationName = "Cells S3 small transfer"
        val testFile = createTestFile(
            fileName = "$FILE_PREFIX-small ü.bin",
            size = SMALL_FILE_SIZE,
        )

        step("Prepare a Cells conversation created by the sender's test-service device") {
            prepareCellsConversation(conversationName)
        }

        step("Login as the receiver in the Android app") {
            loginUser(receiver)
        }

        step("Send the small Cells attachment from the sender's test-service device") {
            sendFileFromTestService(testFile.sourceFile, conversationName)
        }

        step("Open Shared Drive as the receiver and start the remote download") {
            openSharedDrive(conversationName)
            waitUntilAppExternalFileIsAbsent(
                appPackage = UiAutomatorSetup.appPackage,
                fileName = testFile.fileName,
                timeout = 5.seconds,
            )
            pages.cellsPage.tapFile(testFile.fileName)
        }

        step("Verify the remotely downloaded bytes, size, and SHA-256") {
            waitUntilAppExternalFileMatches(
                appPackage = UiAutomatorSetup.appPackage,
                expected = testFile,
                timeout = SMALL_DOWNLOAD_TIMEOUT,
            )
        }
    }

    @Suppress("LongMethod")
    @Category("cells", "regression", "RC")
    @Test
    fun givenMultipartCellsAttachment_whenDownloadIsCancelledAndRetried_thenContentMatchesExactly() {
        val conversationName = "Cells S3 multipart transfer"
        val testFile = createTestFile(
            fileName = "$FILE_PREFIX-multipart.bin",
            size = MULTIPART_FILE_SIZE,
        )

        step("Prepare a Cells conversation created by the sender's test-service device") {
            prepareCellsConversation(conversationName)
        }

        step("Login as the receiver in the Android app") {
            loginUser(receiver)
        }

        step("Send a Cells attachment one byte above the multipart threshold from the test-service device") {
            sendFileFromTestService(testFile.sourceFile, conversationName)
        }

        step("Open the conversation Shared Drive as the receiver") {
            openSharedDrive(conversationName)
        }

        step("Start the direct download, cancel it, and verify the partial file is deleted") {
            pages.cellsPage.apply {
                tapFile(testFile.fileName)
                assertDownloadStarted(timeout = 30.seconds)
            }
            waitUntilAppExternalFileIsPartial(
                appPackage = UiAutomatorSetup.appPackage,
                expected = testFile,
                timeout = 30.seconds,
            )
            pages.cellsPage.apply {
                cancelActiveDownload()
                assertDownloadCancelled()
            }
            waitUntilAppExternalFileIsAbsent(
                appPackage = UiAutomatorSetup.appPackage,
                fileName = testFile.fileName,
                timeout = 30.seconds,
            )
        }

        step("Retry the direct download and wait until it is ready") {
            pages.cellsPage.apply {
                tapFile(testFile.fileName)
                assertDownloadStarted(timeout = 30.seconds)
                assertFileReadyToOpen(timeout = MULTIPART_DOWNLOAD_TIMEOUT)
            }
        }

        step("Verify the multipart round-trip bytes, size, and SHA-256") {
            waitUntilAppExternalFileMatches(
                appPackage = UiAutomatorSetup.appPackage,
                expected = testFile,
                timeout = 2.minutes,
            )
        }
    }

    private fun prepareCellsConversation(conversationName: String) {
        backendSetupHelper.createTeamOwnerByAlias(
            "user1Name",
            TEAM_NAME,
            "en_US",
            true,
            backendClient,
            context,
        )
        backendSetupHelper.userXAddsUsersToTeam(
            "user1Name",
            "user2Name",
            TEAM_NAME,
            TeamRoles.Member,
            backendClient,
            context,
            true,
        )
        backendSetupHelper.enableCellsFeature("user1Name", TEAM_NAME, backendClient)
        receiver = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
        testServiceHelper.addDevice("user1Name", null, TEST_SERVICE_DEVICE)
        testServiceHelper.userCreatesGroupConversation(
            ownerAlias = "user1Name",
            participantsAliases = "user2Name",
            chatName = conversationName,
            deviceName = TEST_SERVICE_DEVICE,
            cellsEnabled = true,
        )
    }

    private fun createTestFile(fileName: String, size: Long) =
        createDeterministicFile(context.cacheDir, fileName, size).also { sourceFile = it.sourceFile }

    private fun sendFileFromTestService(file: File, conversationName: String) {
        testServiceHelper.contactSendsLocalFileConversation(
            file = file,
            senderAlias = "user1Name",
            deviceName = TEST_SERVICE_DEVICE,
            dstConvoName = conversationName,
            mimeType = "application/octet-stream",
            requestTimeout = TEST_SERVICE_REQUEST_TIMEOUT,
        )
    }

    private fun loginUser(user: ClientUser) {
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
            enterUserIdentifier(user.email.orEmpty())
            clickLoginButton()
            enterUserPassword(user.password.orEmpty())
            clickLoginButton()
        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsCompleted()
            clickAllowNotificationButton()
            clickDeclineShareDataAlert()
            assertConversationPageVisible()
        }
    }

    private fun openConversation(conversationName: String) {
        pages.conversationListPage.clickGroupConversation(conversationName, timeout = 2.minutes)
        pages.conversationViewPage.assertGroupConversationInForeground(conversationName)
    }

    private fun openSharedDrive(conversationName: String) {
        openConversation(conversationName)
        pages.conversationViewPage.clickOnGroupConversationDetails(conversationName)
        pages.groupConversationDetailsPage.apply {
            assertGroupDetailsPageVisible()
            tapSharedDriveButton()
        }
        pages.cellsPage.assertSharedDriveVisible()
    }

    private companion object {
        const val TEAM_NAME = "Cells S3 E2E"
        const val TEST_SERVICE_DEVICE = "Device1"
        const val FILE_PREFIX = "cells-s3-e2e"
        const val SMALL_FILE_SIZE = 2L * 1024L * 1024L
        const val MULTIPART_FILE_SIZE = 100L * 1024L * 1024L + 1L
        val SMALL_DOWNLOAD_TIMEOUT = 2.minutes
        val MULTIPART_DOWNLOAD_TIMEOUT = 10.minutes
        val TEST_SERVICE_REQUEST_TIMEOUT: JavaDuration = JavaDuration.ofMinutes(6)
    }
}
