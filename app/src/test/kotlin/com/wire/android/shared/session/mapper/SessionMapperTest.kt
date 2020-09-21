package com.wire.android.shared.session.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.auth.login.email.datasource.remote.LoginWithEmailResponse
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.datasources.local.SessionEntity
import okhttp3.Headers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import retrofit2.Response

class SessionMapperTest : UnitTest() {

    private lateinit var sessionMapper: SessionMapper

    @Mock
    private lateinit var loginWithEmailResponse: Response<LoginWithEmailResponse>

    @Mock
    private lateinit var loginWithEmailResponseBody: LoginWithEmailResponse

    @Before
    fun setUp() {
        sessionMapper = SessionMapper()
    }

    @Test
    fun `given fromLoginResponse is called, when response body is empty, then returns empty Session`() {
        `when`(loginWithEmailResponse.body()).thenReturn(null)

        val result = sessionMapper.fromLoginResponse(loginWithEmailResponse)

        assertThat(result).isEqualTo(Session.EMPTY)
    }

    @Test
    fun `given fromLoginResponse is called, when response header does not contain refresh token key, then returns empty Session`() {
        `when`(loginWithEmailResponse.body()).thenReturn(loginWithEmailResponseBody)
        `when`(loginWithEmailResponse.headers()).thenReturn(Headers.headersOf("dummyKey", "dummyValue"))

        val result = sessionMapper.fromLoginResponse(loginWithEmailResponse)

        assertThat(result).isEqualTo(Session.EMPTY)
    }

    @Test
    fun `given fromLoginResponse is called, when refresh token is not in correct format, then returns empty Session`() {
        `when`(loginWithEmailResponse.body()).thenReturn(loginWithEmailResponseBody)
        `when`(loginWithEmailResponse.headers()).thenReturn(Headers.headersOf(LOGIN_REFRESH_TOKEN_HEADER_KEY, "dummyValue"))

        val result = sessionMapper.fromLoginResponse(loginWithEmailResponse)

        assertThat(result).isEqualTo(Session.EMPTY)
    }

    @Test
    fun `given fromLoginResponse is called, when body exists and refresh token is correct, then returns correct Session mapping`() {
        `when`(loginWithEmailResponse.body()).thenReturn(
            LoginWithEmailResponse(expiresIn = 900, accessToken = TEST_ACCESS_TOKEN, userId = TEST_USER_ID, tokenType = TEST_TOKEN_TYPE)
        )
        `when`(loginWithEmailResponse.headers()).thenReturn(
            Headers.headersOf(LOGIN_REFRESH_TOKEN_HEADER_KEY, TEST_REFRESH_TOKEN_HEADER_VALUE)
        )

        val result = sessionMapper.fromLoginResponse(loginWithEmailResponse)

        assertThat(result).isEqualTo(
            Session(userId = TEST_USER_ID, accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE, refreshToken = TEST_REFRESH_TOKEN)
        )
    }

    @Test
    fun `given toSessionEntity is called, then maps the Session and returns a SessionEntity`() {
        val session = Session(
            userId = TEST_USER_ID, accessToken = TEST_ACCESS_TOKEN,
            tokenType = TEST_TOKEN_TYPE, refreshToken = TEST_REFRESH_TOKEN
        )

        val currentSession = sessionMapper.toSessionEntity(session, true)
        assertThat(currentSession).isEqualTo(testSessionEntity(true))

        val notCurrentSession = sessionMapper.toSessionEntity(session, false)
        assertThat(notCurrentSession).isEqualTo(testSessionEntity(false))
    }

    companion object {
        private const val TEST_USER_ID = "user-id-1"
        private const val TEST_ACCESS_TOKEN = "access-token-1"
        private const val TEST_TOKEN_TYPE = "token-type-bearer"
        private const val TEST_REFRESH_TOKEN =
            "kj-BRlhCPgzEomBoWYnOAZRB8DLczUCcUrZtVuWzWTRI3p_gb5Y_nn9Y6vTC7cVcTEztsi2cNFN_aPImyTE3DA==.v=1.k=1.d=1605278429" +
                ".t=u.l=.u=1c5af167-fd5d-4ee5-a11b-fe93b1882ade.r=73e30faa"
        private const val TEST_REFRESH_TOKEN_HEADER_VALUE = "zuid=$TEST_REFRESH_TOKEN; Path=/access; HttpOnly; Secure"

        private const val LOGIN_REFRESH_TOKEN_HEADER_KEY = "Set-Cookie"

        private fun testSessionEntity(current: Boolean) = SessionEntity(
            userId = TEST_USER_ID, accessToken = TEST_ACCESS_TOKEN,
            tokenType = TEST_TOKEN_TYPE, refreshToken = TEST_REFRESH_TOKEN, isCurrent = current
        )
    }
}
