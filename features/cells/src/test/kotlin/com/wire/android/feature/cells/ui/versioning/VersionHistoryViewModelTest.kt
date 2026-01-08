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
package com.wire.android.feature.cells.ui.versioning

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.ui.versioning.download.DownloadState
import com.wire.android.feature.cells.ui.versioning.restore.RestoreDialogState
import com.wire.android.feature.cells.ui.versioning.restore.RestoreVersionState
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.util.FileSizeFormatter
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.resolveForTest
import com.wire.android.util.ui.toUIText
import com.wire.kalium.cells.domain.model.NodeVersion
import com.wire.kalium.cells.domain.model.PreSignedUrl
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellVersionUseCase
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.cells.domain.usecase.versioning.RestoreNodeVersionUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.right
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dispatcher = StandardTestDispatcher()

@ExperimentalCoroutinesApi
class VersionHistoryViewModelTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenViewModel_whenItInits_thenIsFetchingStateIsManagedCorrectly() = runTest {
        val (_, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(emptyList()))
            .arrange()

        assertEquals(VersionHistoryState.Idle, viewModel.versionHistoryState.value)
        advanceUntilIdle()
        assertEquals(VersionHistoryState.Success, viewModel.versionHistoryState.value)
    }

    @Suppress("LongMethod")
    @Test
    fun givenSuccessfulFetch_whenViewModelInits_thenVersionsAreGroupedCorrectly() = runTest {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)

        val (_, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(versionsFromApi))
            .withFileSizeFormatter()
            .arrange()

        advanceUntilIdle()

        // Versions should be grouped into three sections (Today, Yesterday, and an older date)
        val groupedVersions = viewModel.versionsGroupedByTime.value
        assertEquals(3, groupedVersions.size)

        // Verify "Today" group is correct
        val todayFormattedDate = today.format(DateTimeFormatter.ofPattern("d MMM yyyy"))

        val todayFakeString = mapOf(R.string.date_label_today to "Today, %1\$s")
        val actualTodayText = groupedVersions[0].dateLabel.resolveForTest(todayFakeString)

        assertEquals("Today, $todayFormattedDate", actualTodayText)
        assertEquals(1, groupedVersions[0].versions.size)
        assertEquals("User A", groupedVersions[0].versions[0].modifiedBy)
        val expectedTime = Instant
            .ofEpochSecond(versionNode.modifiedTime!!.toLong())
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        assertEquals(expectedTime, groupedVersions[0].versions[0].modifiedAt)

        // Verify "Yesterday" group is correct
        val yesterdayFormattedDate = yesterday.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        val yesterdayFakeString = mapOf(R.string.date_label_yesterday to "Yesterday, %1\$s")
        val actualYesterdayText = groupedVersions[1].dateLabel.resolveForTest(yesterdayFakeString)

        assertEquals("Yesterday, $yesterdayFormattedDate", actualYesterdayText)
        assertEquals(2, groupedVersions[1].versions.size)
        assertEquals("User B", groupedVersions[1].versions[0].modifiedBy)
        assertEquals("User A", groupedVersions[1].versions[1].modifiedBy)

        // Verify older date group is correct
        val twoDaysAgoFormatted = twoDaysAgo.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        assertEquals(twoDaysAgoFormatted.toUIText(), groupedVersions[2].dateLabel)
        assertEquals(1, groupedVersions[2].versions.size)
    }

    @Test
    fun givenApiFailure_whenViewModelInits_thenVersionListIsEmpty() = runTest {
        val (_, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Left(CoreFailure.MissingClientRegistration))
            .arrange()

        advanceUntilIdle()

        assertTrue(viewModel.versionsGroupedByTime.value.isEmpty())
        assertEquals(VersionHistoryState.Failed, viewModel.versionHistoryState.value)
    }

    @Test
    fun givenDialogIsHidden_whenShowRestoreConfirmationDialogIsCalled_thenStateIsVisibleWithCorrectVersionId() = runTest {
        // GIVEN an initial state where the dialog is not visible
        val testVersionId = "version-id-12345"
        val (_, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(emptyList()))
            .arrange()

        viewModel.restoreDialogState.value = RestoreDialogState(visible = false)
        assertFalse(viewModel.restoreDialogState.value.visible)

        // WHEN the `showRestoreConfirmationDialog` function is called
        viewModel.showRestoreConfirmationDialog(testVersionId)

        // THEN the dialog state should be updated to be visible with the correct data
        val newState = viewModel.restoreDialogState.value
        assertTrue(newState.visible)
        assertEquals(testVersionId, newState.versionId)
        assertEquals(RestoreVersionState.Idle, newState.restoreVersionState)
        assertEquals(0f, newState.restoreProgress)
    }

    @Test
    fun givenDialogIsVisible_whenHideRestoreConfirmationDialogIsCalled_thenStateIsHiddenAndReset() = runTest {
        // GIVEN an initial state where the dialog is visible and has data
        val (_, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(emptyList()))
            .arrange()

        viewModel.restoreDialogState.value = RestoreDialogState(
            visible = true,
            versionId = "version-id-12345",
            restoreVersionState = RestoreVersionState.Completed,
            restoreProgress = 1f
        )

        assertTrue(viewModel.restoreDialogState.value.visible)

        // WHEN the `hideRestoreConfirmationDialog` function is called
        viewModel.hideRestoreConfirmationDialog()

        // THEN the dialog state should be updated to be hidden and reset
        val newState = viewModel.restoreDialogState.value
        assertFalse(newState.visible)
        assertEquals("", newState.versionId)
        assertEquals(RestoreVersionState.Idle, newState.restoreVersionState)
    }

    @Test
    fun givenUseCaseSucceeds_whenRestoreVersionIsCalled_thenStateBecomesCompleted() = runTest {
        // GIVEN the restore use case will succeed
        val testVersionId = "version-to-restore"
        val (arrangement, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(emptyList()))
            .withRestoreNodeVersionReturning(Unit.right())
            .arrange()
        viewModel.restoreDialogState.value = RestoreDialogState(versionId = testVersionId)

        // WHEN the `restoreVersion` function is called
        viewModel.restoreVersion()

        // THEN the state should immediately be updated to Restoring
        assertEquals(RestoreVersionState.Restoring, viewModel.restoreDialogState.value.restoreVersionState)

        // AND after coroutines complete, the final state should be Completed
        advanceUntilIdle()

        val finalState = viewModel.restoreDialogState.value
        assertEquals(RestoreVersionState.Completed, finalState.restoreVersionState)
        assertEquals(1f, finalState.restoreProgress)

        // list of versions should be re-fetched, making it the SECOND call overall after the init block
        coVerify(exactly = 2) { arrangement.getNodeVersionsUseCase(any()) }
    }

    @Test
    fun givenUseCaseFails_whenRestoreVersionIsCalled_thenStateBecomesFailed() = runTest {
        // GIVEN
        val testVersionId = "version-to-restore"
        val (arrangement, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(emptyList()))
            .withRestoreNodeVersionReturning(Either.Left(CoreFailure.MissingClientRegistration))
            .arrange()
        viewModel.restoreDialogState.value = RestoreDialogState(versionId = testVersionId)

        // WHEN the `restoreVersion` function is called
        viewModel.restoreVersion()

        // THEN the state should immediately be updated to Restoring
        assertEquals(RestoreVersionState.Restoring, viewModel.restoreDialogState.value.restoreVersionState)

        advanceUntilIdle() // Execute all pending coroutines

        val finalState = viewModel.restoreDialogState.value
        assertEquals(RestoreVersionState.Failed, finalState.restoreVersionState)

        // list should NOT be re-fetched, the call count remains 1 (from the init block)
        coVerify(exactly = 1) { arrangement.getNodeVersionsUseCase(any()) }
    }

    @Test
    fun givenVersionExistsAndUseCaseSucceeds_whenDownloadVersionIsCalled_thenStateBecomesDownloaded() = runTest {
        // GIVEN a version exists and all dependencies will succeed
        val (_, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withDownloadVersionReturning(shouldSucceed = true, true)
            .withGetNodeVersionReturning(Either.Right(versionsFromApi))
            .withFileSizeFormatter()
            .withSuccessfulFileCreation()
            .arrange()

        // WHEN downloadVersion is called
        viewModel.downloadVersion(testVersion.versionId, "2025-01-01")

        runCurrent()
        // THEN the state should immediately become Downloading
        assertTrue(viewModel.downloadState.value is DownloadState.Downloading)

        // AND after the coroutine finishes, the state becomes Downloaded
        advanceUntilIdle()

        assertTrue(viewModel.downloadState.value is DownloadState.Downloaded)
    }

    @Test
    fun givenDownloadUseCaseFails_whenDownloadVersionIsCalled_thenStateBecomesFailed() = runTest {
        // GIVEN a version exists but the download use case will fail
        val (_, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(emptyList()))
            .withDownloadVersionReturning(shouldSucceed = false)
            .withFileSizeFormatter()
            .withSuccessfulFileCreation()
            .arrange()

        val versionGroup = VersionGroup(
            dateLabel = UIText.DynamicString("Today"),
            versions = listOf(testVersion)
        )
        viewModel.versionsGroupedByTime.value = listOf(versionGroup)

        // WHEN downloadVersion is called
        viewModel.downloadVersion(testVersion.versionId, "2025-01-01")
        advanceUntilIdle()

        // THEN the final state should be Failed
        assertEquals(DownloadState.Failed, viewModel.downloadState.value)
    }

    @Test
    fun givenFileCreationFails_whenDownloadVersionIsCalled_thenStateBecomesFailed() = runTest {
        // GIVEN a version exists but the file helper returns null (cannot create file)
        val (arrangement, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(emptyList()))
            .withFileSizeFormatter()
            .withFileCreationFailure()
            .arrange()

        val versionGroup = VersionGroup(
            dateLabel = UIText.DynamicString("Today"),
            versions = listOf(testVersion)
        )
        viewModel.versionsGroupedByTime.value = listOf(versionGroup)

        // WHEN downloadVersion is called
        viewModel.downloadVersion(testVersion.versionId, "2025-01-01")
        advanceUntilIdle()

        // THEN the state remains Idle and the use case is never called
        assertEquals(DownloadState.Failed, viewModel.downloadState.value)
        coVerify(exactly = 0) { arrangement.downloadCellVersionUseCase(any(), any(), any()) }
    }

    @Test
    fun givenVersionDoesNotExist_whenDownloadVersionIsCalled_thenStateBecomesFailed() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(emptyList()))
            .withFileSizeFormatter()
            .withFileCreationFailure()
            .arrange()

        // WHEN downloadVersion is called with a non-existent ID
        viewModel.downloadVersion("non-existent-id", "2025-01-01")
        advanceUntilIdle()

        // THEN the state remains Idle
        assertEquals(DownloadState.Failed, viewModel.downloadState.value)
        coVerify(exactly = 0) { arrangement.fileHelper.createDownloadFileStream(any()) }
        coVerify(exactly = 0) { arrangement.downloadCellVersionUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun givenEditorUrlExists_whenOpenOnlineEditor_thenEditorIsOpened() = runTest {
        // Given
        val expectedUrl = "https://example.com/editor"
        val (arrangement, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(emptyList()))
            .withGetEditorUrlReturning(expectedUrl.right())
            .withOnlineEditor()
            .arrange()

        // When
        viewModel.openOnlineEditor()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { arrangement.getEditorUrlUseCase(any()) }
        coVerify(exactly = 1) { arrangement.onlineEditor.open(expectedUrl) }
    }

    @Test
    fun givenGetEditorUrlFails_whenOpenOnlineEditorIsCalled_thenEditorIsNotOpened() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withSavedStateHandleReturning()
            .withGetNodeVersionReturning(Either.Right(emptyList()))
            .withGetEditorUrlReturning(Either.Left(CoreFailure.MissingClientRegistration))
            .withOnlineEditor()
            .arrange()

        // When
        viewModel.openOnlineEditor()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { arrangement.getEditorUrlUseCase(any()) }
        coVerify(inverse = true) { arrangement.onlineEditor.open("url") }
    }

    private class Arrangement {

        val savedStateHandle: SavedStateHandle = mockk(relaxed = true)
        val getNodeVersionsUseCase: GetNodeVersionsUseCase = mockk()
        val fileSizeFormatter: FileSizeFormatter = mockk()
        val restoreNodeVersionUseCase: RestoreNodeVersionUseCase = mockk()
        val downloadCellVersionUseCase: DownloadCellVersionUseCase = mockk()
        val fileHelper: FileHelper = mockk()
        val onlineEditor: OnlineEditor = mockk()
        val getEditorUrlUseCase: GetEditorUrlUseCase = mockk()
        private val testDispatcherProvider = TestDispatcherProvider(dispatcher)

        private val testNodeUuid = "test-node-uuid"

        init {
            every { savedStateHandle.get<String>("uuid") } returns "test-node-uuid"
            every { savedStateHandle.get<String>("fileName") } returns "file-name"
        }

        fun withSavedStateHandleReturning() = apply {
            every { savedStateHandle.get<String>("uuid") } returns testNodeUuid
            every { savedStateHandle.get<String>("fileName") } returns "file-name"
        }

        fun withGetNodeVersionReturning(returnValue: Either<CoreFailure, List<NodeVersion>>) = apply {
            coEvery { getNodeVersionsUseCase(testNodeUuid) } returns returnValue
        }

        fun withRestoreNodeVersionReturning(returnValue: Either<CoreFailure, Unit>) = apply {
            coEvery { restoreNodeVersionUseCase(any(), any()) } returns returnValue
        }

        fun withDownloadVersionReturning(
            shouldSucceed: Boolean,
            simulateProgress: Boolean = false
        ) = apply {
            coEvery { downloadCellVersionUseCase.invoke(any(), any(), any()) } coAnswers {
                val onProgressUpdate = it.invocation.args[2] as (Long, Long) -> Unit

                if (simulateProgress) {
                    // Simulate async work before the first progress update
                    kotlinx.coroutines.delay(1)
                    onProgressUpdate(50L, 100L) // Simulate 50% progress
                    // Simulate more work before finishing
                    kotlinx.coroutines.delay(1)
                }

                if (shouldSucceed) {
                    Unit.right()
                } else {
                    Either.Left(CoreFailure.MissingClientRegistration)
                }
            }
        }

        fun withSuccessfulFileCreation() = apply {
            val mockOutputStream: OutputStream = mockk(relaxed = true)
            coEvery { fileHelper.createDownloadFileStream(any()) } returns mockOutputStream
        }

        fun withFileCreationFailure() = apply {
            every { fileHelper.createDownloadFileStream(any()) } returns null
        }

        fun withFileSizeFormatter() = apply {
            coEvery { fileSizeFormatter.formatSize(any()) } returns "30 MB"
        }

        fun withGetEditorUrlReturning(result: Either<CoreFailure, String?>) = apply {
            coEvery { getEditorUrlUseCase(any()) } returns result
        }

        fun withOnlineEditor() = apply {
            coEvery { onlineEditor.open(any()) } returns Unit
        }

        fun arrange(): Pair<Arrangement, VersionHistoryViewModel> {
            val viewModel = VersionHistoryViewModel(
                savedStateHandle = savedStateHandle,
                getNodeVersionsUseCase = getNodeVersionsUseCase,
                fileSizeFormatter = fileSizeFormatter,
                restoreNodeVersionUseCase = restoreNodeVersionUseCase,
                downloadCellVersionUseCase = downloadCellVersionUseCase,
                fileHelper = fileHelper,
                onlineEditor = onlineEditor,
                getEditorUrl = getEditorUrlUseCase,
                dispatchers = testDispatcherProvider,
            )
            return this to viewModel
        }
    }

    companion object {
        val today: LocalDate = LocalDate.now()
        val yesterday: LocalDate = today.minusDays(1)
        val twoDaysAgo: LocalDate = today.minusDays(2)

        val versionNode = NodeVersion(
            id = "v1",
            hash = null,
            description = null,
            isDraft = true,
            etag = "etag",
            editorUrls = null,
            filePreviews = null,
            isHead = false,
            modifiedTime = today.atTime(10, 30).toEpochSecond(ZoneOffset.UTC).toString(),
            ownerName = "User A",
            ownerUuid = "uuid",
            getUrl = PreSignedUrl("expiration", "url"),
            size = "1500"
        )

        val versionsFromApi = listOf(
            versionNode,
            versionNode.copy(
                id = "v2",
                ownerName = "User B",
                modifiedTime = yesterday.atTime(14, 0).toEpochSecond(ZoneOffset.UTC).toString(),
                size = "2048"
            ),
            versionNode.copy(
                id = "v3",
                ownerName = "User A",
                modifiedTime = yesterday.atTime(9, 15).toEpochSecond(ZoneOffset.UTC).toString(),
                size = "5000000"
            ),
            versionNode.copy(
                id = "v4",
                ownerName = "User C",
                modifiedTime = twoDaysAgo.atStartOfDay().toEpochSecond(ZoneOffset.UTC).toString(),
                size = "123"
            ),
        )
        private val testVersion = CellVersion(
            versionId = "v1",
            modifiedBy = "user",
            fileSize = "1MB",
            modifiedAt = "10:30 AM",
            isCurrentVersion = false,
            presignedUrl = "https://wire.com/"
        )
    }
}
