package com.wire.android.feature.auth.registration.ui

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.shared.user.username.CheckUsernameExistsUseCase
import com.wire.android.shared.user.username.UsernameAlreadyExists
import com.wire.android.shared.user.username.UsernameGeneralError
import com.wire.android.shared.user.username.UsernameInvalid
import com.wire.android.shared.user.username.UsernameTooLong
import com.wire.android.shared.user.username.UsernameTooShort
import com.wire.android.shared.user.username.ValidateUsernameUseCase
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class CreateAccountUsernameViewModelTest : UnitTest() {

    private lateinit var usernameViewModel: CreateAccountUsernameViewModel

    @MockK
    private lateinit var validateUsernameUseCase: ValidateUsernameUseCase

    @MockK
    private lateinit var checkUsernameExistsUseCase: CheckUsernameExistsUseCase

    @Before
    fun setup() {
        usernameViewModel = CreateAccountUsernameViewModel(validateUsernameUseCase, checkUsernameExistsUseCase)
    }

    @Test
    fun `given validateUsername is called, when use case returns UsernameTooShort error, then propagates error through usernameLiveData`() =
        runBlocking {
            coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(UsernameTooShort)

            usernameViewModel.validateUsername(TEST_USERNAME)

            assertUsernameErrors(R.string.create_account_with_username_error_too_short)
        }

    @Test
    fun `given validateUsername is called, when use case returns UsernameTooLong error, then propagate error through usernameLiveData`() =
        runBlocking {
            coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(UsernameTooLong)

            usernameViewModel.validateUsername(TEST_USERNAME)

            assertUsernameErrors(R.string.create_account_with_username_error_too_long)
        }

    @Test
    fun `given validateUsername is called, when use case returns UsernameInvalid error, then propagate error through usernameLiveData`() =
        runBlocking {
            coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(UsernameInvalid)

            usernameViewModel.validateUsername(TEST_USERNAME)

            assertUsernameErrors(R.string.create_account_with_username_error_invalid_characters)
        }

    @Test
    fun `given validateUsername is called, when use case returns UsernameGeneralError error, then propagate error`() =
        runBlocking {
            coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(UsernameGeneralError)

            usernameViewModel.validateUsername(TEST_USERNAME)

            assertUsernameErrors(R.string.general_error_dialog_message)
        }

    @Test
    fun `given validateUsername is called, when validation use case returns success, then enable confirmation button`() = runBlocking {
        coEvery { validateUsernameUseCase.run(any()) } returns Either.Right(TEST_USERNAME)

        usernameViewModel.validateUsername(TEST_USERNAME)

        usernameViewModel.confirmationButtonEnabled shouldBeUpdated {
            it shouldBe true
        }
    }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns UsernameAlreadyExists, then propagate error`() =
        runBlocking {
            coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Left(UsernameAlreadyExists)

            usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

            assertUsernameErrors(R.string.create_account_with_username_error_already_taken)
        }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns UsernameGeneralError, then propagate error`() =
        runBlocking {
            coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Left(UsernameGeneralError)

            usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

            assertUsernameErrors(R.string.general_error_dialog_message)
        }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns UsernameInvalid, then propagate error`() =
        runBlocking {
            coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Left(UsernameInvalid)

            usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

            assertUsernameErrors(R.string.create_account_with_username_error_invalid_characters)
        }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns NetworkConnection, then propagate error`() =
        runBlocking {
            coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Left(NetworkConnection)
            usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

            assertGeneralErrors(R.string.network_error_dialog_message)
        }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns ServerError, then propagate error`() =
        runBlocking {
            coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Left(ServerError)
            usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

            assertGeneralErrors(R.string.general_error_dialog_message)
        }

    @Test
    fun `given validateUsername, when validate username use case returns NetworkConnection, then propagate error`() =
        runBlocking {
            coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(NetworkConnection)
            usernameViewModel.validateUsername(TEST_USERNAME)

            assertGeneralErrors(R.string.network_error_dialog_message)
        }

    @Test
    fun `given validateUsername, when validate username use case returns ServerError, then propagate error`() =
        runBlocking {
            coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(ServerError)
            usernameViewModel.validateUsername(TEST_USERNAME)

            assertGeneralErrors(R.string.general_error_dialog_message)
        }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns UsernameIsAvailable, then propagate username`() =
        runBlocking {
            coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Right(TEST_USERNAME)

            usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

            usernameViewModel.confirmationButtonEnabled shouldBeUpdated {
                it shouldBe true
            }
            usernameViewModel.usernameLiveData shouldBeUpdated { result ->
                result shouldSucceed { it shouldBeEqualTo TEST_USERNAME }
            }
        }

    private fun assertGeneralErrors(@StringRes message: Int) {
        usernameViewModel.confirmationButtonEnabled shouldBeUpdated {
            it shouldBe false
        }
        usernameViewModel.dialogErrorLiveData shouldBeUpdated {
            it.message shouldBeEqualTo message
        }
    }

    private fun assertUsernameErrors(@StringRes message: Int) {
        usernameViewModel.confirmationButtonEnabled shouldBeUpdated {
            it shouldBe false
        }
        usernameViewModel.usernameLiveData shouldBeUpdated { result ->
            result shouldFail { it.message shouldBeEqualTo message }
        }
    }


    companion object {
        private const val TEST_USERNAME = "username"
    }
}
