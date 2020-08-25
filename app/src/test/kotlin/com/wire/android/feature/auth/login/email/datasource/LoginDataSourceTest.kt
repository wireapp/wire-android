package com.wire.android.feature.auth.login.email.datasource

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.login.email.datasource.remote.LoginRemoteDataSource
import com.wire.android.feature.auth.login.email.datasource.remote.LoginWithEmailResponse
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class LoginDataSourceTest : UnitTest() {

    @Mock
    private lateinit var loginRemoteDataSource: LoginRemoteDataSource

    private lateinit var loginDataSource: LoginDataSource

    @Before
    fun setUp() {
        loginDataSource = LoginDataSource(loginRemoteDataSource)
    }

    @Test
    fun `given email and password, when loginWithEmail is called, then calls remoteDataSource with given credentials`() {
        runBlocking {
            `when`(loginRemoteDataSource.loginWithEmail(email = TEST_EMAIL, password = TEST_PASSWORD))
                .thenReturn(Either.Right(LOGIN_WITH_EMAIL_RESPONSE))

            loginDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)

            verify(loginRemoteDataSource).loginWithEmail(email = TEST_EMAIL, password = TEST_PASSWORD)
        }
    }

    @Test
    fun `given remoteDataSource returns success, when loginWithEmail is called, then returns success`() {
        runBlocking {
            `when`(loginRemoteDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Right(LOGIN_WITH_EMAIL_RESPONSE))

            val result = loginDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)

            result.assertRight()
        }
    }

    @Test
    fun `given remoteDataSource returns a failure, when loginWithEmail is called, then returns that failure`() {
        runBlocking {
            `when`(loginRemoteDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Left(ServerError))

            val result = loginDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)

            result.assertLeft {
                assertThat(it).isEqualTo(ServerError)
            }
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "123456"
        private val LOGIN_WITH_EMAIL_RESPONSE = LoginWithEmailResponse(
            expiresIn = 900,
            accessToken = "AccessToken",
            userId = "123",
            tokenType = "Bearer"
        )
    }
}
