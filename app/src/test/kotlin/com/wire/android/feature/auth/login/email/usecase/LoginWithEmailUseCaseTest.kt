package com.wire.android.feature.auth.login.email.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.ServerError
import com.wire.android.core.exception.TooManyRequests
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.login.email.LoginRepository
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class LoginWithEmailUseCaseTest : UnitTest() {

    @Mock
    private lateinit var loginRepository: LoginRepository

    private lateinit var loginWithEmailUseCase: LoginWithEmailUseCase

    @Before
    fun setUp() {
        loginWithEmailUseCase = LoginWithEmailUseCase(loginRepository)
    }

    @Test
    fun `given run is called, when repository returns success, then returns success`() {
        runBlocking {
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Right(Unit))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            result.assertRight()
        }
    }

    @Test
    fun `given run is called, when repository returns Forbidden failure, then returns LoginAuthenticationFailure`() {
        runBlocking {
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Left(Forbidden))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            result.assertLeft {
                assertThat(it).isEqualTo(LoginAuthenticationFailure)
            }
        }
    }

    @Test
    fun `given run is called, when repository returns TooManyRequests failure, then returns LoginTooFrequentFailure`() {
        runBlocking {
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Left(TooManyRequests))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            result.assertLeft {
                assertThat(it).isEqualTo(LoginTooFrequentFailure)
            }
        }
    }

    @Test
    fun `given run is called, when repository returns any other failure, then returns that failure`() {
        runBlocking {
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Left(ServerError))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            result.assertLeft {
                assertThat(it).isEqualTo(ServerError)
            }
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "123456"
    }
}
