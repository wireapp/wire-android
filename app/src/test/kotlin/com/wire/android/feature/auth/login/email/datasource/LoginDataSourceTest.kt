package com.wire.android.feature.auth.login.email.datasource

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.login.email.datasource.remote.LoginRemoteDataSource
import com.wire.android.feature.auth.login.email.datasource.remote.LoginWithEmailResponse
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.mapper.SessionMapper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class LoginDataSourceTest : UnitTest() {

    @MockK
    private lateinit var loginRemoteDataSource: LoginRemoteDataSource

    @MockK
    private lateinit var sessionMapper: SessionMapper

    @MockK
    private lateinit var loginWithEmailResponse: Response<LoginWithEmailResponse>

    @MockK
    private lateinit var session: Session

    private lateinit var loginDataSource: LoginDataSource

    @Before
    fun setUp() {
        loginDataSource = LoginDataSource(loginRemoteDataSource, sessionMapper)
    }

    @Test
    fun `given loginWithEmail is called, then calls remoteDataSource with given credentials`() {
        coEvery { loginRemoteDataSource.loginWithEmail(email = TEST_EMAIL,
            password = TEST_PASSWORD) } returns Either.Right(loginWithEmailResponse)

        runBlocking {
            loginDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)

            coVerify(exactly = 1) { loginRemoteDataSource.loginWithEmail(email = TEST_EMAIL, password = TEST_PASSWORD) }
        }
    }

    @Test
    fun `given loginWithEmail is called, when remoteDataSource returns a response, then maps the result and returns user session`() {
        coEvery { loginRemoteDataSource.loginWithEmail(email = TEST_EMAIL,
            password = TEST_PASSWORD) } returns Either.Right(loginWithEmailResponse)
        every { sessionMapper.fromLoginResponse(loginWithEmailResponse) } returns session

        runBlocking {
            loginDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) shouldSucceed { it shouldBe session }
        }
        verify(exactly = 1) { sessionMapper.fromLoginResponse(loginWithEmailResponse) }
    }

    @Test
    fun `given loginWithEmail is called, when remoteDataSource returns a failure, then directly returns that failure`() {
        coEvery { loginRemoteDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) } returns Either.Left(ServerError)

        runBlocking {
            val result = loginDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)

            result shouldFail { it shouldBe ServerError }
            verify(exactly = 1) { sessionMapper wasNot Called }
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "123456"
    }
}
