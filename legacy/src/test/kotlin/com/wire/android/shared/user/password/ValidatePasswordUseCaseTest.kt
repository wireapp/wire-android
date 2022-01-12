package com.wire.android.shared.user.password

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class ValidatePasswordUseCaseTest : UnitTest() {

    @MockK
    private lateinit var passwordLengthConfig: PasswordLengthConfig

    private lateinit var useCase: ValidatePasswordUseCase

    @Before
    fun setUp() {
        every { passwordLengthConfig.minLength() } returns TEST_MIN_LENGTH
        every { passwordLengthConfig.maxLength() } returns TEST_MAX_LENGTH

        useCase = ValidatePasswordUseCase(passwordLengthConfig)
    }

    @Test
    fun `given a passwordLengthConfig, when minLength() is called, then returns config's minLength`() {
        val minLength = useCase.minLength()

        verify(exactly = 1) { passwordLengthConfig.minLength() }
        minLength shouldBe TEST_MIN_LENGTH
    }

    @Test
    fun `given a password with length shorter than config's minLength, when run is called, returns InvalidPasswordFailure`() {
        val password = "12"

        val result = runBlocking { useCase.run(ValidatePasswordParams(password)) }

        result shouldFail { it shouldBe InvalidPasswordFailure }
    }

    @Test
    fun `given a password with length longer than config's maxLength, when run is called, returns InvalidPasswordFailure`() {
        val password = "1234567890aaaaBBBBB*****"

        val result = runBlocking { useCase.run(ValidatePasswordParams(password)) }

        result shouldFail { it shouldBe InvalidPasswordFailure }
    }

    @Test
    fun `given a password with no lowercase letter, when run is called, returns InvalidPasswordFailure`() {
        val password = "B123CD*"

        val result = runBlocking { useCase.run(ValidatePasswordParams(password)) }

        result.shouldFail { it shouldBe InvalidPasswordFailure }
    }

    @Test
    fun `given a password with no uppercase letter, when run is called, returns InvalidPasswordFailure`() {
        val password = "1a_23b*"

        val result = runBlocking { useCase.run(ValidatePasswordParams(password)) }

        result.shouldFail { it shouldBe InvalidPasswordFailure }
    }

    @Test
    fun `given a password with no digits, when run is called, returns InvalidPasswordFailure`() {
        val password = "aBc*D!"

        val result = runBlocking { useCase.run(ValidatePasswordParams(password)) }

        result.shouldFail { it shouldBe InvalidPasswordFailure }
    }

    @Test
    fun `given a password with no special character, when run is called, returns InvalidPasswordFailure`() {
        val password = "1a23BcD4"

        val result = runBlocking { useCase.run(ValidatePasswordParams(password)) }

        result.shouldFail { it shouldBe InvalidPasswordFailure }
    }

    @Test
    fun `given a password with valid length and characters, when run is called, returns success`() {
        val password = "_12abCD*Ef3"

        val result = runBlocking { useCase.run(ValidatePasswordParams(password)) }

        result.shouldSucceed { it shouldBe Unit }
    }

    companion object {
        private const val TEST_MIN_LENGTH = 3
        private const val TEST_MAX_LENGTH = 12
    }
}
