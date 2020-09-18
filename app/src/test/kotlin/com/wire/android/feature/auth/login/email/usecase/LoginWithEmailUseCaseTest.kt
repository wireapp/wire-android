package com.wire.android.feature.auth.login.email.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.ServerError
import com.wire.android.core.exception.TooManyRequests
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.login.email.LoginRepository
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.user.UserRepository
import com.wire.android.shared.user.UserSession
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class LoginWithEmailUseCaseTest : UnitTest() {

    @Mock
    private lateinit var loginRepository: LoginRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userSession: UserSession

    private lateinit var loginWithEmailUseCase: LoginWithEmailUseCase

    @Before
    fun setUp() {
        `when`(userSession.userId).thenReturn(TEST_USER_ID)
        loginWithEmailUseCase = LoginWithEmailUseCase(loginRepository, userRepository)
    }

    @Test
    fun `given run is called, when loginRepository returns non-empty user and userRepository returns success, then returns success`() {
        runBlocking {
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Right(userSession))
            `when`(userRepository.saveUser(TEST_USER_ID)).thenReturn(Either.Right(Unit))
            `when`(userRepository.saveCurrentSession(userSession)).thenReturn(Either.Right(Unit))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            verify(loginRepository).loginWithEmail(TEST_EMAIL, TEST_PASSWORD)
            verify(userRepository).saveUser(TEST_USER_ID)
            verify(userRepository).saveCurrentSession(userSession)
            result.assertRight()
        }
    }

    @Test
    fun `given run is called, when loginRepository returns Forbidden failure, then directly returns LoginAuthenticationFailure`() {
        runBlocking {
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Left(Forbidden))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            result.assertLeft {
                assertThat(it).isEqualTo(LoginAuthenticationFailure)
            }
            verifyNoInteractions(userRepository)
        }
    }

    @Test
    fun `given run is called, when loginRepository returns TooManyRequests failure, then directly returns LoginTooFrequentFailure`() {
        runBlocking {
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Left(TooManyRequests))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            result.assertLeft {
                assertThat(it).isEqualTo(LoginTooFrequentFailure)
            }
            verifyNoInteractions(userRepository)
        }
    }

    @Test
    fun `given run is called, when loginRepository returns any other failure, then directly returns that failure`() {
        runBlocking {
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Left(ServerError))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            result.assertLeft {
                assertThat(it).isEqualTo(ServerError)
            }
            verifyNoInteractions(userRepository)
        }
    }

    @Test
    fun `given run is called, when loginRepository returns empty user session, then directly returns SessionCredentialsMissing`() {
        runBlocking {
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Right(UserSession.EMPTY))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            result.assertLeft {
                assertThat(it).isEqualTo(SessionCredentialsMissing)
            }
            verifyNoInteractions(userRepository)
        }
    }

    @Test
    fun `given run is called, when loginRepository returns success but userRepository fails to save user, then returns failure`() {
        runBlocking {
            val failure = DatabaseFailure()
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Right(userSession))
            `when`(userRepository.saveUser(TEST_USER_ID)).thenReturn(Either.Left(failure))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            result.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(userRepository).saveUser(TEST_USER_ID)
            verify(userRepository, never()).saveCurrentSession(userSession)
        }
    }

    @Test
    fun `given run is called, when loginRepository returns success, but userRepository fails to save session, then returns failure`() {
        runBlocking {
            val failure = DatabaseFailure()
            `when`(loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Right(userSession))
            `when`(userRepository.saveUser(TEST_USER_ID)).thenReturn(Either.Right(Unit))
            `when`(userRepository.saveCurrentSession(userSession)).thenReturn(Either.Left(failure))

            val result = loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD))

            result.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(userRepository).saveUser(TEST_USER_ID)
            verify(userRepository).saveCurrentSession(userSession)
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "123456"
        private const val TEST_USER_ID = "ruoe123-3243lk"
    }
}
