package com.wire.android.core.network.auth.accesstoken

import com.wire.android.UnitTest
import com.wire.android.core.exception.NoEntityFound
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class AccessTokenAuthenticatorTest : UnitTest() {

    private lateinit var accessTokenAuthenticator: AccessTokenAuthenticator

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var response: Response

    @MockK
    private lateinit var route: Route

    @MockK
    private lateinit var currentSession: Session

    @MockK
    private lateinit var originalRequest: Request

    @MockK
    private lateinit var authenticationManager: AuthenticationManager

    @Before
    fun setup() {
        accessTokenAuthenticator = AccessTokenAuthenticator(sessionRepository, authenticationManager)

        every { response.request } returns originalRequest
        every { originalRequest.header(AUTH_HEADER_KEY) } returns String.EMPTY
    }

    @Test
    fun `Given http response, when current session fails, then do not authenticate request`() {
        coEvery { sessionRepository.currentSession() } returns Either.Left(NoEntityFound)

        val request = accessTokenAuthenticator.authenticate(route, response)

        request shouldBe null
    }

    @Test
    fun `Given http response, when response cookie header is null, then request access token with the same refresh token as before`() {
        coEvery { sessionRepository.newAccessToken(any()) } returns Either.Left(NoEntityFound)
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        every { response.headers[TOKEN_HEADER_KEY] } returns null
        every { currentSession.refreshToken } returns CURRENT_REFRESH_TOKEN

        accessTokenAuthenticator.authenticate(route, response)

        coVerify(exactly = 1) {
            sessionRepository.newAccessToken(eq(CURRENT_REFRESH_TOKEN))
        }
    }

    @Test
    fun `Given cookie header is valid, when refresh token is not current, then build request with new refresh token`() {
        coEvery { sessionRepository.newAccessToken(any()) } returns Either.Left(NoEntityFound)
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        every { response.headers[TOKEN_HEADER_KEY] } returns NEW_REFRESH_TOKEN
        every { currentSession.refreshToken } returns CURRENT_REFRESH_TOKEN

        accessTokenAuthenticator.authenticate(route, response)

        coVerify(exactly = 1) {
            sessionRepository.newAccessToken(NEW_REFRESH_TOKEN)
        }
    }

    @Test
    fun `Given cookie header is valid, when refresh token is same as now, then build request with current refresh token`() {
        coEvery { sessionRepository.newAccessToken(any()) } returns Either.Left(NoEntityFound)
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        every { response.headers[TOKEN_HEADER_KEY] } returns CURRENT_REFRESH_TOKEN
        every { currentSession.refreshToken } returns CURRENT_REFRESH_TOKEN

        accessTokenAuthenticator.authenticate(route, response)

        coVerify(exactly = 1) {
            sessionRepository.newAccessToken(CURRENT_REFRESH_TOKEN)
        }
    }

    @Test
    fun `Given http response, when access token request is successful, then save session`() {
        coEvery { sessionRepository.newAccessToken(any()) } returns Either.Right(currentSession)
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        coEvery { sessionRepository.save(any(), false) } returns Either.Right(Unit)
        every { response.headers[TOKEN_HEADER_KEY] } returns CURRENT_REFRESH_TOKEN
        every { currentSession.refreshToken } returns CURRENT_REFRESH_TOKEN

        accessTokenAuthenticator.authenticate(route, response)

        coVerify(exactly = 1) {
            sessionRepository.newAccessToken(any())
            sessionRepository.save(currentSession, false)
        }
    }

    @Test
    fun `Given http response, when access token request is successful, then build new authentication request`() {
        val authorizationToken = "$AUTH_HEADER_TOKEN_TYPE $NEW_ACCESS_TOKEN"
        every { currentSession.accessToken } returns NEW_ACCESS_TOKEN
        every { authenticationManager.authorizationToken(currentSession) } returns authorizationToken
        coEvery { sessionRepository.newAccessToken(any()) } returns Either.Right(currentSession)
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        coEvery { sessionRepository.save(currentSession, false) } returns Either.Right(Unit)

        val requestBuilder = mockk<Request.Builder>(relaxed = true)
        every { originalRequest.newBuilder() } returns requestBuilder
        every { requestBuilder.removeHeader(any()) } returns requestBuilder

        accessTokenAuthenticator.authenticate(route, response)

        coVerify(exactly = 1) { sessionRepository.newAccessToken(any()) }
        verify(exactly = 1) { requestBuilder.removeHeader(AUTH_HEADER_KEY) }
        verify(exactly = 1) { requestBuilder.addHeader(AUTH_HEADER_KEY, authorizationToken) }
        verify(exactly = 1) { authenticationManager.authorizationToken(currentSession) }
    }

    companion object {
        private const val TOKEN_HEADER_KEY = "Cookie"
        private const val AUTH_HEADER_KEY = "Authorization"
        private const val AUTH_HEADER_TOKEN_TYPE = "Bearer"
        private const val CURRENT_REFRESH_TOKEN = "=d478784jnfmkwjehry-"
        private const val NEW_REFRESH_TOKEN = "=d4785dj347sl;pwp=="
        private const val NEW_ACCESS_TOKEN = "=d478fjfhjf84884ods5dj347sl;pwp=="

    }
}

