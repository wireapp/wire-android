package com.wire.android.shared.session.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.auth.login.email.datasource.remote.LoginWithEmailResponse
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.datasources.local.SessionEntity
import com.wire.android.shared.session.datasources.remote.AccessTokenResponse
import io.mockk.every
import io.mockk.impl.annotations.MockK
import okhttp3.Headers
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class SessionMapperTest : UnitTest() {

    private lateinit var sessionMapper: SessionMapper

    @MockK
    private lateinit var loginWithEmailResponse: Response<LoginWithEmailResponse>

    @MockK
    private lateinit var loginWithEmailResponseBody: LoginWithEmailResponse

    @Before
    fun setUp() {
        sessionMapper = SessionMapper()
    }

    @Test
    fun `given fromLoginResponse is called, when response body is empty, then returns empty Session`() {
        every { loginWithEmailResponse.body() } returns null

        val result = sessionMapper.fromLoginResponse(loginWithEmailResponse)

        result shouldBe Session.EMPTY
    }

    @Test
    fun `given fromLoginResponse is called, when response header does not contain refresh token key, then returns empty Session`() {
        every { loginWithEmailResponse.body() } returns loginWithEmailResponseBody
        every { loginWithEmailResponse.headers() } returns Headers.headersOf("dummyKey", "dummyValue")

        val result = sessionMapper.fromLoginResponse(loginWithEmailResponse)

        result shouldBe Session.EMPTY
    }

    @Test
    fun `given fromLoginResponse is called, when refresh token is not in correct format, then returns empty Session`() {
        every { loginWithEmailResponse.body() } returns loginWithEmailResponseBody
        every { loginWithEmailResponse.headers() } returns Headers.headersOf(AUTH_REFRESH_TOKEN_HEADER_KEY, "dummyValue")

        val result = sessionMapper.fromLoginResponse(loginWithEmailResponse)

        result shouldBe Session.EMPTY
    }

    @Test
    fun `given fromLoginResponse is called, when body exists and refresh token is correct, then returns correct Session mapping`() {
        every { loginWithEmailResponse.body() } returns
            LoginWithEmailResponse(expiresIn = 900, accessToken = TEST_ACCESS_TOKEN, userId = TEST_USER_ID, tokenType = TEST_TOKEN_TYPE)

        every { loginWithEmailResponse.headers() } returns
            Headers.headersOf(AUTH_REFRESH_TOKEN_HEADER_KEY, TEST_REFRESH_TOKEN_HEADER_VALUE)

        val result = sessionMapper.fromLoginResponse(loginWithEmailResponse)

        result shouldEqual
            Session(userId = TEST_USER_ID, accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE, refreshToken = TEST_REFRESH_TOKEN)
    }

    @Test
    fun `given extractRefreshToken is called, when header does not contain refresh token key, then returns null`() {
        val headers = Headers.headersOf("dummyKey", "dummyValue")

        val result = sessionMapper.extractRefreshToken(headers)

        result shouldBe null
    }

    @Test
    fun `given extractRefreshToken is called, when refresh token exists but is in incorrect format, then returns null`() {
        val headers = Headers.headersOf(AUTH_REFRESH_TOKEN_HEADER_KEY, "dummyValue")

        val result = sessionMapper.extractRefreshToken(headers)

        result shouldBe null
    }

    @Test
    fun `given extractRefreshToken is called, when refresh token exists and is in correct format, then returns refresh token`() {
        val headers = Headers.headersOf(AUTH_REFRESH_TOKEN_HEADER_KEY, TEST_REFRESH_TOKEN_HEADER_VALUE)

        val result = sessionMapper.extractRefreshToken(headers)

        result shouldEqual TEST_REFRESH_TOKEN
    }

    @Test
    fun `given toSessionEntity is called, then maps the Session and returns a SessionEntity`() {
        val session = Session(
            userId = TEST_USER_ID, accessToken = TEST_ACCESS_TOKEN,
            tokenType = TEST_TOKEN_TYPE, refreshToken = TEST_REFRESH_TOKEN
        )

        val currentSession = sessionMapper.toSessionEntity(session, true)
        currentSession shouldEqual testSessionEntity(true)

        val dormantSession = sessionMapper.toSessionEntity(session, false)
        dormantSession shouldEqual testSessionEntity(false)
    }

    @Test
    fun `given fromSessionEntity is called, when entity is the current session, then maps the SessionEntity and returns a Session`() {
        val session = sessionMapper.fromSessionEntity(testSessionEntity(current = true))

        val expectedSession = Session(
            userId = TEST_USER_ID, accessToken = TEST_ACCESS_TOKEN,
            tokenType = TEST_TOKEN_TYPE, refreshToken = TEST_REFRESH_TOKEN
        )

        session shouldEqual expectedSession
    }

    @Test
    fun `given fromSessionEntity is called, when entity is dormant, then maps the SessionEntity and returns a Session`() {
        val session = sessionMapper.fromSessionEntity(testSessionEntity(current = false))

        val expectedSession = Session(
            userId = TEST_USER_ID, accessToken = TEST_ACCESS_TOKEN,
            tokenType = TEST_TOKEN_TYPE, refreshToken = TEST_REFRESH_TOKEN
        )

        session shouldEqual expectedSession
    }

    @Test
    fun `given fromAccessTokenResponse is called with a refresh token, then maps the inputs and returns a Session`() {
        val accessTokenResponse = AccessTokenResponse(
            userId = TEST_USER_ID,
            accessToken = TEST_ACCESS_TOKEN,
            tokenType = TEST_TOKEN_TYPE,
            expiresIn = 900
        )

        val session = sessionMapper.fromAccessTokenResponse(accessTokenResponse, TEST_REFRESH_TOKEN)

        val expectedSession = Session(
            userId = TEST_USER_ID,
            accessToken = TEST_ACCESS_TOKEN,
            tokenType = TEST_TOKEN_TYPE,
            refreshToken = TEST_REFRESH_TOKEN
        )

        session shouldEqual expectedSession
    }

    companion object {
        private const val TEST_USER_ID = "user-id-1"
        private const val TEST_ACCESS_TOKEN = "access-token-1"
        private const val TEST_TOKEN_TYPE = "token-type-bearer"
        private const val TEST_REFRESH_TOKEN =
            "kj-BRlhCPgzEomBoWYnOAZRB8DLczUCcUrZtVuWzWTRI3p_gb5Y_nn9Y6vTC7cVcTEztsi2cNFN_aPImyTE3DA==.v=1.k=1.d=1605278429" +
                ".t=u.l=.u=1c5af167-fd5d-4ee5-a11b-fe93b1882ade.r=73e30faa"
        private const val TEST_REFRESH_TOKEN_HEADER_VALUE = "zuid=$TEST_REFRESH_TOKEN; Path=/access; HttpOnly; Secure"

        private const val AUTH_REFRESH_TOKEN_HEADER_KEY = "Set-Cookie"

        private fun testSessionEntity(current: Boolean) = SessionEntity(
            userId = TEST_USER_ID, accessToken = TEST_ACCESS_TOKEN,
            tokenType = TEST_TOKEN_TYPE, refreshToken = TEST_REFRESH_TOKEN, isCurrent = current
        )
    }
}
