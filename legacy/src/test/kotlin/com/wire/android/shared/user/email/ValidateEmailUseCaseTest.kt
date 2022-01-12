package com.wire.android.shared.user.email

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ValidateEmailUseCaseTest : UnitTest() {

    @MockK
    private lateinit var validateEmailParams: ValidateEmailParams

    private lateinit var validationEmailUseCase: ValidateEmailUseCase

    @Before
    fun setup() {
        validationEmailUseCase = ValidateEmailUseCase()
    }

    @Test
    fun `Given run is executed, when email doesn't match regex, then return EmailInvalid failure`() {
        every { validateEmailParams.email } returns "email"

        val result = runBlocking { validationEmailUseCase.run(validateEmailParams) }

        result shouldFail { it shouldBeEqualTo EmailInvalid }
    }

    @Test
    fun `Given run is executed, when email length is smaller than 5, then return EmailTooShort failure`() {
        val email = "t"
        every { validateEmailParams.email } returns email

        val result = runBlocking { validationEmailUseCase.run(validateEmailParams) }

        result shouldFail { it shouldBeEqualTo EmailTooShort }
    }

    @Test
    fun `Given run is executed, when email matches regex and email fits requirements then return success`() {
        every { validateEmailParams.email } returns VALID_TEST_EMAIL

        val result = runBlocking { validationEmailUseCase.run(validateEmailParams) }

        result shouldSucceed { it shouldBe Unit }
    }

    companion object {
        private const val VALID_TEST_EMAIL = "test@wire.com"
    }
}
