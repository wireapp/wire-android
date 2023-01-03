package com.wire.android.ui.home.settings.home

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.settings.backup.BackupAndRestoreState
import com.wire.android.ui.home.settings.backup.BackupAndRestoreViewModel
import com.wire.android.ui.home.settings.backup.BackupCreationProgress
import com.wire.android.util.FileManager
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.feature.backup.CreateBackupResult
import com.wire.kalium.logic.feature.backup.CreateBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreBackupResult
import com.wire.kalium.logic.feature.backup.RestoreBackupUseCase
import com.wire.kalium.logic.feature.backup.VerifyBackupResult
import com.wire.kalium.logic.feature.backup.VerifyBackupUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.IOException
import okio.Path.Companion.toPath
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupAndRestoreViewModelTest {

    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun givenAnEmptyPassword_whenCreatingABackup_thenItCreatesItSuccessfully() = runTest(dispatcherProvider.main()) {
        // Given
        val emptyPassword = ""
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withSuccessfulCreation(emptyPassword)
            .arrange()

        // When
        backupAndRestoreViewModel.createBackup(emptyPassword)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.latestCreatedBackup?.isEncrypted == false)
        assertEquals(backupAndRestoreViewModel.state.backupCreationProgress, BackupCreationProgress.Finished)
        coVerify(exactly = 1) { arrangement.createBackupFile(password = emptyPassword) }
    }

    @Test
    fun givenANonEmptyPassword_whenCreatingABackup_thenItCreatesItSuccessfully() = runTest(dispatcherProvider.main()) {
        // Given
        val password = "mayTh3ForceBeWIthYou"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withSuccessfulCreation(password)
            .arrange()

        // When
        backupAndRestoreViewModel.createBackup(password)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.latestCreatedBackup?.isEncrypted == true)
        assertEquals(backupAndRestoreViewModel.state.backupCreationProgress, BackupCreationProgress.Finished)
        coVerify(exactly = 1) { arrangement.createBackupFile(password = password) }
    }

    @Test
    fun givenANonEmptyPassword_whenCreatingABackupWithAGivenError_thenItReturnsAFailure() = runTest(dispatcherProvider.main()) {
        // Given
        val password = "mayTh3ForceBeWIthYou"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withFailedCreation(password)
            .arrange()

        // When
        backupAndRestoreViewModel.createBackup(password)
        advanceUntilIdle()

        // Then
        assertEquals(backupAndRestoreViewModel.state.backupCreationProgress, BackupCreationProgress.Failed)
        assert(backupAndRestoreViewModel.latestCreatedBackup == null)
        coVerify(exactly = 1) { arrangement.createBackupFile(password = password) }
    }

    @Test
    fun givenACreatedBackup_whenSavingIt_thenTheStateIsReset() = runTest(dispatcherProvider.main()) {
        // Given
        val storedBackup = BackupAndRestoreState.CreatedBackup("backupFilePath".toPath(), "backupName.zip", 100L, true)
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withPreviouslyCreatedBackup(storedBackup)
            .arrange()

        // When
        backupAndRestoreViewModel.saveBackup()
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.latestCreatedBackup == storedBackup)
        assert(backupAndRestoreViewModel.state == BackupAndRestoreState.INITIAL_STATE)
        coVerify(exactly = 1) { arrangement.fileManager.shareWithExternalApp(any(), any(), any()) }
    }

    private inner class Arrangement {

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { importBackup(any(), any()) } returns RestoreBackupResult.Success
            coEvery { createBackupFile(any()) } returns CreateBackupResult.Success("".toPath(), 0L, "")
            coEvery { verifyBackup(any()) } returns VerifyBackupResult.Success.Encrypted
        }

        @MockK
        private lateinit var importBackup: RestoreBackupUseCase

        @MockK
        lateinit var createBackupFile: CreateBackupUseCase

        @MockK
        private lateinit var verifyBackup: VerifyBackupUseCase

        @MockK
        lateinit var fileManager: FileManager

        private val fakeKaliumFileSystem = FakeKaliumFileSystem()

        private val viewModel = BackupAndRestoreViewModel(
            navigationManager = NavigationManager(),
            importBackup = importBackup,
            createBackupFile = createBackupFile,
            verifyBackup = verifyBackup,
            kaliumFileSystem = fakeKaliumFileSystem,
            dispatcher = dispatcherProvider,
            fileManager = fileManager
        )

        fun withSuccessfulCreation(password: String) = apply {
            val backupFilePath = "some-file-path".toPath()
            val backupSize = 1000L
            val backupName = "some-backup.zip"
            coEvery { createBackupFile(eq(password)) } returns CreateBackupResult.Success(backupFilePath, backupSize, backupName)
        }

        fun withFailedCreation(password: String) = apply {
            coEvery { createBackupFile(eq(password)) } returns CreateBackupResult.Failure(CoreFailure.Unknown(IOException("Some db error")))
        }

        fun withPreviouslyCreatedBackup(backup: BackupAndRestoreState.CreatedBackup) = apply {
            viewModel.latestCreatedBackup = backup
            viewModel.state = BackupAndRestoreState.INITIAL_STATE.copy(backupCreationProgress = BackupCreationProgress.Finished)
        }

        fun withSuccessfulBackupRestore() = apply {
            coEvery { importBackup(any(), any()) } returns RestoreBackupResult.Success
        }

        fun arrange() = this to viewModel
    }
}
