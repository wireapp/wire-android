package com.wire.android.feature.auth.login.email.datasource

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.login.email.datasource.remote.LoginRemoteDataSource
import com.wire.android.feature.auth.login.email.datasource.remote.LoginWithEmailResponse
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.mapper.SessionMapper
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import retrofit2.Response

class LoginDataSourceTest : UnitTest() {

    @Mock
    private lateinit var loginRemoteDataSource: LoginRemoteDataSource

    @Mock
    private lateinit var sessionMapper: SessionMapper

    @Mock
    private lateinit var loginWithEmailResponse: Response<LoginWithEmailResponse>

    @Mock
    private lateinit var session: Session

    private lateinit var loginDataSource: LoginDataSource

    @Before
    fun setUp() {
        loginDataSource = LoginDataSource(loginRemoteDataSource, sessionMapper)
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
            `when`(sessionMapper.fromLoginResponse(loginWithEmailResponse)).thenReturn(session)

            loginDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD).assertRight {
                assertThat(it).isEqualTo(session)
            }
            verify(sessionMapper).fromLoginResponse(loginWithEmailResponse)
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
            verifyNoInteractions(sessionMapper)
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "123456"
    }
}
