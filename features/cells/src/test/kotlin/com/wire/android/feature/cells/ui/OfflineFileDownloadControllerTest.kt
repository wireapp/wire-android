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
package com.wire.android.feature.cells.ui

import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.util.FileHelper
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.SaveOfflineFileUseCase
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.common.functional.left
import com.wire.kalium.common.functional.right
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.time.Duration.Companion.milliseconds

class OfflineFileDownloadControllerTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun beforeEach() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenFileWithNoName_whenStartCalled_thenOnErrorCallbackInvoked() = runTest {
        val (_, controller) = Arrangement().arrange()
        var errorReceived: CellError? = null

        controller.start(
            scope = this,
            cellNode = testFile.copy(name = null),
            onSuccess = {},
            onError = { errorReceived = it },
        )
        advanceUntilIdle()

        assertEquals(CellError.OTHER_ERROR, errorReceived)
    }

    @Test
    fun givenFileWithExistingLocalPath_whenFileOnDisk_thenSaveOfflineCalledWithoutDownload() = runTest {
        val (arrangement, controller) = Arrangement().arrange()
        val realFile = File(arrangement.externalFilesDir, "report.pdf").also { it.createNewFile() }
        val fileWithLocalPath = testFile.copy(localPath = realFile.absolutePath)
        var successPath: String? = null

        controller.start(
            scope = this,
            cellNode = fileWithLocalPath,
            onSuccess = { successPath = it },
            onError = {},
        )
        advanceUntilIdle()

        assertEquals(realFile.absolutePath, successPath)
        coVerify(exactly = 0) { arrangement.downloadUseCase(any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { arrangement.saveOfflineFile(any()) }
    }

    @Test
    fun givenFileWithExistingLocalPath_whenFileDeletedFromDisk_thenDownloadStarted() = runTest {
        val (arrangement, controller) = Arrangement()
            .withDownloadSuccess()
            .arrange()
        val fileWithStalePath = testFile.copy(localPath = "/non/existent/report.pdf")

        controller.start(
            scope = this,
            cellNode = fileWithStalePath,
            onSuccess = {},
            onError = {},
        )
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.downloadUseCase(eq(testFile.uuid), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun givenDownloadSuccess_whenStartCalled_thenOnSuccessCallbackInvoked() = runTest {
        val (_, controller) = Arrangement()
            .withDownloadSuccess()
            .arrange()
        var successPath: String? = null

        controller.start(
            scope = this,
            cellNode = testFile,
            onSuccess = { successPath = it },
            onError = {},
        )
        advanceUntilIdle()

        assertNotNull(successPath)
    }

    @Test
    fun givenDownloadSuccess_whenStartCalled_thenProgressCleared() = runTest {
        val (_, controller) = Arrangement()
            .withDownloadSuccess()
            .arrange()

        controller.start(scope = this, cellNode = testFile, onSuccess = {}, onError = {})
        advanceUntilIdle()

        assertNull(controller.downloadProgresses.value[testFile.uuid], "Progress should be cleared after success")
    }

    @Test
    fun givenDownloadSuccess_whenStartCalled_thenSaveOfflineFileCalled() = runTest {
        val (arrangement, controller) = Arrangement()
            .withDownloadSuccess()
            .arrange()

        controller.start(scope = this, cellNode = testFile, onSuccess = {}, onError = {})
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.saveOfflineFile(any()) }
    }

    @Test
    fun givenDownloadSuccess_whenStartCalled_thenPathRecordedInSharedCache() = runTest {
        val (arrangement, controller) = Arrangement()
            .withDownloadSuccess()
            .arrange()

        controller.start(scope = this, cellNode = testFile, onSuccess = {}, onError = {})
        advanceUntilIdle()

        assertNotNull(arrangement.sharedPathCache.getCompletedPath(testFile.uuid))
    }

    @Test
    fun givenProgressUpdate_whenDownloadProgresses_thenProgressReflectedInFlow() = runTest {
        val (_, controller) = Arrangement()
            .withProgressThenSuccess(bytesDownloaded = 512L)
            .arrange()

        controller.start(scope = this, cellNode = testFile.copy(size = 1024L), onSuccess = {}, onError = {})
        // Download emits progress at 200ms, completes at 300ms.
        // Advance to 250ms to capture the in-progress state.
        advanceTimeBy(250.milliseconds)

        val progress = controller.downloadProgresses.value[testFile.uuid]
        assertEquals(0.5f, progress)
    }

    @Test
    fun givenDownloadFailure_whenStartCalled_thenOnErrorCallbackInvoked() = runTest {
        val (_, controller) = Arrangement()
            .withDownloadFailure()
            .arrange()
        var errorReceived: CellError? = null

        controller.start(scope = this, cellNode = testFile, onSuccess = {}, onError = { errorReceived = it })
        advanceUntilIdle()

        assertEquals(CellError.DOWNLOAD_FAILED, errorReceived)
    }

    @Test
    fun givenNoSpaceLeftFailure_whenStartCalled_thenNoSpaceLeftErrorReturned() = runTest {
        val (_, controller) = Arrangement()
            .withNoSpaceLeftFailure()
            .arrange()
        var errorReceived: CellError? = null

        controller.start(scope = this, cellNode = testFile, onSuccess = {}, onError = { errorReceived = it })
        advanceUntilIdle()

        assertEquals(CellError.NO_SPACE_LEFT, errorReceived)
    }

    @Test
    fun givenDownloadFailure_whenStartCalled_thenProgressCleared() = runTest {
        val (_, controller) = Arrangement()
            .withDownloadFailure()
            .arrange()

        controller.start(scope = this, cellNode = testFile, onSuccess = {}, onError = {})
        advanceUntilIdle()

        assertNull(controller.downloadProgresses.value[testFile.uuid])
    }

    @Test
    fun givenActiveDownload_whenCancelCalled_thenProgressCleared() = runTest {
        val (_, controller) = Arrangement()
            .withSlowDownloadSuccess()
            .arrange()

        controller.start(scope = this, cellNode = testFile, onSuccess = {}, onError = {})
        advanceTimeBy(100.milliseconds)

        controller.cancel(testFile.uuid, this)

        assertNull(controller.downloadProgresses.value[testFile.uuid], "Progress should be cleared on cancel")
    }

    @Test
    fun givenActiveDownload_whenCancelCalled_thenOnSuccessNotInvoked() = runTest {
        val (_, controller) = Arrangement()
            .withSlowDownloadSuccess()
            .arrange()
        var successCalled = false

        controller.start(scope = this, cellNode = testFile, onSuccess = { successCalled = true }, onError = {})
        advanceTimeBy(100.milliseconds)

        controller.cancel(testFile.uuid, this)
        advanceUntilIdle()

        assertTrue(!successCalled, "onSuccess must not be called after cancel")
    }

    @Test
    fun givenNoActiveDownload_whenCancelCalled_thenNothingHappens() = runTest {
        val (_, controller) = Arrangement().arrange()

        // Should not throw
        controller.cancel("non-existent-uuid", this)
    }

    @Test
    fun givenRapidRetry_whenStartCalledTwice_thenOnlySecondDownloadCompletes() = runTest {
        val (_, controller) = Arrangement()
            .withSlowDownloadSuccess()
            .arrange()
        var successCount = 0

        controller.start(scope = this, cellNode = testFile, onSuccess = { successCount++ }, onError = {})
        controller.start(scope = this, cellNode = testFile, onSuccess = { successCount++ }, onError = {})
        advanceUntilIdle()

        assertEquals(1, successCount, "Only the second download should complete")
    }

    @Test
    fun givenFileWithConversationId_whenDownloadSucceeds_thenFileStoredInConversationDirectory() = runTest {
        val (_, controller) = Arrangement()
            .withDownloadSuccess()
            .arrange()
        var successPath: String? = null

        controller.start(
            scope = this,
            cellNode = testFile.copy(conversationId = "conv-123"),
            onSuccess = { successPath = it },
            onError = {},
        )
        advanceUntilIdle()

        assertNotNull(successPath)
        assertTrue(
            successPath!!.contains("conv-123"),
            "File should be stored under the conversation directory, got: $successPath"
        )
    }

    @Test
    fun givenFileWithNoConversationId_whenDownloadSucceeds_thenFileStoredUnderUuidDirectory() = runTest {
        val (_, controller) = Arrangement()
            .withDownloadSuccess()
            .arrange()
        var successPath: String? = null

        controller.start(
            scope = this,
            cellNode = testFile.copy(conversationId = null),
            onSuccess = { successPath = it },
            onError = {},
        )
        advanceUntilIdle()

        assertNotNull(successPath)
        assertTrue(
            successPath!!.contains(testFile.uuid),
            "Standalone file should fall back to UUID directory, got: $successPath"
        )
    }

    private companion object {
        val testFile = CellNodeUi.File(
            uuid = "test-uuid",
            conversationId = "conversation-id",
            name = "report.pdf",
            mimeType = "application/pdf",
            assetType = AttachmentFileType.OTHER,
            localPath = null,
            size = 1024L,
            remotePath = "remote/report.pdf",
            userName = null,
            userHandle = null,
            ownerUserId = null,
            conversationName = null,
            modifiedTime = null,
        )
    }

    private inner class Arrangement {

        @MockK
        lateinit var downloadUseCase: DownloadCellFileUseCase

        @MockK
        lateinit var saveOfflineFile: SaveOfflineFileUseCase

        @MockK
        lateinit var fileHelper: FileHelper

        val sharedPathCache = CellFileLocalPathCache()
        val externalFilesDir: File = createTempDirectory("cells-offline-test").toFile()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { saveOfflineFile(any()) } returns Unit
            coEvery { fileHelper.getExternalFilesDir() } returns externalFilesDir
        }

        fun withDownloadSuccess() = apply {
            coEvery { downloadUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit.right()
        }

        fun withSlowDownloadSuccess() = apply {
            coEvery { downloadUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
                delay(500.milliseconds)
                Unit.right()
            }
        }

        fun withDownloadFailure() = apply {
            coEvery { downloadUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                    StorageFailure.DataNotFound.left()
        }

        fun withNoSpaceLeftFailure() = apply {
            coEvery { downloadUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                    NetworkFailure.ServerMiscommunication(IOException("No space left on device")).left()
        }

        fun withProgressThenSuccess(bytesDownloaded: Long) = apply {
            coEvery { downloadUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
                val onProgressUpdate = arg<(Long) -> Unit>(7)
                delay(200.milliseconds)
                onProgressUpdate(bytesDownloaded)
                delay(100.milliseconds)
                Unit.right()
            }
        }

        fun arrange() = this to OfflineFileDownloadController(
            download = downloadUseCase,
            fileHelper = fileHelper,
            saveOfflineFile = saveOfflineFile,
            sharedPathCache = sharedPathCache,
        )
    }
}
