package com.wire.android.ui.home.settings.home

import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.settings.backup.BackupAndRestoreState
import com.wire.android.ui.home.settings.backup.BackupAndRestoreViewModel
import com.wire.android.ui.home.settings.backup.BackupCreationProgress
import com.wire.android.util.FileManager
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.feature.backup.CreateBackupResult
import com.wire.kalium.logic.feature.backup.CreateBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreBackupUseCase
import com.wire.kalium.logic.feature.backup.VerifyBackupUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.IOException
import okio.Path.Companion.toPath
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupAndRestoreViewModelTest {
    @Test
    fun givenAnEmptyPassword_whenCreatingABackup_thenItCreatesItSuccessfully() = runTest {
        // Given
        val emptyPassword = ""
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withSuccessfulCreation(emptyPassword)
            .arrange()

        // When
        backupAndRestoreViewModel.createBackup(emptyPassword)

        // Then
        assert(backupAndRestoreViewModel.latestCreatedBackup?.isEncrypted == false)
        assertEquals(backupAndRestoreViewModel.state.backupRestoreProgress, BackupCreationProgress.Finished)
        coVerify(exactly = 1) { arrangement.createBackupFile(password = emptyPassword) }
    }

    @Test
    fun givenANonEmptyPassword_whenCreatingABackup_thenItCreatesItSuccessfully() = runTest {
        // Given
        val password = "mayTh3ForceBeWIthYou"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withSuccessfulCreation(password)
            .arrange()

        // When
        backupAndRestoreViewModel.createBackup(password)

        // Then
        assert(backupAndRestoreViewModel.latestCreatedBackup?.isEncrypted == true)
        assertEquals(backupAndRestoreViewModel.state.backupRestoreProgress, BackupCreationProgress.Finished)
        coVerify(exactly = 1) { arrangement.createBackupFile(password = password) }
    }

    @Test
    fun givenANonEmptyPassword_whenCreatingABackupWithAGivenError_thenItReturnsAFailure() = runTest {
        // Given
        val password = "mayTh3ForceBeWIthYou"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withFailedCreation(password)
            .arrange()

        // When
        backupAndRestoreViewModel.createBackup(password)

        // Then
        assertEquals(backupAndRestoreViewModel.state.backupRestoreProgress, BackupCreationProgress.Failed)
        assert(backupAndRestoreViewModel.latestCreatedBackup == null)
        coVerify(exactly = 1) { arrangement.createBackupFile(password = password) }
    }

    @Test
    fun givenACreatedBackup_whenSavingIt_thenTheStateIsReset() = runTest {
        // Given
        val password = "mayTh3ForceBeWIthYou"

        val storedBackup = BackupAndRestoreState.CreatedBackup("backupFilePath".toPath(), "backupName.zip", 100L, true)
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withPreviouslyCreatedBackup(storedBackup)
            .arrange()

        // When
        backupAndRestoreViewModel.saveBackup()

        // Then
        assert(backupAndRestoreViewModel.latestCreatedBackup == storedBackup)
        coVerify(exactly = 1) { arrangement.createBackupFile(password = password) }
    }

    private inner class Arrangement {

        @MockK
        private lateinit var importBackup: RestoreBackupUseCase

        @MockK
        lateinit var createBackupFile: CreateBackupUseCase

        @MockK
        private lateinit var verifyBackup: VerifyBackupUseCase

        @MockK
        private lateinit var fileManager: FileManager

        private val fakeKaliumFileSystem = FakeKaliumFileSystem()

        private val viewModel = BackupAndRestoreViewModel(
            navigationManager = NavigationManager(),
            importBackup = importBackup,
            createBackupFile = createBackupFile,
            verifyBackup = verifyBackup,
            kaliumFileSystem = fakeKaliumFileSystem,
            fileManager = fileManager
        )

        fun withSuccessfulCreation(password: String): Arrangement = apply {
            val backupFilePath = "some-file-path".toPath()
            val backupSize = 1000L
            val backupName = "some-backup.zip"
            coEvery { createBackupFile(eq(password)) } returns CreateBackupResult.Success(backupFilePath, backupSize, backupName)
        }

        fun withFailedCreation(password: String): Arrangement = apply {
            coEvery { createBackupFile(eq(password)) } returns CreateBackupResult.Failure(CoreFailure.Unknown(IOException("Some db error")))
        }

        fun withPreviouslyCreatedBackup(backup: BackupAndRestoreState.CreatedBackup): Arrangement = apply {
            viewModel.latestCreatedBackup = backup
            viewModel.state = BackupAndRestoreState.INITIAL_STATE.copy(backupCreationProgress = BackupCreationProgress.Finished)
        }

        fun arrange() = this to viewModel
    }
}
