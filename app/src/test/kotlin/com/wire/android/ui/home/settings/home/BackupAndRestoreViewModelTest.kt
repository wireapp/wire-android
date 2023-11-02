/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.settings.home

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.net.toUri
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.ui.home.settings.backup.BackupAndRestoreState
import com.wire.android.ui.home.settings.backup.BackupAndRestoreViewModel
import com.wire.android.ui.home.settings.backup.BackupCreationProgress
import com.wire.android.ui.home.settings.backup.BackupRestoreProgress
import com.wire.android.ui.home.settings.backup.PasswordValidation
import com.wire.android.ui.home.settings.backup.RestoreFileValidation
import com.wire.android.util.FileManager
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.backup.CreateBackupResult
import com.wire.kalium.logic.feature.backup.CreateBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreBackupResult
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.BackupIOFailure
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.IncompatibleBackup
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.InvalidPassword
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.InvalidUserId
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.Failure
import com.wire.kalium.logic.feature.backup.RestoreBackupUseCase
import com.wire.kalium.logic.feature.backup.VerifyBackupResult
import com.wire.kalium.logic.feature.backup.VerifyBackupUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class BackupAndRestoreViewModelTest {

    private val dispatcher = TestDispatcherProvider()

    @Test
    fun givenAnEmptyPassword_whenCreatingABackup_thenItCreatesItSuccessfully() = runTest(dispatcher.default()) {
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
        assert(backupAndRestoreViewModel.state.backupCreationProgress is BackupCreationProgress.Finished)
        coVerify(exactly = 1) { arrangement.createBackupFile(password = emptyPassword) }
    }

    @Test
    fun givenANonEmptyPassword_whenCreatingABackup_thenItCreatesItSuccessfully() = runTest(dispatcher.default()) {
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
        assert(backupAndRestoreViewModel.state.backupCreationProgress is BackupCreationProgress.Finished)
        coVerify(exactly = 1) { arrangement.createBackupFile(password = password) }
    }

    @Test
    fun givenAnEmptyPassword_whenValidating_thenItUpdatePasswordStateToValid() = runTest(dispatcher.default()) {
        // Given
        val password = ""
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withInvalidPassword()
            .arrange()

        // When
        backupAndRestoreViewModel.validateBackupCreationPassword(TextFieldValue(password))
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.passwordValidation.isValid)
        coVerify(exactly = 0) { arrangement.validatePassword(any()) }
    }

    @Test
    fun givenANonEmptyPassword_whenItIsInvalid_thenItUpdatePasswordValidationState() = runTest(dispatcher.default()) {
        // Given
        val password = "mayTh3ForceBeWIthYou"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withInvalidPassword()
            .arrange()

        // When
        backupAndRestoreViewModel.validateBackupCreationPassword(TextFieldValue(password))
        advanceUntilIdle()

        // Then
        assert(!backupAndRestoreViewModel.state.passwordValidation.isValid)
    }

    @Test
    fun givenANonEmptyPassword_whenItIsValid_thenItUpdatePasswordValidationState() = runTest(dispatcher.default()) {
        // Given
        val password = "mayTh3ForceBeWIthYou_"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withValidPassword()
            .arrange()

        // When
        backupAndRestoreViewModel.validateBackupCreationPassword(TextFieldValue(password))
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.passwordValidation.isValid)
    }

    @Test
    fun givenANonEmptyPassword_whenCreatingABackupWithAGivenError_thenItReturnsAFailure() = runTest(dispatcher.default()) {
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
    fun givenACreatedBackup_whenSharingIt_thenTheStateIsReset() = runTest(dispatcher.default()) {
        // Given
        val storedBackup = BackupAndRestoreState.CreatedBackup("backupFilePath".toPath(), "backupName.zip", 100L, true)
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withPreviouslyCreatedBackup(storedBackup)
            .arrange()

        // When
        backupAndRestoreViewModel.shareBackup()
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.latestCreatedBackup == storedBackup)
        assert(backupAndRestoreViewModel.state == BackupAndRestoreState.INITIAL_STATE)
        coVerify(exactly = 1) {
            arrangement.fileManager.shareWithExternalApp(
                storedBackup.path,
                storedBackup.assetName,
                any()
            )
        }
    }

    @Test
    fun givenACreatedBackup_whenSavingIt_thenTheStateIsReset() = runTest(dispatcher.default()) {
        // Given
        val storedBackup = BackupAndRestoreState.CreatedBackup("backupFilePath".toPath(), "backupName.zip", 100L, true)
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withPreviouslyCreatedBackup(storedBackup)
            .arrange()
        val backupUri = "some-backup".toUri()

        // When
        backupAndRestoreViewModel.saveBackup(backupUri)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.latestCreatedBackup == storedBackup)
        assert(backupAndRestoreViewModel.state == BackupAndRestoreState.INITIAL_STATE)
        coVerify(exactly = 1) {
            arrangement.fileManager.copyToUri(
                storedBackup.path,
                backupUri,
                any()
            )
        }
    }

    @Test
    fun givenANonEncryptedBackup_whenChoosingIt_thenTheRestoreProgressUpdatesCorrectly() = runTest(dispatcher.default()) {
        // Given
        val isBackupEncrypted = false
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withSuccessfulDBImport(isBackupEncrypted)
            .arrange()
        val backupUri = "some-backup".toUri()

        // When
        backupAndRestoreViewModel.chooseBackupFileToRestore(backupUri)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.backupRestoreProgress == BackupRestoreProgress.Finished)
        assert(backupAndRestoreViewModel.state.restoreFileValidation == RestoreFileValidation.ValidNonEncryptedBackup)
        assert(arrangement.fakeKaliumFileSystem.exists(backupAndRestoreViewModel.latestImportedBackupTempPath))
        coVerify(exactly = 1) {
            arrangement.fileManager.copyToPath(backupUri, backupAndRestoreViewModel.latestImportedBackupTempPath, any())
        }
    }

    @Test
    fun givenAStoredEncryptedBackup_whenChoosingIt_thenTheRequirePasswordDialogIsShown() = runTest(dispatcher.default()) {
        // Given
        val isBackupEncrypted = true
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withSuccessfulDBImport(isBackupEncrypted)
            .arrange()
        val backupUri = "some-backup".toUri()

        // When
        backupAndRestoreViewModel.chooseBackupFileToRestore(backupUri)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.restoreFileValidation == RestoreFileValidation.PasswordRequired)
        assert(arrangement.fakeKaliumFileSystem.exists(backupAndRestoreViewModel.latestImportedBackupTempPath))
        coVerify(exactly = 1) {
            arrangement.fileManager.copyToPath(backupUri, backupAndRestoreViewModel.latestImportedBackupTempPath, any())
        }
    }

    @Test
    fun givenAStoredBackup_whenThereIsAnErrorVerifyingItsEncryption_thenTheRightErrorDialogIsShown() = runTest(dispatcher.default()) {
        // Given
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withFailedBackupVerification()
            .arrange()
        val backupUri = "some-backup".toUri()

        // When
        backupAndRestoreViewModel.chooseBackupFileToRestore(backupUri)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.restoreFileValidation == RestoreFileValidation.IncompatibleBackup)
        assert(arrangement.fakeKaliumFileSystem.exists(backupAndRestoreViewModel.latestImportedBackupTempPath))
        coVerify(exactly = 1) {
            arrangement.fileManager.copyToPath(backupUri, backupAndRestoreViewModel.latestImportedBackupTempPath, any())
        }
    }

    @Test
    fun givenAStoredBackup_whenThereIsAnErrorImportingTheDB_thenTheRightErrorDialogIsShown() = runTest(dispatcher.default()) {
        // Given
        val backupUri = "some-backup".toUri()
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withFailedDBImport()
            .arrange()

        // When
        backupAndRestoreViewModel.chooseBackupFileToRestore(backupUri)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.restoreFileValidation == RestoreFileValidation.IncompatibleBackup)
        assert(backupAndRestoreViewModel.state.backupRestoreProgress == BackupRestoreProgress.Failed)
        assert(arrangement.fakeKaliumFileSystem.exists(backupAndRestoreViewModel.latestImportedBackupTempPath))
        coVerify(exactly = 1) {
            arrangement.fileManager.copyToPath(backupUri, backupAndRestoreViewModel.latestImportedBackupTempPath, any())
        }
    }

    @Test
    fun givenARestoreDialogShown_whenDismissingIt_thenTheTempImportedBackupPathIsDeleted() = runTest(dispatcher.default()) {
        // Given
        val mockUri = "some-backup"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withSuccessfulDBImport(false)
            .arrange()
        val backupUri = mockUri.toUri()

        // When
        backupAndRestoreViewModel.chooseBackupFileToRestore(backupUri)
        advanceUntilIdle()
        backupAndRestoreViewModel.cancelBackupRestore()
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.restoreFileValidation == RestoreFileValidation.Initial)
        assert(backupAndRestoreViewModel.state.backupRestoreProgress == BackupRestoreProgress.InProgress(0f))
        assert(backupAndRestoreViewModel.state.restorePasswordValidation == PasswordValidation.NotVerified)
        assert(!arrangement.fakeKaliumFileSystem.exists(backupAndRestoreViewModel.latestImportedBackupTempPath))
        coVerify(exactly = 1) {
            arrangement.fileManager.copyToPath(backupUri, backupAndRestoreViewModel.latestImportedBackupTempPath, any())
        }
    }

    @Test
    fun givenAPasswordEncryptedBackup_whenRestoringIt_thenTheCorrectSuccessDialogIsShown() = runTest(dispatcher.default()) {
        // Given
        val password = "some-password"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withSuccessfulBackupRestore()
            .withRequestedPasswordDialog()
            .arrange()

        // When
        backupAndRestoreViewModel.restorePasswordProtectedBackup(password)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.backupRestoreProgress == BackupRestoreProgress.Finished)
        assert(backupAndRestoreViewModel.state.restorePasswordValidation == PasswordValidation.Valid)
        assert(!arrangement.fakeKaliumFileSystem.exists(backupAndRestoreViewModel.latestImportedBackupTempPath))
        coVerify(exactly = 1) {
            arrangement.importBackup(any(), password)
        }
    }

    @Test
    fun givenAPasswordEncryptedBackup_whenRestoringWithWrongPassword_thenTheCorrectErrorDialogIsShown() = runTest(dispatcher.default()) {
        // Given
        val password = "some-password"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withFailedDBImport(Failure(InvalidPassword))
            .withRequestedPasswordDialog()
            .arrange()

        // When
        backupAndRestoreViewModel.restorePasswordProtectedBackup(password)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.backupRestoreProgress == BackupRestoreProgress.Failed)
        assert(backupAndRestoreViewModel.state.restoreFileValidation == RestoreFileValidation.PasswordRequired)
        assert(backupAndRestoreViewModel.state.restorePasswordValidation == PasswordValidation.NotValid)
        coVerify(exactly = 1) {
            arrangement.importBackup(any(), password)
        }
    }

    @Test
    fun givenAPasswordEncryptedBackup_whenRestoringAnInvalidUserIdBackup_thenTheCorrectErrorDialogIsShown() =
        runTest(dispatcher.default()) {
            // Given
            val password = "some-password"
            val (arrangement, backupAndRestoreViewModel) = Arrangement()
                .withFailedDBImport(Failure(InvalidUserId))
                .withRequestedPasswordDialog()
                .arrange()

            // When
            backupAndRestoreViewModel.restorePasswordProtectedBackup(password)
            advanceUntilIdle()

            // Then
            assert(backupAndRestoreViewModel.state.backupRestoreProgress == BackupRestoreProgress.Failed)
            assert(backupAndRestoreViewModel.state.restoreFileValidation == RestoreFileValidation.WrongBackup)
            assert(backupAndRestoreViewModel.state.restorePasswordValidation == PasswordValidation.Valid)
            coVerify(exactly = 1) {
                arrangement.importBackup(any(), password)
            }
        }

    @Test
    fun givenAPasswordEncryptedBackup_whenRestoringAnIncompatibleBackup_thenTheCorrectErrorDialogIsShown() = runTest(dispatcher.default()) {
        // Given
        val password = "some-password"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withFailedDBImport(Failure(IncompatibleBackup("old format backup")))
            .withRequestedPasswordDialog()
            .arrange()

        // When
        backupAndRestoreViewModel.restorePasswordProtectedBackup(password)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.backupRestoreProgress == BackupRestoreProgress.Failed)
        assert(backupAndRestoreViewModel.state.restoreFileValidation == RestoreFileValidation.IncompatibleBackup)
        assert(backupAndRestoreViewModel.state.restorePasswordValidation == PasswordValidation.Valid)
        coVerify(exactly = 1) {
            arrangement.importBackup(any(), password)
        }
    }

    @Test
    fun givenAPasswordEncryptedBackup_whenRestoringABackupWithAnIOError_thenTheCorrectErrorDialogIsShown() = runTest(dispatcher.default()) {
        // Given
        val password = "some-password"
        val (arrangement, backupAndRestoreViewModel) = Arrangement()
            .withFailedDBImport(Failure(BackupIOFailure("IO error")))
            .withRequestedPasswordDialog()
            .withValidPassword()
            .arrange()

        // When
        backupAndRestoreViewModel.restorePasswordProtectedBackup(password)
        advanceUntilIdle()

        // Then
        assert(backupAndRestoreViewModel.state.backupRestoreProgress == BackupRestoreProgress.Failed)
        assert(backupAndRestoreViewModel.state.restoreFileValidation == RestoreFileValidation.GeneralFailure)
        assert(backupAndRestoreViewModel.state.restorePasswordValidation == PasswordValidation.Valid)
        coVerify(exactly = 1) {
            arrangement.importBackup(any(), password)
        }
    }

    private inner class Arrangement {

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)
            val mockUri = mockk<Uri>()
            mockkStatic(Uri::class)
            every { Uri.parse("some-backup") } returns mockUri
            coEvery { importBackup(any(), any()) } returns RestoreBackupResult.Success
            coEvery { createBackupFile(any()) } returns CreateBackupResult.Success("".toPath(), 0L, "")
            coEvery { verifyBackup(any()) } returns VerifyBackupResult.Success.Encrypted
        }

        @MockK
        lateinit var importBackup: RestoreBackupUseCase

        @MockK
        lateinit var createBackupFile: CreateBackupUseCase

        @MockK
        private lateinit var verifyBackup: VerifyBackupUseCase

        @MockK
        lateinit var validatePassword: ValidatePasswordUseCase

        @MockK
        lateinit var fileManager: FileManager

        val fakeKaliumFileSystem = FakeKaliumFileSystem()

        private val viewModel = BackupAndRestoreViewModel(
            importBackup = importBackup,
            createBackupFile = createBackupFile,
            verifyBackup = verifyBackup,
            kaliumFileSystem = fakeKaliumFileSystem,
            dispatcher = dispatcher,
            fileManager = fileManager,
            validatePassword = validatePassword
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
            viewModel.state = viewModel.state.copy(backupCreationProgress = BackupCreationProgress.Finished(backup.assetName))
        }

        fun withSuccessfulBackupRestore() = apply {
            viewModel.latestImportedBackupTempPath =
                fakeKaliumFileSystem.tempFilePath(BackupAndRestoreViewModel.TEMP_IMPORTED_BACKUP_FILE_NAME)
            coEvery { importBackup(any(), any()) } returns RestoreBackupResult.Success
        }

        fun withRequestedPasswordDialog() = apply {
            viewModel.state = viewModel.state.copy(restoreFileValidation = RestoreFileValidation.PasswordRequired)
        }

        fun withSuccessfulDBImport(isEncrypted: Boolean) = apply {
            coEvery { fileManager.copyToPath(any(), any(), any()) } returns (100L).also {
                viewModel.latestImportedBackupTempPath =
                    fakeKaliumFileSystem.tempFilePath(BackupAndRestoreViewModel.TEMP_IMPORTED_BACKUP_FILE_NAME)
                fakeKaliumFileSystem.sink(viewModel.latestImportedBackupTempPath).buffer().use {
                    it.write("someBackupData".toByteArray())
                }
            }

            coEvery { verifyBackup(any()) } returns
                    if (isEncrypted) VerifyBackupResult.Success.Encrypted else VerifyBackupResult.Success.NotEncrypted
            coEvery { importBackup(any(), any()) } returns RestoreBackupResult.Success
        }

        fun withFailedBackupVerification() = apply {
            coEvery { fileManager.copyToPath(any(), any(), any()) } returns (100L).also {
                viewModel.latestImportedBackupTempPath =
                    fakeKaliumFileSystem.tempFilePath(BackupAndRestoreViewModel.TEMP_IMPORTED_BACKUP_FILE_NAME)
                fakeKaliumFileSystem.sink(viewModel.latestImportedBackupTempPath).buffer().use {
                    it.write("someBackupData".toByteArray())
                }
            }

            coEvery { verifyBackup(any()) } returns VerifyBackupResult.Failure.InvalidBackupFile
        }

        fun withFailedDBImport(error: Failure = Failure(IncompatibleBackup("DB failed to import"))) = apply {
            coEvery { fileManager.copyToPath(any(), any(), any()) } returns (100L).also {
                viewModel.latestImportedBackupTempPath =
                    fakeKaliumFileSystem.tempFilePath(BackupAndRestoreViewModel.TEMP_IMPORTED_BACKUP_FILE_NAME)
                fakeKaliumFileSystem.sink(viewModel.latestImportedBackupTempPath).buffer().use {
                    it.write("someBackupData".toByteArray())
                }
            }

            coEvery { verifyBackup(any()) } returns VerifyBackupResult.Success.NotEncrypted
            coEvery { importBackup(any(), any()) } returns error
        }

        fun withValidPassword() = apply {
            every { validatePassword(any()) } returns ValidatePasswordResult.Valid
        }

        fun withInvalidPassword() = apply {
            every { validatePassword(any()) } returns ValidatePasswordResult.Invalid()
        }

        fun arrange() = this to viewModel
    }
}
