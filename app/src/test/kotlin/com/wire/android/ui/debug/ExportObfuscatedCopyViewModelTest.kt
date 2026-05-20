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

package com.wire.android.ui.debug

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.ui.home.settings.backup.BackupAndRestoreState
import com.wire.android.ui.home.settings.backup.BackupCreationProgress
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.feature.backup.CreateBackupResult
import com.wire.kalium.logic.feature.backup.CreateObfuscatedCopyUseCase
import com.wire.kalium.util.DelicateKaliumApi
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(DelicateKaliumApi::class, ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ExportObfuscatedCopyViewModelTest {

    @Test
    fun givenCopyCreationSucceeds_whenCreatingObfuscatedCopy_thenFinishedStateAndLatestBackupAreSet() = runTest {
        val backupPath = "backup-file-path".toPath()
        val backupName = "backup-name.zip"
        val (_, viewModel) = Arrangement()
            .withSuccessfulCreation(backupPath, backupName)
            .arrange()

        viewModel.createObfuscatedCopy()
        advanceUntilIdle()

        assertEquals(BackupCreationProgress.Finished(backupName), viewModel.state.backupCreationProgress)
        assertEquals(
            BackupAndRestoreState.CreatedBackup(backupPath, backupName, false),
            viewModel.latestCreatedBackup
        )
    }

    @Test
    fun givenCopyCreationFails_whenCreatingObfuscatedCopy_thenFailedStateIsSet() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withFailedCreation()
            .arrange()

        viewModel.createObfuscatedCopy()
        advanceUntilIdle()

        assertEquals(BackupCreationProgress.Failed, viewModel.state.backupCreationProgress)
        assertNull(viewModel.latestCreatedBackup)
        coVerify(exactly = 1) { arrangement.createUnencryptedCopy(null) }
    }

    @Test
    fun givenACreatedCopy_whenSharingIt_thenGatewaySharesCopyAndStateIsReset() = runTest {
        val createdCopy = BackupAndRestoreState.CreatedBackup("backup-file-path".toPath(), "backup-name.zip", false)
        val (arrangement, viewModel) = Arrangement()
            .withPreviouslyCreatedCopy(createdCopy)
            .arrange()

        viewModel.shareCopy()
        advanceUntilIdle()

        assertEquals(listOf(createdCopy.path to createdCopy.assetName), arrangement.fileGateway.sharedCopies)
        assertEquals(BackupCreationProgress.InProgress(), viewModel.state.backupCreationProgress)
    }

    @Test
    fun givenACreatedCopy_whenSavingIt_thenGatewaySavesCopyAndStateIsReset() = runTest {
        val createdCopy = BackupAndRestoreState.CreatedBackup("backup-file-path".toPath(), "backup-name.zip", false)
        val destinationUri = "content://backup-destination"
        val (arrangement, viewModel) = Arrangement()
            .withPreviouslyCreatedCopy(createdCopy)
            .arrange()

        viewModel.saveCopy(destinationUri)
        advanceUntilIdle()

        assertEquals(listOf(createdCopy.path to destinationUri), arrangement.fileGateway.savedCopies)
        assertEquals(BackupCreationProgress.InProgress(), viewModel.state.backupCreationProgress)
    }

    private class Arrangement {

        @MockK
        lateinit var createUnencryptedCopy: CreateObfuscatedCopyUseCase

        val fileGateway = FakeExportObfuscatedCopyFileGateway()

        private val viewModel: ExportObfuscatedCopyViewModelImpl

        init {
            MockKAnnotations.init(this)
            coEvery { createUnencryptedCopy(null) } returns CreateBackupResult.Success(
                "backup-file-path".toPath(),
                "backup-name.zip"
            )
            viewModel = ExportObfuscatedCopyViewModelImpl(
                createUnencryptedCopy = createUnencryptedCopy,
                dispatcher = TestDispatcherProvider(),
                fileGateway = fileGateway,
            )
        }

        fun withSuccessfulCreation(path: Path, name: String) = apply {
            coEvery { createUnencryptedCopy(null) } returns CreateBackupResult.Success(path, name)
        }

        fun withFailedCreation() = apply {
            coEvery { createUnencryptedCopy(null) } returns CreateBackupResult.Failure(
                CoreFailure.Unknown(IOException("Some db error"))
            )
        }

        fun withPreviouslyCreatedCopy(createdCopy: BackupAndRestoreState.CreatedBackup) = apply {
            viewModel.latestCreatedBackup = createdCopy
            viewModel.state = viewModel.state.copy(
                backupCreationProgress = BackupCreationProgress.Finished(createdCopy.assetName)
            )
        }

        fun arrange() = this to viewModel
    }

    private class FakeExportObfuscatedCopyFileGateway : ExportObfuscatedCopyFileGateway {
        val sharedCopies = mutableListOf<Pair<Path, String?>>()
        val savedCopies = mutableListOf<Pair<Path, String>>()

        override suspend fun shareCopy(path: Path, assetName: String?) {
            sharedCopies += path to assetName
        }

        override suspend fun saveCopy(path: Path, destinationUri: String) {
            savedCopies += path to destinationUri
        }
    }
}
