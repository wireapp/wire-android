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

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.asset.UploadedAssetId
import com.wire.kalium.logic.data.backup.OnlineBackupMetadata
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.backup.BackupRootKey
import com.wire.kalium.logic.feature.backup.CreateOnlineBackupResult
import com.wire.kalium.logic.feature.backup.CreateOnlineBackupUseCase
import com.wire.kalium.logic.feature.backup.GenerateBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GenerateBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.GetBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GetBackupRootKeyUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
            .withGenerateBackupRootKey(GenerateBackupRootKeyResult.Success(BACKUP_ROOT_KEY))
            .arrange()

        viewModel.generateNewBackupRootKey()

        assertEquals(BACKUP_ROOT_KEY.id, viewModel.state.value.backupRootKey?.id)
        assertEquals(false, viewModel.state.value.isGenerating)
    }

    @Test
    fun givenGenerationFails_whenGeneratingNewKey_thenErrorMessageIsEmitted() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetBackupRootKey(GetBackupRootKeyResult.Success(null))
            .withGenerateBackupRootKey(GenerateBackupRootKeyResult.Failure.StorageFailure(IllegalStateException("boom")))
            .arrange()

        viewModel.infoMessage.test {
            viewModel.generateNewBackupRootKey()

            assertEquals(UIText.DynamicString("Failed to generate Backup Root Key: boom"), awaitItem())
        }
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

    private class Arrangement {
        @MockK
        lateinit var getBackupRootKey: GetBackupRootKeyUseCase

        @MockK
        lateinit var generateBackupRootKey: GenerateBackupRootKeyUseCase

        @MockK
        lateinit var createOnlineBackup: CreateOnlineBackupUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withGetBackupRootKey(result: GetBackupRootKeyResult) = apply {
            coEvery { getBackupRootKey() } returns result
        }

        fun withGenerateBackupRootKey(result: GenerateBackupRootKeyResult) = apply {
            coEvery { generateBackupRootKey() } returns result
        }

        fun withCreateOnlineBackup(result: CreateOnlineBackupResult) = apply {
            coEvery { createOnlineBackup(any()) } returns result
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
                generateBackupRootKey = generateBackupRootKey,
                createOnlineBackup = createOnlineBackup,
            )
    }

    private companion object {
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
