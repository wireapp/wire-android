package com.wire.android.feature.auth.login.email.datasource

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.login.email.datasource.remote.LoginRemoteDataSource
import com.wire.android.feature.auth.login.email.datasource.remote.LoginWithEmailResponse
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.user.UserSession
import com.wire.android.shared.user.mapper.UserSessionMapper
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import retrofit2.Response

class LoginDataSourceTest : UnitTest() {

    @Mock
    private lateinit var loginRemoteDataSource: LoginRemoteDataSource

    @Mock
    private lateinit var userSessionMapper: UserSessionMapper

    @Mock
    private lateinit var loginWithEmailResponse : Response<LoginWithEmailResponse>

    @Mock
    private lateinit var userSession: UserSession

    private lateinit var loginDataSource: LoginDataSource

    @Before
    fun setUp() {
        loginDataSource = LoginDataSource(loginRemoteDataSource, userSessionMapper)
    }

    @Test
    fun `given loginWithEmail is called, then calls remoteDataSource with given credentials`() {
        runBlocking {
            `when`(loginRemoteDataSource.loginWithEmail(email = TEST_EMAIL, password = TEST_PASSWORD))
                .thenReturn(Either.Right(loginWithEmailResponse))

            loginDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)

            verify(loginRemoteDataSource).loginWithEmail(email = TEST_EMAIL, password = TEST_PASSWORD)
        }
    }

    @Test
    fun `given loginWithEmail is called, when remoteDataSource returns a response, then maps the result and returns user session`() {
        runBlocking {
            `when`(loginRemoteDataSource.loginWithEmail(email = TEST_EMAIL, password = TEST_PASSWORD))
                .thenReturn(Either.Right(loginWithEmailResponse))
            `when`(userSessionMapper.fromLoginResponse(loginWithEmailResponse)).thenReturn(userSession)

            loginDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD).assertRight {
                assertThat(it).isEqualTo(userSession)
            }
            verify(userSessionMapper).fromLoginResponse(loginWithEmailResponse)
        }
    }

    @Test
    fun `given loginWithEmail is called, when remoteDataSource returns a failure, then directly returns that failure`() {
        runBlocking {
            `when`(loginRemoteDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)).thenReturn(Either.Left(ServerError))

            val result = loginDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)

            result.assertLeft {
                assertThat(it).isEqualTo(ServerError)
            }
            verifyNoInteractions(userSessionMapper)
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "123456"
    }
}
