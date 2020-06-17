package com.wire.android.shared.user.email

import com.wire.android.UnitTest
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import java.util.regex.Matcher
import java.util.regex.Pattern

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
    fun `Given run is executed, when email doesn't match regex, then return failure`() {
        `when`(validateEmailParams.email).thenReturn("email")

        runBlockingTest {
            assertThat(validationEmailUseCase.run(validateEmailParams).isLeft).isTrue()
        }
    }

    @Test
    fun `Given run is executed, when email length is smaller than 5, then return failure`() {
        val email = "t"
        `when`(validateEmailParams.email).thenReturn(email)

        runBlockingTest {
            assertThat(validationEmailUseCase.run(validateEmailParams).isLeft).isTrue()
        }
    }

    @Test
    fun `Given run is executed, when email matches regex and email fits requirements then return success`() {
        `when`(validateEmailParams.email).thenReturn(VALID_TEST_EMAIL)

        runBlockingTest {
            assertThat(validationEmailUseCase.run(validateEmailParams).isRight).isTrue()
        }
    }

    companion object {
        private const val VALID_TEST_EMAIL = "test@wire.com"
    }
}
