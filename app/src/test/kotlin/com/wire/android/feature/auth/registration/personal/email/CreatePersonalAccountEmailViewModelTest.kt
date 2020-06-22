package com.wire.android.feature.auth.registration.personal.email

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.executors.OneShotUseCaseExecutor
import com.wire.android.feature.auth.activation.usecase.EmailBlacklisted
import com.wire.android.feature.auth.activation.usecase.EmailInUse
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeParams
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.user.email.EmailInvalid
import com.wire.android.shared.user.email.EmailTooShort
import com.wire.android.shared.user.email.ValidateEmailUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule

import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class CreatePersonalAccountEmailViewModelTest : UnitTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var emailViewModel: CreatePersonalAccountEmailViewModel

    private val oneShotUseCaseExecutor: OneShotUseCaseExecutor = OneShotUseCaseExecutor()

    @Mock
    private lateinit var sendActivationCodeUseCase: SendEmailActivationCodeUseCase

    @Mock
    private lateinit var validateEmailUseCase: ValidateEmailUseCase

    @Before
    fun setUp() {
        emailViewModel = CreatePersonalAccountEmailViewModel(oneShotUseCaseExecutor, validateEmailUseCase, sendActivationCodeUseCase)
    }

    @Test
    fun `given validateEmail is called, when the validation succeeds then isValidEmail should be true`() =
        runBlockingTest {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Right(Unit))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isTrue()
        }

    @Test
    fun `given validateEmail is called, when the validation fails with EmailTooShort error then isValidEmail should be false`() =
        runBlockingTest {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Left(EmailTooShort))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isFalse()
        }

    @Test
    fun `given validateEmail is called, when the validation fails with EmailInvalid error then isValidEmail should be false`() =
        runBlockingTest {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Left(EmailInvalid))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isFalse()
        }

    @Test
    fun `given sendActivation is called, then calls SendEmailActivationCodeUseCase`() = runBlockingTest {
        val params = SendEmailActivationCodeParams(TEST_EMAIL)
        `when`(sendActivationCodeUseCase.run(params)).thenReturn(Either.Right(Unit))

        emailViewModel.sendActivationCode(TEST_EMAIL)
        verify(sendActivationCodeUseCase).run(params)
    }

    @Test
    fun `given sendActivation is called, when use case is successful, then sets success to sendActivationCodeLiveData`() =
        runBlockingTest {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Right(Unit))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            assertThat(emailViewModel.sendActivationCodeLiveData.awaitValue()).isEqualTo(Either.Right(Unit))
        }

    @Test
    fun `given sendActivation is called, when use case returns networkError, then updates networkConnectionErrorLiveData`() =
        runBlockingTest {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Left(NetworkConnection))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            assertThat(emailViewModel.networkConnectionErrorLiveData.awaitValue()).isEqualTo(Unit)
        }

    @Test
    fun `given sendActivation is called, when use case returns EmailBlacklisted, then sets error message to sendActivationCodeLiveData`() =
        runBlockingTest {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Left(EmailBlacklisted))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue().assertLeft {
                assertThat(it.message).isEqualTo(R.string.create_personal_account_with_email_email_blacklisted_error)
            }
        }

    @Test
    fun `given sendActivation is called, when use case returns EmailInUse, then sets error message to sendActivationCodeLiveData`() =
        runBlockingTest {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Left(EmailInUse))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue().assertLeft {
                assertThat(it.message).isEqualTo(R.string.create_personal_account_with_email_email_in_use_error)
            }
        }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
    }
}
