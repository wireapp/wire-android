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
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.feature.backup.BackupRootKey
import com.wire.kalium.logic.feature.backup.GenerateBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GenerateBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.GetBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GetBackupRootKeyUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
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

    private class Arrangement {
        @MockK
        lateinit var getBackupRootKey: GetBackupRootKeyUseCase

        @MockK
        lateinit var generateBackupRootKey: GenerateBackupRootKeyUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withGetBackupRootKey(result: GetBackupRootKeyResult) = apply {
            coEvery { getBackupRootKey() } returns result
        }

        fun withGenerateBackupRootKey(result: GenerateBackupRootKeyResult) = apply {
            coEvery { generateBackupRootKey() } returns result
        }

        fun arrange(): Pair<Arrangement, AutomaticBackupsDebugViewModel> =
            this to AutomaticBackupsDebugViewModel(
                getBackupRootKey = getBackupRootKey,
                generateBackupRootKey = generateBackupRootKey,
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
    }
}
