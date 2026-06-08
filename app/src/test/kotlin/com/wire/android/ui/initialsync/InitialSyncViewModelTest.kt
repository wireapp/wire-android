/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.initialsync

import android.net.Uri
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.util.FileManager
import com.wire.android.util.lifecycle.AutomatedLoginManager
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.asset.UploadedAssetId
import com.wire.kalium.logic.data.backup.OnlineBackupMetadata
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.backup.BackupRootKey
import com.wire.kalium.logic.feature.backup.ImportBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.ImportBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.RestoreLatestOnlineBackupResult
import com.wire.kalium.logic.feature.backup.RestoreLatestOnlineBackupUseCase
import com.wire.kalium.logic.feature.conversation.SyncConversationsUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class InitialSyncViewModelTest {

    @Test
    fun `given sync is live, when observing initial sync state, then navigate home`() = runTest {
        // given
        val (viewModel, arrangement) = Arrangement()
            .withRestoreLatestOnlineBackup(RESTORE_BACKUP_SUCCESS)
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()

        // then
        assertTrue(viewModel.isSyncCompleted)
        assertFalse(viewModel.isRestoringBackup)
        assertEquals(SyncCompletionState(shouldMoveToBackground = false), viewModel.syncCompletionState)
        coVerify(exactly = 1) { arrangement.restoreLatestOnlineBackup(any()) }
    }

    @Test
    fun `given sync is not live, when observing initial sync state, then stay on this screen`() = runTest {
        // given
        val (viewModel, arrangement) = Arrangement()
            .withSyncState(SyncState.Waiting)
            .arrange()
        // when
        arrangement.withSyncState(SyncState.GatheringPendingEvents)
        arrangement.withSyncState(SyncState.SlowSync)

        advanceUntilIdle()

        // then
        assertFalse(viewModel.isSyncCompleted)
        assertEquals(null, viewModel.syncCompletionState)
        coVerify(exactly = 0) { arrangement.restoreLatestOnlineBackup(any()) }
    }

    @Test
    fun `given restore is running, when observing initial sync state, then keep initial sync in progress`() = runTest {
        // given
        val restoreStarted = CompletableDeferred<Unit>()
        val finishRestore = CompletableDeferred<Unit>()
        val (viewModel, _) = Arrangement()
            .withSuspendedRestoreLatestOnlineBackup(restoreStarted, finishRestore)
            .withSyncState(SyncState.Live)
            .arrange()

        // when
        advanceTimeBy(DefaultDurationMillis.toLong())
        runCurrent()
        restoreStarted.await()

        // then
        assertTrue(viewModel.isRestoringBackup)
        assertFalse(viewModel.isSyncCompleted)
        assertEquals(null, viewModel.syncCompletionState)

        finishRestore.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `given no online backup exists, when restoring after initial sync, then complete without toast`() = runTest {
        // given
        val restoreErrorToasts = mutableListOf<Int>()
        val (viewModel, _) = Arrangement()
            .withRestoreLatestOnlineBackup(RestoreLatestOnlineBackupResult.Failure.NoOnlineBackupFound)
            .withSyncState(SyncState.Live)
            .arrange()
        val job = launch { viewModel.restoreErrorToast.collect { restoreErrorToasts.add(it) } }

        advanceUntilIdle()

        // then
        assertTrue(viewModel.isSyncCompleted)
        assertEquals(emptyList<Int>(), restoreErrorToasts)
        job.cancel()
    }

    @Test
    fun `given no backup root key is available, when restoring after initial sync, then show dialog and stay on this screen`() = runTest {
        // given
        val restoreErrorToasts = mutableListOf<Int>()
        val (viewModel, _) = Arrangement()
            .withRestoreLatestOnlineBackup(RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable)
            .withSyncState(SyncState.Live)
            .arrange()
        val job = launch { viewModel.restoreErrorToast.collect { restoreErrorToasts.add(it) } }

        advanceUntilIdle()

        // then
        assertFalse(viewModel.isSyncCompleted)
        assertTrue(viewModel.showBackupRootKeyUnavailableDialog)
        assertEquals(emptyList<Int>(), restoreErrorToasts)
        job.cancel()
    }

    @Test
    fun `given no backup root key is available, when trying again succeeds, then complete initial sync`() = runTest {
        // given
        val (viewModel, arrangement) = Arrangement()
            .withRestoreLatestOnlineBackupResults(
                RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable,
                RESTORE_BACKUP_SUCCESS
            )
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()
        assertTrue(viewModel.showBackupRootKeyUnavailableDialog)

        // when
        viewModel.onBackupRootKeyDialogTryAgain()
        advanceUntilIdle()

        // then
        assertTrue(viewModel.isSyncCompleted)
        assertFalse(viewModel.showBackupRootKeyUnavailableDialog)
        assertEquals(SyncCompletionState(shouldMoveToBackground = false), viewModel.syncCompletionState)
        coVerify(exactly = 2) { arrangement.restoreLatestOnlineBackup(any()) }
    }

    @Test
    fun `given no backup root key is available, when trying again fails the same way, then show dialog again`() = runTest {
        // given
        val (viewModel, arrangement) = Arrangement()
            .withRestoreLatestOnlineBackupResults(
                RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable,
                RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable
            )
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()
        assertTrue(viewModel.showBackupRootKeyUnavailableDialog)

        // when
        viewModel.onBackupRootKeyDialogTryAgain()
        advanceUntilIdle()

        // then
        assertFalse(viewModel.isSyncCompleted)
        assertTrue(viewModel.showBackupRootKeyUnavailableDialog)
        assertEquals(null, viewModel.syncCompletionState)
        coVerify(exactly = 2) { arrangement.restoreLatestOnlineBackup(any()) }
    }

    @Test
    fun `given no backup root key is available, when cancelling dialog, then complete initial sync`() = runTest {
        // given
        val restoreErrorToasts = mutableListOf<Int>()
        val (viewModel, _) = Arrangement()
            .withAutomatedLoginPending()
            .withRestoreLatestOnlineBackup(RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable)
            .withSyncState(SyncState.Live)
            .arrange()
        val job = launch { viewModel.restoreErrorToast.collect { restoreErrorToasts.add(it) } }

        advanceUntilIdle()
        assertTrue(viewModel.showBackupRootKeyUnavailableDialog)

        // when
        viewModel.onBackupRootKeyDialogCancel()
        advanceUntilIdle()

        // then
        assertTrue(viewModel.isSyncCompleted)
        assertTrue(viewModel.shouldMoveToBackground)
        assertFalse(viewModel.showBackupRootKeyUnavailableDialog)
        assertEquals(SyncCompletionState(shouldMoveToBackground = true), viewModel.syncCompletionState)
        assertEquals(emptyList<Int>(), restoreErrorToasts)
        job.cancel()
    }

    @Test
    fun `given no backup root key is available, when import file is selected, then show password dialog`() = runTest {
        // given
        val uri = mockk<Uri>()
        val (viewModel, arrangement) = Arrangement()
            .withRestoreLatestOnlineBackup(RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable)
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()
        assertTrue(viewModel.showBackupRootKeyUnavailableDialog)

        // when
        viewModel.onBackupRootKeyImportFileSelected(uri)
        advanceUntilIdle()

        // then
        coVerify { arrangement.fileManager.copyToPath(uri, IMPORT_BACKUP_ROOT_KEY_PATH, any()) }
        assertFalse(viewModel.showBackupRootKeyUnavailableDialog)
        assertTrue(viewModel.showImportBackupRootKeyPasswordDialog)
        assertEquals(IMPORT_BACKUP_ROOT_KEY_PATH, viewModel.pendingImportedBackupRootKeyPath)
    }

    @Test
    fun `given no backup root key is available, when import file selection is cancelled, then show root key dialog again`() = runTest {
        // given
        val (viewModel, _) = Arrangement()
            .withRestoreLatestOnlineBackup(RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable)
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()

        // when
        viewModel.onBackupRootKeyImportFileSelected(null)
        advanceUntilIdle()

        // then
        assertTrue(viewModel.showBackupRootKeyUnavailableDialog)
        assertFalse(viewModel.showImportBackupRootKeyPasswordDialog)
        assertEquals(null, viewModel.pendingImportedBackupRootKeyPath)
    }

    @Test
    fun `given no backup root key is available, when import file copy fails, then emit toast and show root key dialog again`() = runTest {
        // given
        val uri = mockk<Uri>()
        val (viewModel, _) = Arrangement()
            .withRestoreLatestOnlineBackup(RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable)
            .withImportFileCopyFailure(IllegalStateException("boom"))
            .withSyncState(SyncState.Live)
            .arrange()
        val toast = async { viewModel.restoreErrorToast.first() }

        advanceUntilIdle()

        // when
        viewModel.onBackupRootKeyImportFileSelected(uri)
        advanceUntilIdle()

        // then
        assertEquals(R.string.initial_sync_import_backup_root_key_failed, toast.await())
        assertTrue(viewModel.showBackupRootKeyUnavailableDialog)
        assertFalse(viewModel.showImportBackupRootKeyPasswordDialog)
        assertEquals(null, viewModel.pendingImportedBackupRootKeyPath)
    }

    @Test
    fun `given import password dialog is shown, when dismissing dialog, then password and pending file are cleared`() = runTest {
        // given
        val uri = mockk<Uri>()
        val (viewModel, _) = Arrangement()
            .withRestoreLatestOnlineBackup(RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable)
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()
        viewModel.onBackupRootKeyImportFileSelected(uri)
        advanceUntilIdle()
        viewModel.importBackupRootKeyPasswordState.setTextAndPlaceCursorAtEnd("password")

        // when
        viewModel.onImportBackupRootKeyPasswordDialogDismiss()

        // then
        assertTrue(viewModel.showBackupRootKeyUnavailableDialog)
        assertFalse(viewModel.showImportBackupRootKeyPasswordDialog)
        assertFalse(viewModel.isImportingBackupRootKey)
        assertEquals(null, viewModel.pendingImportedBackupRootKeyPath)
        assertEquals("", viewModel.importBackupRootKeyPasswordState.text.toString())
    }

    @Test
    fun `given root key import succeeds, when restoring retry succeeds, then complete initial sync`() = runTest {
        // given
        val uri = mockk<Uri>()
        val (viewModel, arrangement) = Arrangement()
            .withRestoreLatestOnlineBackupResults(
                RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable,
                RESTORE_BACKUP_SUCCESS
            )
            .withImportBackupRootKey(ImportBackupRootKeyResult.Success(BACKUP_ROOT_KEY))
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()
        viewModel.onBackupRootKeyImportFileSelected(uri)
        advanceUntilIdle()
        viewModel.importBackupRootKeyPasswordState.setTextAndPlaceCursorAtEnd("password")

        // when
        viewModel.onImportBackupRootKey()
        advanceUntilIdle()

        // then
        coVerify { arrangement.importBackupRootKey(IMPORT_BACKUP_ROOT_KEY_PATH, "password") }
        coVerify(exactly = 2) { arrangement.restoreLatestOnlineBackup(any()) }
        assertTrue(viewModel.isSyncCompleted)
        assertFalse(viewModel.showBackupRootKeyUnavailableDialog)
        assertFalse(viewModel.showImportBackupRootKeyPasswordDialog)
        assertEquals(null, viewModel.pendingImportedBackupRootKeyPath)
        assertEquals("", viewModel.importBackupRootKeyPasswordState.text.toString())
    }

    @Test
    fun `given root key import fails because password is wrong, when importing, then emit toast and keep password dialog open`() = runTest {
        // given
        val uri = mockk<Uri>()
        val (viewModel, _) = Arrangement()
            .withRestoreLatestOnlineBackup(RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable)
            .withImportBackupRootKey(ImportBackupRootKeyResult.Failure.AuthenticationFailure)
            .withSyncState(SyncState.Live)
            .arrange()
        val toast = async { viewModel.restoreErrorToast.first() }

        advanceUntilIdle()
        viewModel.onBackupRootKeyImportFileSelected(uri)
        advanceUntilIdle()
        viewModel.importBackupRootKeyPasswordState.setTextAndPlaceCursorAtEnd("wrong-password")

        // when
        viewModel.onImportBackupRootKey()
        advanceUntilIdle()

        // then
        assertEquals(R.string.initial_sync_import_backup_root_key_wrong_password, toast.await())
        assertFalse(viewModel.isSyncCompleted)
        assertFalse(viewModel.isImportingBackupRootKey)
        assertFalse(viewModel.showBackupRootKeyUnavailableDialog)
        assertTrue(viewModel.showImportBackupRootKeyPasswordDialog)
        assertEquals(IMPORT_BACKUP_ROOT_KEY_PATH, viewModel.pendingImportedBackupRootKeyPath)
    }

    @Test
    fun `given root key import fails because file is invalid, when importing, then emit toast and keep password dialog open`() = runTest {
        // given
        val uri = mockk<Uri>()
        val (viewModel, _) = Arrangement()
            .withRestoreLatestOnlineBackup(RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable)
            .withImportBackupRootKey(ImportBackupRootKeyResult.Failure.InvalidFile)
            .withSyncState(SyncState.Live)
            .arrange()
        val toast = async { viewModel.restoreErrorToast.first() }

        advanceUntilIdle()
        viewModel.onBackupRootKeyImportFileSelected(uri)
        advanceUntilIdle()
        viewModel.importBackupRootKeyPasswordState.setTextAndPlaceCursorAtEnd("password")

        // when
        viewModel.onImportBackupRootKey()
        advanceUntilIdle()

        // then
        assertEquals(R.string.initial_sync_import_backup_root_key_invalid_file, toast.await())
        assertFalse(viewModel.isSyncCompleted)
        assertFalse(viewModel.isImportingBackupRootKey)
        assertFalse(viewModel.showBackupRootKeyUnavailableDialog)
        assertTrue(viewModel.showImportBackupRootKeyPasswordDialog)
        assertEquals(IMPORT_BACKUP_ROOT_KEY_PATH, viewModel.pendingImportedBackupRootKeyPath)
    }

    @Test
    fun `given restore fails, when restoring after initial sync, then emit toast and complete`() = runTest {
        // given
        val (viewModel, _) = Arrangement()
            .withRestoreLatestOnlineBackup(RestoreLatestOnlineBackupResult.Failure.RootKeyIdMismatch)
            .withSyncState(SyncState.Live)
            .arrange()
        val restoreErrorToast = async { viewModel.restoreErrorToast.first() }

        advanceUntilIdle()

        // then
        assertTrue(viewModel.isSyncCompleted)
        assertEquals(R.string.initial_sync_restore_backup_failed, restoreErrorToast.await())
    }

    @Test
    fun `given sync is live, when restoring backup, then sync conversations before restore`() = runTest {
        // given
        val (viewModel, arrangement) = Arrangement()
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()

        // then
        assertTrue(viewModel.isSyncCompleted)
        coVerifyOrder {
            arrangement.syncConversations()
            arrangement.restoreLatestOnlineBackup(any())
        }
    }

    @Test
    fun `given conversation sync fails, when restoring backup, then still try restore and complete`() = runTest {
        // given
        val (viewModel, arrangement) = Arrangement()
            .withSyncConversations(Either.Left(CoreFailure.Unknown(null)))
            .withRestoreLatestOnlineBackup(RESTORE_BACKUP_SUCCESS)
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()

        // then
        assertTrue(viewModel.isSyncCompleted)
        coVerify(exactly = 1) { arrangement.syncConversations() }
        coVerify(exactly = 1) { arrangement.restoreLatestOnlineBackup(any()) }
    }

    @Test
    fun `given automated login pending, when sync completes, then shouldMoveToBackground is true and flag is cleared`() = runTest {
        // given
        val (viewModel, arrangement) = Arrangement()
            .withAutomatedLoginPending()
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()

        // then
        assertTrue(viewModel.isSyncCompleted)
        assertTrue(viewModel.shouldMoveToBackground)
        assertEquals(SyncCompletionState(shouldMoveToBackground = true), viewModel.syncCompletionState)
        assertFalse(arrangement.automatedLoginManager.pendingMoveToBackgroundAfterSync)
    }

    @Test
    fun `given no automated login pending, when sync completes, then shouldMoveToBackground is false`() = runTest {
        // given
        val (viewModel, _) = Arrangement()
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()

        // then
        assertTrue(viewModel.isSyncCompleted)
        assertFalse(viewModel.shouldMoveToBackground)
        assertEquals(SyncCompletionState(shouldMoveToBackground = false), viewModel.syncCompletionState)
    }

    private class Arrangement {

        @MockK
        lateinit var observeSyncState: ObserveSyncStateUseCase

        @MockK
        lateinit var userDataStoreProvider: UserDataStoreProvider

        @MockK
        lateinit var userDataStore: UserDataStore

        @MockK
        lateinit var restoreLatestOnlineBackup: RestoreLatestOnlineBackupUseCase

        @MockK
        lateinit var importBackupRootKey: ImportBackupRootKeyUseCase

        @MockK
        lateinit var kaliumFileSystem: KaliumFileSystem

        @MockK
        lateinit var fileManager: FileManager

        @MockK
        lateinit var syncConversations: SyncConversationsUseCase

        val userId = UserId("id", "domain")

        val automatedLoginManager = AutomatedLoginManager()

        val viewModel by lazy {
            InitialSyncViewModel(
                observeSyncState,
                userDataStoreProvider,
                userId,
                TestDispatcherProvider(),
                automatedLoginManager,
                syncConversations,
                restoreLatestOnlineBackup,
                importBackupRootKey,
                kaliumFileSystem,
                fileManager,
            )
        }

        private val syncStateChannel = Channel<SyncState>(capacity = Channel.UNLIMITED)

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)
            // Default empty values
            mockUri()
            coEvery { userDataStoreProvider.getOrCreate(any()) } returns userDataStore
            coEvery { syncConversations() } returns Either.Right(Unit)
            coEvery { restoreLatestOnlineBackup(any()) } returns RestoreLatestOnlineBackupResult.Failure.NoOnlineBackupFound
            coEvery { importBackupRootKey(any(), any()) } returns ImportBackupRootKeyResult.Success(BACKUP_ROOT_KEY)
            every { kaliumFileSystem.tempFilePath("imported-backup-root-key.wbrk") } returns IMPORT_BACKUP_ROOT_KEY_PATH
            coEvery { fileManager.copyToPath(any(), any(), any()) } returns 1L
        }

        suspend fun withSyncState(syncState: SyncState): Arrangement {
            every { observeSyncState.invoke() } returns syncStateChannel.consumeAsFlow()
            syncStateChannel.send(syncState)
            return this
        }

        fun withAutomatedLoginPending(): Arrangement = apply {
            automatedLoginManager.markPendingMoveToBackgroundAfterSync()
        }

        fun withRestoreLatestOnlineBackup(result: RestoreLatestOnlineBackupResult) = apply {
            coEvery { restoreLatestOnlineBackup(any()) } returns result
        }

        fun withRestoreLatestOnlineBackupResults(vararg results: RestoreLatestOnlineBackupResult) = apply {
            coEvery { restoreLatestOnlineBackup(any()) } returnsMany results.toList()
        }

        fun withImportBackupRootKey(result: ImportBackupRootKeyResult) = apply {
            coEvery { importBackupRootKey(any(), any()) } returns result
        }

        fun withImportFileCopyFailure(cause: Throwable) = apply {
            coEvery { fileManager.copyToPath(any(), any(), any()) } throws cause
        }

        fun withSyncConversations(result: Either<CoreFailure, Unit>) = apply {
            coEvery { syncConversations() } returns result
        }

        fun withSuspendedRestoreLatestOnlineBackup(
            restoreStarted: CompletableDeferred<Unit>,
            finishRestore: CompletableDeferred<Unit>,
        ) = apply {
            coEvery { restoreLatestOnlineBackup(any()) } coAnswers {
                restoreStarted.complete(Unit)
                finishRestore.await()
                RESTORE_BACKUP_SUCCESS
            }
        }

        fun arrange() = viewModel to this
    }

    private companion object {
        val IMPORT_BACKUP_ROOT_KEY_PATH = "/tmp/imported-backup-root-key.wbrk".toPath()
        val BACKUP_ROOT_KEY = BackupRootKey(
            id = "backup-root-key-id",
            keyMaterial = ByteArray(32) { it.toByte() },
            createdAt = Instant.parse("2026-06-06T12:00:00Z"),
            createdByClientId = ClientId("client-id"),
            version = 1,
        )
        val RESTORE_BACKUP_SUCCESS = RestoreLatestOnlineBackupResult.Success(
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
