package com.wire.android.shared.user.password

import com.wire.android.UnitTest
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class ValidatePasswordUseCaseTest : UnitTest() {

    @Mock
    private lateinit var passwordLengthConfig: PasswordLengthConfig

    private lateinit var useCase: ValidatePasswordUseCase

    @Before
    fun setUp() {
        `when`(passwordLengthConfig.minLength()).thenReturn(TEST_MIN_LENGTH)
        `when`(passwordLengthConfig.maxLength()).thenReturn(TEST_MAX_LENGTH)

        useCase = ValidatePasswordUseCase(passwordLengthConfig)
    }

    @Test
    fun `given a passwordLengthConfig, when minLength() is called, then returns config's minLength`() {
        val minLength = useCase.minLength()

        verify(passwordLengthConfig).minLength()
        assertThat(minLength).isEqualTo(TEST_MIN_LENGTH)
    }

    @Test
    fun `given a password with length shorter than config's minLength, when run is called, returns InvalidPasswordFailure`() {
        runBlocking {
            val password = "12"

            val result = useCase.run(ValidatePasswordParams(password))

            result.assertLeft {
                assertThat(it).isEqualTo(InvalidPasswordFailure)
            }
        }
    }

    @Test
    fun `given a password with length longer than config's maxLength, when run is called, returns InvalidPasswordFailure`() {
        runBlocking {
            val password = "1234567890aaaaBBBBB*****"

            val result = useCase.run(ValidatePasswordParams(password))

            result.assertLeft {
                assertThat(it).isEqualTo(InvalidPasswordFailure)
            }
        }
    }

    @Test
    fun `given a password with no lowercase letter, when run is called, returns InvalidPasswordFailure`() {
        runBlocking {
            val password = "B123CD*"

            val result = useCase.run(ValidatePasswordParams(password))

            result.assertLeft {
                assertThat(it).isEqualTo(InvalidPasswordFailure)
            }
        }
    }

    @Test
    fun `given a password with no uppercase letter, when run is called, returns InvalidPasswordFailure`() {
        runBlocking {
            val password = "1a_23b"

            val result = useCase.run(ValidatePasswordParams(password))

            result.assertLeft {
                assertThat(it).isEqualTo(InvalidPasswordFailure)
            }
        }
    }

    @Test
    fun `given a password with no digits, when run is called, returns InvalidPasswordFailure`() {
        runBlocking {
            val password = "aBc*D!"

            val result = useCase.run(ValidatePasswordParams(password))

            result.assertLeft {
                assertThat(it).isEqualTo(InvalidPasswordFailure)
            }
        }
    }

    @Test
    fun `given a password with no special character, when run is called, returns InvalidPasswordFailure`() {
        runBlocking {
            val password = "1a23BcD4"

            val result = useCase.run(ValidatePasswordParams(password))

            result.assertLeft {
                assertThat(it).isEqualTo(InvalidPasswordFailure)
            }
        }
    }

    @Test
    fun `given a password with valid length and characters, when run is called, returns success`() {
        runBlocking {
            val password = "_12abCD*Ef3"

            val result = useCase.run(ValidatePasswordParams(password))

            result.assertRight()
        }
    }

    companion object {
        private const val TEST_MIN_LENGTH = 3
        private const val TEST_MAX_LENGTH = 12
    }
}
