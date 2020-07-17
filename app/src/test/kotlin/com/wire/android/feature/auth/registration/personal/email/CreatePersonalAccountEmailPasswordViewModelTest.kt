package com.wire.android.feature.auth.registration.personal.email

import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.personal.email.usecase.EmailInUse
import com.wire.android.feature.auth.registration.personal.email.usecase.EmailRegistrationParams
import com.wire.android.feature.auth.registration.personal.email.usecase.InvalidEmailActivationCode
import com.wire.android.feature.auth.registration.personal.email.usecase.RegisterPersonalAccountWithEmailUseCase
import com.wire.android.feature.auth.registration.personal.email.usecase.UnauthorizedEmail
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.user.password.InvalidPasswordFailure
import com.wire.android.shared.user.password.ValidatePasswordParams
import com.wire.android.shared.user.password.ValidatePasswordUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

@ExperimentalCoroutinesApi
class CreatePersonalAccountEmailPasswordViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Mock
    private lateinit var validatePasswordUseCase: ValidatePasswordUseCase

    @Mock
    private lateinit var registerUseCase: RegisterPersonalAccountWithEmailUseCase

    private lateinit var viewModel: CreatePersonalAccountEmailPasswordViewModel

    @Before
    fun setUp() {
        viewModel = CreatePersonalAccountEmailPasswordViewModel(validatePasswordUseCase, registerUseCase)
    }

    @Test
    fun `given a validatePasswordUseCase, when minPasswordLength() is called, then returns minLength constraint of the use case`() {
        val minLength = 4
        `when`(validatePasswordUseCase.minLength()).thenReturn(minLength)

        assertThat(viewModel.minPasswordLength()).isEqualTo(minLength)
        verify(validatePasswordUseCase).minLength()
    }

    @Test
    fun `given a password, when validatePassword is called, then calls validatePasswordUseCase with correct params`() {
        runBlocking {
            `when`(validatePasswordUseCase.run(any())).thenReturn(Either.Right(Unit))

            viewModel.validatePassword(TEST_PASSWORD)

            viewModel.continueEnabledLiveData.awaitValue()
            verify(validatePasswordUseCase).run(ValidatePasswordParams(TEST_PASSWORD))
        }
    }

    @Test
    fun `given validatePassword is called, when useCase returns success, then sets continueEnabledLiveData to true`() {
        runBlocking {
            `when`(validatePasswordUseCase.run(any())).thenReturn(Either.Right(Unit))

            viewModel.validatePassword(TEST_PASSWORD)

            assertThat(viewModel.continueEnabledLiveData.awaitValue()).isTrue()
        }
    }

    @Test
    fun `given validatePassword is called, when useCase returns InvalidPasswordFailure, then sets continueEnabledLiveData to false`() {
        runBlocking {
            `when`(validatePasswordUseCase.run(any())).thenReturn(Either.Left(InvalidPasswordFailure))

            viewModel.validatePassword(TEST_PASSWORD)

            assertThat(viewModel.continueEnabledLiveData.awaitValue()).isFalse()
        }
    }

    @Test
    fun `given validatePassword is called, when useCase returns general Failure, then sets continueEnabledLiveData to false`() {
        runBlocking {
            val failure = mock(Failure::class.java)
            `when`(validatePasswordUseCase.run(any())).thenReturn(Either.Left(failure))

            viewModel.validatePassword(TEST_PASSWORD)

            assertThat(viewModel.continueEnabledLiveData.awaitValue()).isFalse()
        }
    }

    @Test
    fun `given params, when registerUser is called, then calls registerUseCase with correct params`() {
        runBlocking {
            `when`(registerUseCase.run(any())).thenReturn(Either.Right(Unit))

            viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            verify(registerUseCase).run(
                EmailRegistrationParams(
                    name = TEST_NAME,
                    email = TEST_EMAIL,
                    password = TEST_PASSWORD,
                    activationCode = TEST_ACTIVATION_CODE
                )
            )
        }
    }

    @Test
    fun `given registerUser is called, when use case returns success, then sets success to registerStatusLiveData`() {
        runBlocking {
            `when`(registerUseCase.run(any())).thenReturn(Either.Right(Unit))

            viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            viewModel.registerStatusLiveData.awaitValue().assertRight()
        }
    }

    @Test
    fun `given registerUser is called, when use case returns NetworkConnection error, then sets error to networkConnectionErrorLiveData`() {
        runBlocking {
            `when`(registerUseCase.run(any())).thenReturn(Either.Left(NetworkConnection))

            viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            assertThat(viewModel.networkConnectionErrorLiveData.awaitValue()).isEqualTo(Unit)
        }
    }

    @Test
    fun `given registerUser is called, when use case returns UnauthorizedEmail error, then sets error message to registerStatusLiveData`() {
        runBlocking {
            `when`(registerUseCase.run(any())).thenReturn(Either.Left(UnauthorizedEmail))

            viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            viewModel.registerStatusLiveData.awaitValue().assertLeft {
                assertThat(it.message).isEqualTo(R.string.create_personal_account_unauthorized_email_error)
            }
        }
    }

    @Test
    fun `given registerUser is called, when use case returns InvalidEmailActivationCode, then sets error msg to registerStatusLiveData`() {
        runBlocking {
            `when`(registerUseCase.run(any())).thenReturn(Either.Left(InvalidEmailActivationCode))

            viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            viewModel.registerStatusLiveData.awaitValue().assertLeft {
                assertThat(it.message).isEqualTo(R.string.create_personal_account_invalid_activation_code_error)
            }
        }
    }

    @Test
    fun `given registerUser is called, when use case returns EmailInUse error, then sets error message to registerStatusLiveData`() {
        runBlocking {
            `when`(registerUseCase.run(any())).thenReturn(Either.Left(EmailInUse))

            viewModel.registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            viewModel.registerStatusLiveData.awaitValue().assertLeft {
                assertThat(it.message).isEqualTo(R.string.create_personal_account_email_in_use_error)
            }
        }
    }

    companion object {
        private const val TEST_PASSWORD = "123ABCdef!*"
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_NAME = "Name Surname"
        private const val TEST_ACTIVATION_CODE = "123456"
    }
}
