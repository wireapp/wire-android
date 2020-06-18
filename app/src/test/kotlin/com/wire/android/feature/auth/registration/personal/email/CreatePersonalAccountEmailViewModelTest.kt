package com.wire.android.feature.auth.registration.personal.email

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.functional.Either
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.user.email.EmailInvalid
import com.wire.android.shared.user.email.EmailTooShort
import com.wire.android.shared.user.email.ValidateEmailUseCase
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before

import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class CreatePersonalAccountEmailViewModelTest: UnitTest() {

    private lateinit var emailViewModel: CreatePersonalAccountEmailViewModel

    @Mock
    private lateinit var validateEmailUseCase: ValidateEmailUseCase

    @Before
    fun setUp() {
        emailViewModel = CreatePersonalAccountEmailViewModel(validateEmailUseCase)
    }

    @Test
    fun `given validateEmail is called, when the validation succeeds then isValidEmail should be true`() {
        runBlocking {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Right(Unit))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue())
        }
    }

    @Test
    fun `given validateEmail is called, when the validation fails with EmailTooShort error then isValidEmail should be false`() {
        runBlocking {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Left(EmailTooShort))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isFalse
        }
    }

    @Test
    fun `given validateEmail is called, when the validation fails with EmailInvalid error then isValidEmail should be false`() {
        runBlocking {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Left(EmailInvalid))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isFalse
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
    }
}