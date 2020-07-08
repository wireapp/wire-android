package com.wire.android.shared.user.email

import com.wire.android.UnitTest
import com.wire.android.core.functional.Either
import kotlinx.coroutines.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class ValidateEmailUseCaseTest : UnitTest() {

    @Mock
    private lateinit var validateEmailParams: ValidateEmailParams

    private lateinit var validationEmailUseCase: ValidateEmailUseCase

    @Before
    fun setup() {
        validationEmailUseCase = ValidateEmailUseCase()
    }

    @Test
    fun `Given run is executed, when email doesn't match regex, then return EmailInvalid failure`() {
        `when`(validateEmailParams.email).thenReturn("email")

        runBlocking {
            assertThat(validationEmailUseCase.run(validateEmailParams)).isEqualTo(Either.Left(EmailInvalid))
        }
    }

    @Test
    fun `Given run is executed, when email length is smaller than 5, then return EmailTooShort failure`() {
        val email = "t"
        `when`(validateEmailParams.email).thenReturn(email)

        runBlocking {
            assertThat(validationEmailUseCase.run(validateEmailParams)).isEqualTo(Either.Left(EmailTooShort))
        }
    }

    @Test
    fun `Given run is executed, when email matches regex and email fits requirements then return success`() {
        `when`(validateEmailParams.email).thenReturn(VALID_TEST_EMAIL)

        runBlocking {
            assertThat(validationEmailUseCase.run(validateEmailParams)).isEqualTo(Either.Right(Unit))
        }
    }

    companion object {
        private const val VALID_TEST_EMAIL = "test@wire.com"
    }
}
