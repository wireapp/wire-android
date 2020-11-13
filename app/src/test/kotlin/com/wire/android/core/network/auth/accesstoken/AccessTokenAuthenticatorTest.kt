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

    @Before
    fun setup() {
        accessTokenAuthenticator = AccessTokenAuthenticator(sessionRepository)

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
        coEvery { sessionRepository.accessToken(any()) } returns Either.Left(NoEntityFound)
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        every { response.headers[TOKEN_HEADER_KEY] } returns null
        every { currentSession.refreshToken } returns CURRENT_REFRESH_TOKEN

        accessTokenAuthenticator.authenticate(route, response)

        coVerify(exactly = 1) {
            sessionRepository.accessToken(eq(CURRENT_REFRESH_TOKEN))
        }
    }

    @Test
    fun `Given http response cookie header is valid, when cookie refresh token is different to current, then request access token with new refresh token`() {
        coEvery { sessionRepository.accessToken(any()) } returns Either.Left(NoEntityFound)
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        every { response.headers[TOKEN_HEADER_KEY] } returns NEW_REFRESH_TOKEN
        every { currentSession.refreshToken } returns CURRENT_REFRESH_TOKEN

        accessTokenAuthenticator.authenticate(route, response)

        coVerify(exactly = 1) {
            sessionRepository.accessToken(NEW_REFRESH_TOKEN)
        }
    }

    @Test
    fun `Given http response cookie header is valid, when cookie refresh token is same as current, then request access token current refresh token`() {
        coEvery { sessionRepository.accessToken(any()) } returns Either.Left(NoEntityFound)
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        every { response.headers[TOKEN_HEADER_KEY] } returns CURRENT_REFRESH_TOKEN
        every { currentSession.refreshToken } returns CURRENT_REFRESH_TOKEN

        accessTokenAuthenticator.authenticate(route, response)

        coVerify(exactly = 1) {
            sessionRepository.accessToken(CURRENT_REFRESH_TOKEN)
        }
    }

    @Test
    fun `Given http response, when access token request is successful, then save session`() {
        coEvery { sessionRepository.accessToken(any()) } returns Either.Right(currentSession)
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        coEvery { sessionRepository.save(any()) } returns Either.Right(Unit)
        every { response.headers[TOKEN_HEADER_KEY] } returns CURRENT_REFRESH_TOKEN
        every { currentSession.refreshToken } returns CURRENT_REFRESH_TOKEN

        accessTokenAuthenticator.authenticate(route, response)

        coVerify(exactly = 1) {
            sessionRepository.accessToken(any())
            sessionRepository.save(currentSession)
        }
    }

    @Test
    fun `Given http response, when access token request is successful, then build new authentication request`() {
        every { currentSession.accessToken } returns NEW_ACCESS_TOKEN
        coEvery { sessionRepository.accessToken(any()) } returns Either.Right(currentSession)
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        coEvery { sessionRepository.save(currentSession) } returns Either.Right(Unit)

        val requestBuilder = mockk<Request.Builder>(relaxed = true)
        every { originalRequest.newBuilder() } returns requestBuilder
        every { requestBuilder.removeHeader(any()) } returns requestBuilder

        accessTokenAuthenticator.authenticate(route, response)

        coVerify(exactly = 1) { sessionRepository.accessToken(any()) }
        verify(exactly = 1) { requestBuilder.removeHeader(AUTH_HEADER_KEY) }
        verify(exactly = 1) { requestBuilder.addHeader(AUTH_HEADER_KEY, "$AUTH_HEADER_TOKEN_TYPE $NEW_ACCESS_TOKEN") }
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

