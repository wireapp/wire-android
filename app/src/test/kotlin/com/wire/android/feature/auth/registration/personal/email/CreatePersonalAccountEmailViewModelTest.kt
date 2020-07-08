package com.wire.android.feature.auth.registration.personal.email

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.accessibility.Accessibility
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.activation.usecase.EmailBlacklisted
import com.wire.android.feature.auth.activation.usecase.EmailInUse
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeParams
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.user.email.EmailInvalid
import com.wire.android.shared.user.email.EmailTooShort
import com.wire.android.shared.user.email.ValidateEmailUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

    @Mock
    private lateinit var sendActivationCodeUseCase: SendEmailActivationCodeUseCase

    @Mock
    private lateinit var validateEmailUseCase: ValidateEmailUseCase

    @Mock
    private lateinit var accessibility: Accessibility

    @Before
    fun setUp() {
        emailViewModel = CreatePersonalAccountEmailViewModel(
            validateEmailUseCase,
            sendActivationCodeUseCase,
            accessibility
        )
    }

    @Test
    fun `given shouldFocusInput is queried, when talkback is not enabled, then return true`() {
        runBlocking {
            `when`(accessibility.isTalkbackEnabled()).thenReturn(false)
            assertThat(emailViewModel.shouldFocusInput()).isEqualTo(true)
        }
    }

    @Test
    fun `given shouldFocusInput is queried, when talkback is enabled, then return false `() {
        runBlocking {
            `when`(accessibility.isTalkbackEnabled()).thenReturn(true)
            assertThat(emailViewModel.shouldFocusInput()).isEqualTo(false)
        }
    }

    @Test
    fun `given validateEmail is called, when the validation succeeds then isValidEmail should be true`() {
        runBlocking {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Right(Unit))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isTrue()
        }
    }

    @Test
    fun `given validateEmail is called, when the validation fails with EmailTooShort error then isValidEmail should be false`() {
        runBlocking {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Left(EmailTooShort))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isFalse()
        }
    }

    @Test
    fun `given validateEmail is called, when the validation fails with EmailInvalid error then isValidEmail should be false`() {
        runBlocking {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Left(EmailInvalid))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isFalse()
        }
    }

    @Test
    fun `given sendActivation is called, then calls SendEmailActivationCodeUseCase`() {
        runBlocking {
            val params = SendEmailActivationCodeParams(TEST_EMAIL)
            `when`(sendActivationCodeUseCase.run(params)).thenReturn(Either.Right(Unit))

            emailViewModel.sendActivationCode(TEST_EMAIL)
            verify(sendActivationCodeUseCase).run(params)
        }
    }

    @Test
    fun `given sendActivation is called, when use case is successful, then sets email to sendActivationCodeLiveData`() {
        runBlocking {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Right(Unit))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue().assertRight {
                assertThat(it).isEqualTo(TEST_EMAIL)
            }
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns networkError, then updates networkConnectionErrorLiveData`() {
        runBlocking {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Left(NetworkConnection))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            assertThat(emailViewModel.networkConnectionErrorLiveData.awaitValue()).isEqualTo(Unit)
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns EmailBlacklisted, then sets error message to sendActivationCodeLiveData`() {
        runBlocking {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Left(EmailBlacklisted))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue().assertLeft {
                assertThat(it.message).isEqualTo(R.string.create_personal_account_with_email_email_blacklisted_error)
            }
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns EmailInUse, then sets error message to sendActivationCodeLiveData`() {
        runBlocking {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Left(EmailInUse))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue().assertLeft {
                assertThat(it.message).isEqualTo(R.string.create_personal_account_with_email_email_in_use_error)
            }
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
    }
}
