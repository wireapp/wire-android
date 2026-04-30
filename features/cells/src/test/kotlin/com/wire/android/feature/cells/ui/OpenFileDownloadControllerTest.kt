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

import app.cash.turbine.test
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.feature.cells.util.FileNameResolver
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.common.functional.left
import com.wire.kalium.common.functional.right
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
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

class OpenFileDownloadControllerTest {

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
            onOpenFile = {},
            onError = { errorReceived = it },
        )
        advanceUntilIdle()

        assertEquals(CellError.OTHER_ERROR, errorReceived)
    }

    @Test
    fun givenFastDownloadSuccess_whenStartCalled_thenFileOpenedImmediatelyWithNoLoadingState() = runTest {
        val (_, controller) = Arrangement()
            .withDownloadSuccess()
            .arrange()
        var openedFile: CellNodeUi.File? = null

        controller.start(
            scope = this,
            cellNode = testFile,
            onOpenFile = { openedFile = it },
            onError = {},
        )
        advanceUntilIdle()

        assertEquals(testFile.uuid, openedFile?.uuid)
        assertTrue(controller.openLoadStates.value.isEmpty(), "No load state should be set on fast path")
    }

    @Test
    fun givenFastDownloadSuccess_whenStartCalled_thenLocalPathStoredInSharedCache() = runTest {
        val (arrangement, controller) = Arrangement()
            .withDownloadSuccess()
            .arrange()

        controller.start(
            scope = this,
            cellNode = testFile,
            onOpenFile = {},
            onError = {},
        )
        advanceUntilIdle()

        assertNotNull(arrangement.sharedPathCache.paths.value[testFile.uuid])
    }

    @Test
    fun givenSlowDownloadSuccess_whenSpinnerThresholdPassed_thenLoadingStateAppears() = runTest {
        val (_, controller) = Arrangement()
            .withSlowDownloadSuccess()
            .arrange()

        controller.start(
            scope = this,
            cellNode = testFile,
            onOpenFile = {},
            onError = {},
        )
        advanceTimeBy(SPINNER_THRESHOLD_MS + 1)

        assertEquals(OpenLoadState.Loading(), controller.openLoadStates.value[testFile.uuid])
    }

    @Test
    fun givenSlowDownloadSuccess_whenDownloadCompletes_thenStateBecomesReady() = runTest {
        val (_, controller) = Arrangement()
            .withSlowDownloadSuccess()
            .arrange()

        controller.start(
            scope = this,
            cellNode = testFile,
            onOpenFile = {},
            onError = {},
        )
        // Advance past the spinner (300 ms) and the download (500 ms) but NOT past the
        // auto-dismiss delay (3 000 ms) — otherwise advanceUntilIdle would clear the state too.
        advanceTimeBy(501)

        assertTrue(
            controller.openLoadStates.value[testFile.uuid] is OpenLoadState.Ready,
            "Expected Ready state after slow download"
        )
    }

    @Test
    fun givenSlowDownloadSuccess_whenDownloadCompletes_thenFileReadyEventEmittedToSharedCache() = runTest {
        val (arrangement, controller) = Arrangement()
            .withSlowDownloadSuccess()
            .arrange()

        arrangement.sharedPathCache.fileReadyEvents.test {
            controller.start(
                scope = this@runTest,
                cellNode = testFile,
                onOpenFile = {},
                onError = {},
            )
            advanceUntilIdle()

            assertEquals(testFile.uuid, awaitItem().uuid)
        }
    }

    @Test
    fun givenReadyState_afterAutoDismissDelay_thenLoadStateIsCleared() = runTest {
        val (_, controller) = Arrangement()
            .withSlowDownloadSuccess()
            .arrange()

        controller.start(
            scope = this,
            cellNode = testFile,
            onOpenFile = {},
            onError = {},
        )
        advanceUntilIdle() // download completes → Ready

        advanceTimeBy(AUTO_DISMISS_MS + 1) // auto-dismiss fires

        assertNull(controller.openLoadStates.value[testFile.uuid], "Load state should be cleared after auto-dismiss")
    }

    @Test
    fun givenDownloadFailure_whenStartCalled_thenErrorStateSet() = runTest {
        val (_, controller) = Arrangement()
            .withDownloadFailure()
            .arrange()

        controller.start(
            scope = this,
            cellNode = testFile,
            onOpenFile = {},
            onError = {},
        )
        advanceUntilIdle()

        assertEquals(OpenLoadState.Error, controller.openLoadStates.value[testFile.uuid])
    }

    @Test
    fun givenActiveDownload_whenCancelCalled_thenLoadStateClearedImmediately() = runTest {
        val (_, controller) = Arrangement()
            .withSlowDownloadSuccess()
            .arrange()

        controller.start(scope = this, cellNode = testFile, onOpenFile = {}, onError = {})
        advanceTimeBy(SPINNER_THRESHOLD_MS + 1) // spinner shown → Loading state

        controller.cancel(testFile.uuid)

        assertNull(controller.openLoadStates.value[testFile.uuid], "Cancel should clear load state")
    }

    @Test
    fun givenActiveDownload_whenCancelCalled_thenFileIsNotOpened() = runTest {
        val (_, controller) = Arrangement()
            .withSlowDownloadSuccess()
            .arrange()
        val openedFiles = mutableListOf<CellNodeUi.File>()

        controller.start(scope = this, cellNode = testFile, onOpenFile = { openedFiles += it }, onError = {})
        advanceTimeBy(100)

        controller.cancel(testFile.uuid)
        advanceUntilIdle()

        assertTrue(openedFiles.isEmpty(), "File must not be opened after cancel")
    }

    @Test
    fun givenRapidRetryForSameFile_whenStartCalledTwice_thenOnlySecondDownloadCompletes() = runTest {
        val (arrangement, controller) = Arrangement()
            .withSlowDownloadSuccess()
            .arrange()

        // Both starts are synchronous: when start() #2 runs, job #1 is still suspended at
        // its download delay, so it gets cancelled immediately before completing.
        controller.start(scope = this, cellNode = testFile, onOpenFile = {}, onError = {})
        controller.start(scope = this, cellNode = testFile, onOpenFile = {}, onError = {})

        // Advance past spinner (300 ms) + download (500 ms), but NOT past auto-dismiss (3 000 ms).
        advanceTimeBy(501)

        // The shared path cache is populated by onSuccess regardless of fast/slow path.
        // Exactly one entry means exactly one download completed.
        assertEquals(
            1,
            arrangement.sharedPathCache.paths.value.size,
            "Only the second download should have stored a path"
        )
        assertTrue(
            controller.openLoadStates.value[testFile.uuid] is OpenLoadState.Ready,
            "State should be Ready after the second download completes"
        )
    }

    @Test
    fun givenProgressUpdate_whenDownloadProgresses_thenLoadingProgressReflected() = runTest {
        val (_, controller) = Arrangement()
            .withProgressThenSuccess(progress = 512L)
            .arrange()

        controller.start(scope = this, cellNode = testFile.copy(size = 1024L), onOpenFile = {}, onError = {})
        // Advance to 401 ms:
        //   300 ms → spinner fires → Loading() shown
        //   400 ms → onProgressUpdate fires → child launch sets Loading(0.5f)
        //   500 ms → download completes (NOT yet reached)
        advanceTimeBy(401)

        val state = controller.openLoadStates.value[testFile.uuid]
        assertEquals(0.5f, (state as? OpenLoadState.Loading)?.progress)
    }

    @Test
    fun givenErrorAndLoadingStates_whenClearAllErrorStatesCalled_thenOnlyErrorsRemoved() = runTest {
        val (_, controller) = Arrangement()
            .withSlowDownloadSuccess(uuid = testFile.uuid)
            .withDownloadFailure(uuid = anotherFile.uuid)
            .arrange()

        // testFile → Loading (slow, timer fires), anotherFile → Error (immediate failure)
        controller.start(scope = this, cellNode = testFile, onOpenFile = {}, onError = {})
        controller.start(scope = this, cellNode = anotherFile, onOpenFile = {}, onError = {})
        advanceTimeBy(SPINNER_THRESHOLD_MS + 1) // spinner for testFile shows Loading
        // Advance only enough for anotherFile's (instant) download to fail — but NOT past the
        // 500 ms slow download so testFile stays in Loading.
        advanceTimeBy(1) // anotherFile failure is already settled; testFile still downloading

        controller.clearAllErrorStates()

        assertNull(controller.openLoadStates.value[anotherFile.uuid], "Error state should be removed")
        assertNotNull(controller.openLoadStates.value[testFile.uuid], "Loading state should be preserved")
    }

    private companion object {
        const val SPINNER_THRESHOLD_MS = 300L
        const val AUTO_DISMISS_MS = 3_000L

        val testFile = CellNodeUi.File(
            uuid = "test-uuid",
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

        val anotherFile = testFile.copy(uuid = "another-uuid", name = "photo.jpg")
    }

    private inner class Arrangement {

        @MockK
        lateinit var downloadUseCase: DownloadCellFileUseCase

        @MockK
        lateinit var fileHelper: FileHelper

        @MockK
        lateinit var fileNameResolver: FileNameResolver

        val sharedPathCache = CellFileLocalPathCache()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { fileHelper.getCacheDir() } returns File("")
            every { fileNameResolver.getUniqueFile(any(), any()) } returns File("report.pdf")
        }

        fun withDownloadSuccess(uuid: String = testFile.uuid) = apply {
            coEvery { downloadUseCase(eq(uuid), any(), any(), any(), any()) } returns Unit.right()
        }

        fun withSlowDownloadSuccess(uuid: String = testFile.uuid) = apply {
            coEvery { downloadUseCase(eq(uuid), any(), any(), any(), any()) } coAnswers {
                delay(500) // Exceeds 300 ms spinner threshold
                Unit.right()
            }
        }

        fun withDownloadFailure(uuid: String = testFile.uuid) = apply {
            coEvery { downloadUseCase(eq(uuid), any(), any(), any(), any()) } returns
                    StorageFailure.DataNotFound.left()
        }

        fun withProgressThenSuccess(progress: Long, uuid: String = testFile.uuid) = apply {
            coEvery { downloadUseCase(eq(uuid), any(), any(), any(), any()) } coAnswers {
                val onProgressUpdate = arg<(Long) -> Unit>(4)
                delay(400) // after spinner threshold (300 ms) — progress replaces Loading()
                onProgressUpdate(progress)
                delay(100) // download finishes at 500 ms total
                Unit.right()
            }
        }

        fun arrange() = this to OpenFileDownloadController(
            download = downloadUseCase,
            fileHelper = fileHelper,
            fileNameResolver = fileNameResolver,
            sharedPathCache = sharedPathCache,
        )
    }
}
