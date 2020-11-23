package com.wire.android.core.network.auth.accesstoken

import com.wire.android.UnitTest
import com.wire.android.core.exception.NoEntityFound
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class AccessTokenInterceptorTest : UnitTest() {

    private lateinit var accessTokenInterceptor: AccessTokenInterceptor

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var originalResponse: Response

    @MockK
    private lateinit var originalRequest: Request

    @MockK
    private lateinit var chain: Interceptor.Chain

    @MockK
    private lateinit var currentSession: Session

    @Before
    fun setup() {
        accessTokenInterceptor = AccessTokenInterceptor(sessionRepository)

        every { chain.request() } returns originalRequest
        every { chain.proceed(originalRequest) } returns originalResponse
    }

    @Test
    fun `Given interceptor chain, when current session fails, then do not intercept request`() {
        coEvery { sessionRepository.currentSession() } returns Either.Left(NoEntityFound)

        val interceptedResponse: Response? = accessTokenInterceptor.intercept(chain)

        interceptedResponse shouldBe originalResponse
    }

    @Test
    fun `Given interceptor chain and current session is valid, when access token request fails, then do not intercept request`() {
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        coEvery { sessionRepository.accessToken(any()) } returns Either.Left(NoEntityFound)

        val interceptedResponse: Response? = accessTokenInterceptor.intercept(chain)

        interceptedResponse shouldBe originalResponse
    }

    @Test
    fun `Given interceptor chain and access token request is valid, when access token is empty, then do not intercept request`() {
        every { currentSession.accessToken } returns String.EMPTY
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        coEvery { sessionRepository.accessToken(any()) } returns Either.Right(currentSession)

        val interceptedResponse: Response? = accessTokenInterceptor.intercept(chain)

        interceptedResponse shouldBe originalResponse
    }

    @Test
    fun `Given chain and access token request is valid, when access token is valid, then build request and add token to header`() {
        val requestBuilder = mockk<Request.Builder>(relaxed = true)
        val newRequest = mockk<Request>(relaxed = true)

        every { originalRequest.newBuilder() } returns requestBuilder
        every { requestBuilder.addHeader(eq(AUTH_HEADER_KEY), eq("$AUTH_HEADER_TOKEN_TYPE $ACCESS_TOKEN")) } returns requestBuilder
        every { requestBuilder.build() } returns newRequest

        every { currentSession.accessToken } returns ACCESS_TOKEN
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        coEvery { sessionRepository.accessToken(any()) } returns Either.Right(currentSession)

        accessTokenInterceptor.intercept(chain)

        verify(exactly = 1) { chain.proceed(newRequest) }

    }

    companion object {
        private const val ACCESS_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        private const val AUTH_HEADER_KEY = "Authorization"
        private const val AUTH_HEADER_TOKEN_TYPE = "Bearer"
    }
}
