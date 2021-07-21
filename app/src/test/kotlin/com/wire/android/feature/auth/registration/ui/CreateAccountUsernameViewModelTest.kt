package com.wire.android.feature.auth.registration.ui

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.shared.user.username.CheckUsernameExistsUseCase
import com.wire.android.shared.user.username.GenerateRandomUsernameUseCase
import com.wire.android.shared.user.username.NoAvailableUsernames
import com.wire.android.shared.user.username.UpdateUsernameUseCase
import com.wire.android.shared.user.username.UsernameAlreadyExists
import com.wire.android.shared.user.username.UsernameGeneralError
import com.wire.android.shared.user.username.UsernameInvalid
import com.wire.android.shared.user.username.UsernameTooLong
import com.wire.android.shared.user.username.UsernameTooShort
import com.wire.android.shared.user.username.ValidateUsernameUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CreateAccountUsernameViewModelTest : UnitTest() {

    private lateinit var usernameViewModel: CreateAccountUsernameViewModel

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var validateUsernameUseCase: ValidateUsernameUseCase

    @MockK
    private lateinit var checkUsernameExistsUseCase: CheckUsernameExistsUseCase

    @MockK
    private lateinit var updateUsernameUseCase: UpdateUsernameUseCase

    @MockK
    private lateinit var generateUsernameUseCase: GenerateRandomUsernameUseCase

    @Before
    fun setup() {
        usernameViewModel = CreateAccountUsernameViewModel(
            coroutinesTestRule.dispatcherProvider,
            validateUsernameUseCase,
            checkUsernameExistsUseCase,
            updateUsernameUseCase,
            generateUsernameUseCase,

            )
    }

    @Test
    fun `given validateUsername is called, when use case returns UsernameTooShort error, then propagates error through usernameLiveData`() {
        coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(UsernameTooShort)

        usernameViewModel.validateUsername(TEST_USERNAME)

        assertUsernameErrors(R.string.create_account_with_username_error_too_short)
    }

    @Test
    fun `given validateUsername is called, when use case returns UsernameTooLong error, then propagate error through usernameLiveData`() {
        coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(UsernameTooLong)

        usernameViewModel.validateUsername(TEST_USERNAME)

        assertUsernameErrors(R.string.create_account_with_username_error_too_long)
    }

    @Test
    fun `given validateUsername is called, when use case returns UsernameInvalid error, then propagate error through usernameLiveData`() {
        coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(UsernameInvalid)

        usernameViewModel.validateUsername(TEST_USERNAME)

        assertUsernameErrors(R.string.create_account_with_username_error_invalid_characters)
    }

    @Test
    fun `given validateUsername is called, when use case returns UsernameGeneralError error, then propagate error`() {
        coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(UsernameGeneralError)

        usernameViewModel.validateUsername(TEST_USERNAME)

        assertUsernameErrors(R.string.general_error_dialog_message)
    }

    @Test
    fun `given validateUsername is called, when validation use case returns success, then enable confirmation button`() {
        coEvery { validateUsernameUseCase.run(any()) } returns Either.Right(TEST_USERNAME)

        usernameViewModel.validateUsername(TEST_USERNAME)

        usernameViewModel.confirmationButtonEnabled shouldBeUpdated {
            it shouldBe true
        }
    }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns UsernameAlreadyExists, then propagate error`() {
        coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Left(UsernameAlreadyExists)

        usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

        assertUsernameErrors(R.string.create_account_with_username_error_already_taken)
    }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns UsernameGeneralError, then propagate error`() {
        coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Left(UsernameGeneralError)

        usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

        assertUsernameErrors(R.string.general_error_dialog_message)
    }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns UsernameInvalid, then propagate error`() {
        coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Left(UsernameInvalid)

        usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

        assertUsernameErrors(R.string.create_account_with_username_error_invalid_characters)
    }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns NetworkConnection, then propagate error`() {
        coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Left(NetworkConnection)

        usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

        assertGeneralErrors(R.string.network_error_dialog_message)
    }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns ServerError, then propagate error`() {
        coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Left(ServerError)

        usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

        assertGeneralErrors(R.string.general_error_dialog_message)
    }

    @Test
    fun `given validateUsername, when validate username use case returns NetworkConnection, then propagate error`() {
        coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(NetworkConnection)
        usernameViewModel.validateUsername(TEST_USERNAME)

        assertGeneralErrors(R.string.network_error_dialog_message)
    }


    @Test
    fun `given validateUsername, when validate username use case returns ServerError, then propagate error`() {
        coEvery { validateUsernameUseCase.run(any()) } returns Either.Left(ServerError)
        usernameViewModel.validateUsername(TEST_USERNAME)

        assertGeneralErrors(R.string.general_error_dialog_message)
    }

    @Test
    fun `given onConfirmationButtonClicked, when check username use case returns UsernameIsAvailable, then update username`() {
        coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Right(TEST_USERNAME)
        coEvery { updateUsernameUseCase.run(any()) } returns Either.Right(Unit)

        usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

        usernameViewModel.confirmationButtonEnabled shouldBeUpdated {
            it shouldBe true
        }

        coVerify { updateUsernameUseCase.run(any()) }
    }

    @Test
    fun `given onConfirmationButtonClicked, when check username and update username succeeds, then propagate success`() {
        coEvery { checkUsernameExistsUseCase.run(any()) } returns Either.Right(TEST_USERNAME)
        coEvery { updateUsernameUseCase.run(any()) } returns Either.Right(Unit)

        usernameViewModel.onConfirmationButtonClicked(TEST_USERNAME)

        usernameViewModel.confirmationButtonEnabled shouldBeUpdated {
            it shouldBe true
        }

        usernameViewModel.usernameValidationLiveData.shouldBeUpdated {
            it shouldSucceed { it shouldBe Unit }
        }
    }

    @Test
    fun `given generateUsername is called, when use case returns NoAvailableUsernames error, then propagate generate error`() {
        coEvery { generateUsernameUseCase.run(Unit) } returns Either.Left(NoAvailableUsernames)

        usernameViewModel.generateUsername()

        assertGeneralErrors(R.string.general_error_dialog_message)
    }

    @Test
    fun `given generateUsername is called, when use case returns success, then propagate generated name`() {
        coEvery { generateUsernameUseCase.run(Unit) } returns Either.Right(TEST_USERNAME)

        usernameViewModel.generateUsername()

        usernameViewModel.generatedUsernameLiveData.shouldBeUpdated {
            it shouldBeEqualTo TEST_USERNAME
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
        usernameViewModel.usernameValidationLiveData shouldBeUpdated { result ->
            result shouldFail { it.message shouldBeEqualTo message }
        }
    }

    companion object {
        private const val TEST_USERNAME = "username"
    }
}
