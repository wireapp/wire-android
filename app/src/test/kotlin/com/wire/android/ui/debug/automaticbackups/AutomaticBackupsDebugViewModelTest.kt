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
package com.wire.android.ui.debug.automaticbackups

import android.net.Uri
import app.cash.turbine.test
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.util.FileManager
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.asset.UploadedAssetId
import com.wire.kalium.logic.data.backup.OnlineBackupMetadata
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.backup.BackupRootKey
import com.wire.kalium.logic.feature.backup.CreateOnlineBackupResult
import com.wire.kalium.logic.feature.backup.CreateOnlineBackupUseCase
import com.wire.kalium.logic.feature.backup.ExportBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.ExportBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.RestoreLatestOnlineBackupUseCase
import com.wire.kalium.logic.feature.backup.GenerateAndForcePushBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GenerateAndForcePushBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.GenerateBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GetBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GetBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.ImportBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.ImportBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.PushBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.SyncBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.SyncBackupRootKeyUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class AutomaticBackupsDebugViewModelTest {

    @Test
    fun givenNoStoredKey_whenViewModelIsCreated_thenStateHasNoKey() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .arrange()

        assertNull(viewModel.state.value.backupRootKey)
    }

    @Test
    fun givenStoredKey_whenViewModelIsCreated_thenStateContainsKeyInfo() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(BACKUP_ROOT_KEY))
            .arrange()

        assertEquals(BACKUP_ROOT_KEY.id, viewModel.state.value.backupRootKey?.id)
        assertEquals(BACKUP_ROOT_KEY.fingerprint(), viewModel.state.value.backupRootKey?.fingerprint)
    }

    @Test
    fun givenGenerationSucceeds_whenGeneratingNewKey_thenStateContainsGeneratedKey() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withGenerateBackupRootKey(
                GenerateAndForcePushBackupRootKeyResult.Success(BACKUP_ROOT_KEY, PushBackupRootKeyResult.Success)
            )
            .arrange()

        viewModel.generateNewBackupRootKey()

        assertEquals(BACKUP_ROOT_KEY.id, viewModel.state.value.backupRootKey?.id)
        assertEquals(false, viewModel.state.value.isGenerating)
    }

    @Test
    fun givenGenerationFails_whenGeneratingNewKey_thenErrorMessageIsEmitted() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withGenerateBackupRootKey(
                GenerateAndForcePushBackupRootKeyResult.Failure(
                    GenerateBackupRootKeyResult.Failure.StorageFailure(IllegalStateException("boom"))
                )
            )
            .arrange()

        viewModel.infoMessage.test {
            viewModel.generateNewBackupRootKey()

            assertEquals(
                UIText.DynamicString("Failed to generate Backup Root Key: StorageFailure(cause=java.lang.IllegalStateException: boom)"),
                awaitItem()
            )
        }
    }

    @Test
    fun givenNoStoredKeyAndFetchSucceeds_whenFetchingBackupRootKey_thenStateContainsFetchedKey() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withSyncBackupRootKey(SyncBackupRootKeyResult.Found(BACKUP_ROOT_KEY))
            .arrange()

        viewModel.infoMessage.test {
            viewModel.fetchBackupRootKey()

            assertEquals(UIText.DynamicString("Backup Root Key fetched from another client"), awaitItem())
        }

        assertEquals(BACKUP_ROOT_KEY.id, viewModel.state.value.backupRootKey?.id)
        assertEquals(false, viewModel.state.value.isFetchingBackupRootKey)
    }

    @Test
    fun givenNoCompatibleClientAnswers_whenFetchingBackupRootKey_thenUnavailableMessageIsEmitted() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withSyncBackupRootKey(SyncBackupRootKeyResult.Unavailable)
            .arrange()

        viewModel.infoMessage.test {
            viewModel.fetchBackupRootKey()

            assertEquals(UIText.DynamicString("Backup Root Key unavailable from other clients"), awaitItem())
        }

        assertNull(viewModel.state.value.backupRootKey)
        assertEquals(false, viewModel.state.value.isFetchingBackupRootKey)
    }

    @Test
    fun givenOnlineBackupSucceeds_whenCreatingBackup_thenUseCaseIsCalledAndSuccessMessageIsEmitted() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withCreateOnlineBackup(CREATE_BACKUP_SUCCESS)
            .arrange()

        viewModel.infoMessage.test {
            viewModel.createBackup()

            assertEquals(UIText.DynamicString("Backup created: backup.wbu"), awaitItem())
        }

        coVerify { arrangement.createOnlineBackup(any()) }
        assertEquals(false, viewModel.state.value.isCreatingBackup)
        assertEquals(0f, viewModel.state.value.backupCreationProgress)
    }

    @Test
    fun givenNoReceivedMessages_whenCreatingBackup_thenSkippedMessageIsEmitted() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withCreateOnlineBackup(CreateOnlineBackupResult.Skipped.NoReceivedMessages)
            .arrange()

        viewModel.infoMessage.test {
            viewModel.createBackup()

            assertEquals(UIText.DynamicString("Backup skipped: no received messages"), awaitItem())
        }
    }

    @Test
    fun givenBackupIsUpToDate_whenCreatingBackup_thenSkippedMessageIsEmitted() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withCreateOnlineBackup(
                CreateOnlineBackupResult.Skipped.UpToDate(
                    latestBackupTimestamp = Instant.parse("2026-06-06T12:00:00Z"),
                    latestMessageTimestamp = Instant.parse("2026-06-06T11:00:00Z"),
                )
            )
            .arrange()

        viewModel.infoMessage.test {
            viewModel.createBackup()

            assertEquals(UIText.DynamicString("Backup skipped: already up to date"), awaitItem())
        }
    }

    @Test
    fun givenBackupUploadFails_whenCreatingBackup_thenErrorMessageIsEmittedAndLoadingIsCleared() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withCreateOnlineBackup(CreateOnlineBackupResult.Failure.UploadFailed(CoreFailure.Unknown(RuntimeException("boom"))))
            .arrange()

        viewModel.infoMessage.test {
            viewModel.createBackup()

            assertEquals(
                UIText.DynamicString("Backup failed while uploading backup: Unknown(rootCause=java.lang.RuntimeException: boom)"),
                awaitItem()
            )
        }
        assertEquals(false, viewModel.state.value.isCreatingBackup)
    }

    @Test
    fun givenBackupCreationReportsProgress_whenCreatingBackup_thenProgressStateIsUpdated() = runTest {
        val backupStarted = CompletableDeferred<Unit>()
        val finishBackup = CompletableDeferred<Unit>()
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withCreateOnlineBackupProgress(
                progress = 0.42f,
                backupStarted = backupStarted,
                finishBackup = finishBackup,
            )
            .arrange()

        viewModel.createBackup()
        backupStarted.await()

        assertEquals(true, viewModel.state.value.isCreatingBackup)
        assertEquals(0.42f, viewModel.state.value.backupCreationProgress)

        finishBackup.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun givenExportRootKeyClicked_whenOpeningDialog_thenDialogStateIsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(BACKUP_ROOT_KEY))
            .arrange()

        viewModel.showExportBackupRootKeyPasswordDialog()

        assertTrue(viewModel.state.value.showExportBackupRootKeyPasswordDialog)
    }

    @Test
    fun givenExportSucceeds_whenExportingRootKey_thenPendingFileIsStoredAndCreateFileEffectIsEmitted() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(BACKUP_ROOT_KEY))
            .withExportBackupRootKey(EXPORT_BACKUP_ROOT_KEY_SUCCESS)
            .arrange()
        viewModel.exportBackupRootKeyPasswordState.setTextAndPlaceCursorAtEnd("password")

        viewModel.effect.test {
            viewModel.exportBackupRootKey()

            assertEquals(
                AutomaticBackupsDebugEffect.CreateBackupRootKeyExportFile("wire-backup-root-key-root-key-id.wbrk"),
                awaitItem()
            )
        }

        assertEquals(false, viewModel.state.value.isExportingBackupRootKey)
        assertEquals("wire-backup-root-key-root-key-id.wbrk", viewModel.state.value.pendingExportedBackupRootKey?.fileName)
    }

    @Test
    fun givenExportSaveUri_whenSavingRootKeyExport_thenFileIsCopiedAndSuccessMessageIsEmitted() = runTest {
        val uri = mockk<Uri>()
        val (arrangement, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(BACKUP_ROOT_KEY))
            .withExportBackupRootKey(EXPORT_BACKUP_ROOT_KEY_SUCCESS)
            .arrange()
        viewModel.exportBackupRootKeyPasswordState.setTextAndPlaceCursorAtEnd("password")
        viewModel.exportBackupRootKey()
        advanceUntilIdle()

        viewModel.infoMessage.test {
            viewModel.saveExportedBackupRootKey(uri)

            assertEquals(UIText.DynamicString("Backup Root Key exported"), awaitItem())
        }

        coVerify { arrangement.fileManager.copyToUri(EXPORT_BACKUP_ROOT_KEY_PATH, uri, any()) }
        assertNull(viewModel.state.value.pendingExportedBackupRootKey)
    }

    @Test
    fun givenExportSaveIsCancelled_whenSavingRootKeyExport_thenPendingFileIsClearedAndMessageIsEmitted() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(BACKUP_ROOT_KEY))
            .withExportBackupRootKey(EXPORT_BACKUP_ROOT_KEY_SUCCESS)
            .arrange()
        viewModel.exportBackupRootKeyPasswordState.setTextAndPlaceCursorAtEnd("password")
        viewModel.exportBackupRootKey()
        advanceUntilIdle()

        viewModel.infoMessage.test {
            viewModel.saveExportedBackupRootKey(null)

            assertEquals(UIText.DynamicString("Backup Root Key export save cancelled"), awaitItem())
        }

        assertNull(viewModel.state.value.pendingExportedBackupRootKey)
    }

    @Test
    fun givenExportFailsBecausePasswordIsBlank_whenExportingRootKey_thenMessageIsEmitted() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(BACKUP_ROOT_KEY))
            .withExportBackupRootKey(ExportBackupRootKeyResult.Failure.BlankPassword)
            .arrange()

        viewModel.infoMessage.test {
            viewModel.exportBackupRootKey()

            assertEquals(UIText.DynamicString("Enter a password to export Backup Root Key"), awaitItem())
        }
    }

    @Test
    fun givenImportFileUri_whenChoosingRootKeyToImport_thenFileIsCopiedAndPasswordDialogIsShown() = runTest {
        val uri = mockk<Uri>()
        val (arrangement, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .arrange()

        viewModel.chooseBackupRootKeyToImport(uri)
        advanceUntilIdle()

        coVerify { arrangement.fileManager.copyToPath(uri, IMPORT_BACKUP_ROOT_KEY_PATH, any()) }
        assertEquals(IMPORT_BACKUP_ROOT_KEY_PATH, viewModel.state.value.pendingImportedBackupRootKeyPath)
        assertEquals(true, viewModel.state.value.showImportBackupRootKeyPasswordDialog)
    }

    @Test
    fun givenImportDialogIsDismissed_whenDismissingRootKeyImport_thenPasswordAndPendingPathAreCleared() = runTest {
        val uri = mockk<Uri>()
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .arrange()
        viewModel.chooseBackupRootKeyToImport(uri)
        advanceUntilIdle()
        viewModel.importBackupRootKeyPasswordState.setTextAndPlaceCursorAtEnd("password")

        viewModel.dismissImportBackupRootKeyPasswordDialog()

        assertEquals(false, viewModel.state.value.showImportBackupRootKeyPasswordDialog)
        assertNull(viewModel.state.value.pendingImportedBackupRootKeyPath)
        assertEquals("", viewModel.importBackupRootKeyPasswordState.text.toString())
    }

    @Test
    fun givenImportSucceeds_whenImportingRootKey_thenStateContainsImportedKeyAndSuccessMessageIsEmitted() = runTest {
        val uri = mockk<Uri>()
        val (arrangement, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withImportBackupRootKey(ImportBackupRootKeyResult.Success(BACKUP_ROOT_KEY))
            .arrange()
        viewModel.chooseBackupRootKeyToImport(uri)
        advanceUntilIdle()
        viewModel.importBackupRootKeyPasswordState.setTextAndPlaceCursorAtEnd("password")

        viewModel.infoMessage.test {
            viewModel.importBackupRootKey()

            assertEquals(UIText.DynamicString("Backup Root Key imported"), awaitItem())
        }

        coVerify { arrangement.importBackupRootKey(IMPORT_BACKUP_ROOT_KEY_PATH, "password") }
        assertEquals(BACKUP_ROOT_KEY.id, viewModel.state.value.backupRootKey?.id)
        assertEquals(false, viewModel.state.value.isImportingBackupRootKey)
        assertEquals(false, viewModel.state.value.showImportBackupRootKeyPasswordDialog)
        assertNull(viewModel.state.value.pendingImportedBackupRootKeyPath)
    }

    @Test
    fun givenImportFailsBecausePasswordIsBlank_whenImportingRootKey_thenMessageIsEmitted() = runTest {
        val uri = mockk<Uri>()
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withImportBackupRootKey(ImportBackupRootKeyResult.Failure.BlankPassword)
            .arrange()
        viewModel.chooseBackupRootKeyToImport(uri)
        advanceUntilIdle()

        viewModel.infoMessage.test {
            viewModel.importBackupRootKey()

            assertEquals(UIText.DynamicString("Enter a password to import Backup Root Key"), awaitItem())
        }
    }

    @Test
    fun givenImportFailsBecausePasswordIsWrong_whenImportingRootKey_thenMessageIsEmitted() = runTest {
        val uri = mockk<Uri>()
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withImportBackupRootKey(ImportBackupRootKeyResult.Failure.AuthenticationFailure)
            .arrange()
        viewModel.chooseBackupRootKeyToImport(uri)
        advanceUntilIdle()
        viewModel.importBackupRootKeyPasswordState.setTextAndPlaceCursorAtEnd("wrong-password")

        viewModel.infoMessage.test {
            viewModel.importBackupRootKey()

            assertEquals(UIText.DynamicString("Backup Root Key import failed: wrong password"), awaitItem())
        }
    }

    @Test
    fun givenImportFileCopyFails_whenChoosingRootKeyToImport_thenMessageIsEmittedAndPendingPathIsCleared() = runTest {
        val uri = mockk<Uri>()
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withImportFileCopyFailure(IllegalStateException("boom"))
            .arrange()

        viewModel.infoMessage.test {
            viewModel.chooseBackupRootKeyToImport(uri)

            assertEquals(UIText.DynamicString("Failed to read Backup Root Key import: boom"), awaitItem())
        }

        assertNull(viewModel.state.value.pendingImportedBackupRootKeyPath)
        assertEquals(false, viewModel.state.value.showImportBackupRootKeyPasswordDialog)
    }

    private class Arrangement {
        @MockK
        lateinit var getBackupRootKey: GetBackupRootKeyUseCase

        @MockK
        lateinit var syncBackupRootKey: SyncBackupRootKeyUseCase

        @MockK
        lateinit var generateBackupRootKey: GenerateAndForcePushBackupRootKeyUseCase

        @MockK
        lateinit var exportBackupRootKey: ExportBackupRootKeyUseCase

        @MockK
        lateinit var importBackupRootKey: ImportBackupRootKeyUseCase

        @MockK
        lateinit var createOnlineBackup: CreateOnlineBackupUseCase

        @MockK
        lateinit var restoreLatestOnlineBackup: RestoreLatestOnlineBackupUseCase

        @MockK
        lateinit var kaliumFileSystem: KaliumFileSystem

        @MockK
        lateinit var fileManager: FileManager

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { kaliumFileSystem.tempFilePath("imported-backup-root-key.wbrk") } returns IMPORT_BACKUP_ROOT_KEY_PATH
            coEvery { fileManager.copyToUri(any(), any(), any()) } returns Unit
            coEvery { fileManager.copyToPath(any(), any(), any()) } returns 1L
        }

        fun withGetBackupRootKey(result: GetBackupRootKeyResult) = apply {
            coEvery { getBackupRootKey() } returns result
        }

        fun withGenerateBackupRootKey(result: GenerateAndForcePushBackupRootKeyResult) = apply {
            coEvery { generateBackupRootKey() } returns result
        }

        fun withSyncBackupRootKey(result: SyncBackupRootKeyResult) = apply {
            coEvery { syncBackupRootKey() } returns result
        }

        fun withCreateOnlineBackup(result: CreateOnlineBackupResult) = apply {
            coEvery { createOnlineBackup(any()) } returns result
        }

        fun withExportBackupRootKey(result: ExportBackupRootKeyResult) = apply {
            coEvery { exportBackupRootKey(any()) } returns result
        }

        fun withImportBackupRootKey(result: ImportBackupRootKeyResult) = apply {
            coEvery { importBackupRootKey(any(), any()) } returns result
        }

        fun withImportFileCopyFailure(cause: Throwable) = apply {
            coEvery { fileManager.copyToPath(any(), any(), any()) } throws cause
        }

        fun withCreateOnlineBackupProgress(
            progress: Float,
            backupStarted: CompletableDeferred<Unit>,
            finishBackup: CompletableDeferred<Unit>,
        ) = apply {
            coEvery { createOnlineBackup(any()) } coAnswers {
                firstArg<(Float) -> Unit>().invoke(progress)
                backupStarted.complete(Unit)
                finishBackup.await()
                CREATE_BACKUP_SUCCESS
            }
        }

        fun arrange(): Pair<Arrangement, AutomaticBackupsDebugViewModel> =
            this to AutomaticBackupsDebugViewModel(
                getBackupRootKey = getBackupRootKey,
                syncBackupRootKey = syncBackupRootKey,
                generateAndForcePushBackupRootKey = generateBackupRootKey,
                exportBackupRootKey = exportBackupRootKey,
                importBackupRootKey = importBackupRootKey,
                createOnlineBackup = createOnlineBackup,
                restoreLatestOnlineBackup = restoreLatestOnlineBackup,
                kaliumFileSystem = kaliumFileSystem,
                fileManager = fileManager,
                dispatcher = TestDispatcherProvider(),
            )
    }

    private companion object {
        val EXPORT_BACKUP_ROOT_KEY_PATH = "/tmp/wire-backup-root-key-root-key-id.wbrk".toPath()
        val IMPORT_BACKUP_ROOT_KEY_PATH = "/tmp/imported-backup-root-key.wbrk".toPath()
        val EXPORT_BACKUP_ROOT_KEY_SUCCESS = ExportBackupRootKeyResult.Success(
            exportFilePath = EXPORT_BACKUP_ROOT_KEY_PATH,
            fileName = "wire-backup-root-key-root-key-id.wbrk",
        )
        val BACKUP_ROOT_KEY = BackupRootKey(
            id = "backup-root-key-id",
            keyMaterial = ByteArray(32) { it.toByte() },
            createdAt = Instant.parse("2026-06-06T12:00:00Z"),
            createdByClientId = ClientId("client-id"),
            version = 1,
        )
        val CREATE_BACKUP_SUCCESS = CreateOnlineBackupResult.Success(
            OnlineBackupMetadata(
                backupId = "backup-id",
                userId = UserId("user-id", "wire.com"),
                clientId = "client-id",
                fileName = "backup.wbu",
                lastMessageDate = Instant.parse("2026-06-06T12:00:00Z"),
                assetId = UploadedAssetId("asset-key", "wire.com"),
                rootKeyId = "backup-root-key-id",
                encryptionAlgorithm = "AES256",
            )
        )
    }
}
