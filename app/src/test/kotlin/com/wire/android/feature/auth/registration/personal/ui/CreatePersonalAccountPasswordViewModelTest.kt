package com.wire.android.feature.auth.registration.personal.ui

import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.ui.dialog.NetworkErrorMessage
import com.wire.android.feature.auth.registration.personal.usecase.EmailInUse
import com.wire.android.feature.auth.registration.personal.usecase.InvalidEmailActivationCode
import com.wire.android.feature.auth.registration.personal.usecase.RegisterPersonalAccountParams
import com.wire.android.feature.auth.registration.personal.usecase.RegisterPersonalAccountUseCase
import com.wire.android.feature.auth.registration.personal.usecase.SessionCannotBeCreated
import com.wire.android.feature.auth.registration.personal.usecase.UnauthorizedEmail
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.framework.livedata.shouldNotBeUpdated
import com.wire.android.shared.user.password.InvalidPasswordFailure
import com.wire.android.shared.user.password.ValidatePasswordParams
import com.wire.android.shared.user.password.ValidatePasswordUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CreatePersonalAccountPasswordViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var validatePasswordUseCase: ValidatePasswordUseCase

    @MockK
    private lateinit var registerUseCase: RegisterPersonalAccountUseCase

    private lateinit var viewModel: CreatePersonalAccountPasswordViewModel

    @Before
    fun setUp() {
        viewModel = CreatePersonalAccountPasswordViewModel(
            coroutinesTestRule.dispatcherProvider, validatePasswordUseCase, registerUseCase
        )
    }

    @Test
    fun `given a validatePasswordUseCase, when minPasswordLength() is called, then returns minLength constraint of the use case`() {
        val minLength = 4
        every { validatePasswordUseCase.minLength() } returns minLength

        val result = viewModel.minPasswordLength()

        result shouldBeEqualTo minLength
        verify(exactly = 1) { validatePasswordUseCase.minLength() }
    }

    @Test
    fun `given a password, when validatePassword is called, then calls validatePasswordUseCase with correct params`() {
        coEvery { validatePasswordUseCase.run(any()) } returns Either.Right(Unit)

        viewModel.validatePassword(TEST_PASSWORD)

        viewModel.confirmationButtonEnabledLiveData shouldBeUpdated { }
        coVerify(exactly = 1) { validatePasswordUseCase.run(ValidatePasswordParams(TEST_PASSWORD)) }
    }

    @Test
    fun `given validatePassword is called, when useCase returns success, then sets continueEnabledLiveData to true`() {
        coEvery { validatePasswordUseCase.run(any()) } returns Either.Right(Unit)

        viewModel.validatePassword(TEST_PASSWORD)

        viewModel.confirmationButtonEnabledLiveData shouldBeUpdated { it shouldBe true }
    }

    @Test
    fun `given validatePassword is called, when useCase returns InvalidPasswordFailure, then sets continueEnabledLiveData to false`() {
        coEvery { validatePasswordUseCase.run(any()) } returns Either.Left(InvalidPasswordFailure)

        viewModel.validatePassword(TEST_PASSWORD)

        viewModel.confirmationButtonEnabledLiveData shouldBeUpdated { it shouldBe false }
    }

    @Test
    fun `given validatePassword is called, when useCase returns general Failure, then sets continueEnabledLiveData to false`() {
        val failure = mockk<Failure>()
        coEvery { validatePasswordUseCase.run(any()) } returns Either.Left(failure)

        viewModel.validatePassword(TEST_PASSWORD)

        viewModel.confirmationButtonEnabledLiveData shouldBeUpdated { it shouldBe false }
    }

    @Test
    fun `given params, when registerUser is called, then calls registerUseCase with correct params`() {
        coEvery { registerUseCase.run(any()) } returns Either.Right(Unit)

        viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

        viewModel.registrationStatusLiveData shouldBeUpdated {}

        coVerify(exactly = 1) {
            registerUseCase.run(
                RegisterPersonalAccountParams(
                    name = TEST_NAME, email = TEST_EMAIL,
                    password = TEST_PASSWORD, activationCode = TEST_ACTIVATION_CODE
                )
            )
        }
    }

    @Test
    fun `given registerUser is called, when use case returns success, then sets success to registerStatusLiveData`() {
        coEvery { registerUseCase.run(any()) } returns Either.Right(Unit)

        viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

        viewModel.registrationStatusLiveData shouldBeUpdated { result ->
            result shouldSucceed { it shouldBe Unit }
        }
    }

    @Test
    fun `given registerUser is called, when use case returns SessionCannotBeCreated error, then logs the user out`() {
        coEvery { registerUseCase.run(any()) } returns Either.Left(SessionCannotBeCreated)

        viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

        //TODO: assertion about logout
        viewModel.registrationStatusLiveData.shouldNotBeUpdated()
    }

    @Test
    fun `given registerUser is called, when use case returns NetworkConnection error, then sets NetworkError to registerStatusLiveData`() {
        coEvery { registerUseCase.run(any()) } returns Either.Left(NetworkConnection)

        viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

        viewModel.registrationStatusLiveData shouldBeUpdated { result ->
            result shouldFail { it shouldBe NetworkErrorMessage }
        }
    }

    @Test
    fun `given registerUser is called, when use case returns UnauthorizedEmail error, then sets error message to registerStatusLiveData`() {
        coEvery { registerUseCase.run(any()) } returns Either.Left(UnauthorizedEmail)

        viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

        viewModel.registrationStatusLiveData shouldBeUpdated { result ->
            result shouldFail { it.message shouldBeEqualTo R.string.create_personal_account_unauthorized_email_error }
        }
    }

    @Test
    fun `given registerUser is called, when use case returns InvalidEmailActivationCode, then sets error msg to registerStatusLiveData`() {
        coEvery { registerUseCase.run(any()) } returns Either.Left(InvalidEmailActivationCode)

        viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

        viewModel.registrationStatusLiveData shouldBeUpdated { result ->
            result shouldFail { it.message shouldBeEqualTo R.string.create_personal_account_invalid_activation_code_error }
        }
    }

    @Test
    fun `given registerUser is called, when use case returns EmailInUse error, then sets error message to registerStatusLiveData`() {
        coEvery { registerUseCase.run(any()) } returns Either.Left(EmailInUse)

        viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

        viewModel.registrationStatusLiveData shouldBeUpdated { result ->
            result shouldFail { it.message shouldBeEqualTo R.string.create_personal_account_email_in_use_error }
        }
    }

    @Test
    fun `given registerUser is called, when use case returns other error, then sets GeneralErrorMessage to registerStatusLiveData`() {
        coEvery { registerUseCase.run(any()) } returns Either.Left(ServerError)

        viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

        viewModel.registrationStatusLiveData shouldBeUpdated { result ->
            result shouldFail { it shouldBe GeneralErrorMessage }
        }
    }

    companion object {
        private const val TEST_PASSWORD = "123ABCdef!*"
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_NAME = "Name Surname"
        private const val TEST_ACTIVATION_CODE = "123456"
    }
}
