package com.wire.android.feature.auth.login.email.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.ServerError
import com.wire.android.core.exception.TooManyRequests
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.login.email.LoginRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.User
import com.wire.android.shared.user.UserRepository
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class LoginWithEmailUseCaseTest : UnitTest() {

    @MockK
    private lateinit var loginRepository: LoginRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var sessionRepository: SessionRepository

    private lateinit var session: Session

    @MockK
    private lateinit var user: User

    private lateinit var loginWithEmailUseCase: LoginWithEmailUseCase

    @Before
    fun setUp() {
        session = testSession()
        loginWithEmailUseCase = LoginWithEmailUseCase(loginRepository, userRepository, sessionRepository)
    }

    @Test
    fun `given run is called, when loginRepository returns a valid user and user & session repos return success, then returns success`() {
        coEvery { loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) } returns Either.Right(session)
        coEvery { userRepository.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE) } returns Either.Right(user)
        coEvery { sessionRepository.save(session, true) } returns Either.Right(Unit)

        val result = runBlocking { loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD)) }

        result shouldSucceed {}
        coVerify(exactly = 1) { loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) }
        coVerify(exactly = 1) { userRepository.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE) }
        coVerify(exactly = 1) { sessionRepository.save(eq(session), eq(true)) }
    }

    @Test
    fun `given run is called, when loginRepository returns Forbidden failure, then directly returns LoginAuthenticationFailure`() {
        coEvery { loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) } returns Either.Left(Forbidden)

        val result = runBlocking { loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD)) }

        result.shouldFail { it shouldBe LoginAuthenticationFailure }
        verify(exactly = 1) { userRepository wasNot Called }
    }

    @Test
    fun `given run is called, when loginRepository returns TooManyRequests failure, then directly returns LoginTooFrequentFailure`() {
        coEvery { loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) } returns Either.Left(TooManyRequests)

        val result = runBlocking { loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD)) }

        result shouldFail { it shouldBe LoginTooFrequentFailure }
        verify(exactly = 1) { userRepository wasNot Called }
    }

    @Test
    fun `given run is called, when loginRepository returns any other failure, then directly returns that failure`() {
        coEvery { loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) } returns Either.Left(ServerError)

        val result = runBlocking { loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD)) }

        result shouldFail { it shouldBe ServerError }
        verify(exactly = 1) { userRepository wasNot Called }
    }

    @Test
    fun `given run is called, when loginRepository returns empty user session, then directly returns SessionCredentialsMissing`() {
        coEvery { loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) } returns Either.Right(Session.EMPTY)

        val result = runBlocking { loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD)) }

        result shouldFail { it shouldBe SessionCredentialsMissing }
        verify(exactly = 1) { userRepository wasNot Called }
    }

    @Test
    fun `given run is called, when loginRepository returns success but userRepository fails to get self user, then returns failure`() {
        val failure = DatabaseFailure()
        coEvery { loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) } returns Either.Right(session)
        coEvery { userRepository.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE) } returns Either.Left(failure)

        val result = runBlocking { loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD)) }

        result shouldFail { it shouldBe failure }
        coVerify(exactly = 1) { userRepository.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE) }
        coVerify(inverse = true) { sessionRepository.save(eq(session), any()) }
    }

    @Test
    fun `given run is called, when login & user repos return success, but sessionRepository fails to save session, then returns failure`() {
        val failure = DatabaseFailure()
        coEvery { loginRepository.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) } returns Either.Right(session)
        coEvery { userRepository.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE) } returns Either.Right(user)
        coEvery { sessionRepository.save(session, true) } returns Either.Left(failure)

        val result = runBlocking { loginWithEmailUseCase.run(LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_PASSWORD)) }

        result shouldFail { it shouldBe failure }
        coVerify(exactly = 1) { userRepository.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE) }
        coVerify(exactly = 1) { sessionRepository.save(eq(session), eq(true)) }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "123456"
        private const val TEST_ACCESS_TOKEN = "test-access-token-123"
        private const val TEST_TOKEN_TYPE = "test-token-bearer"

        private fun testSession() = mockkClass(Session::class).also {
            every { it.accessToken } returns TEST_ACCESS_TOKEN
            every { it.tokenType } returns TEST_TOKEN_TYPE
        }
    }
}
