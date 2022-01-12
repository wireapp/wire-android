package com.wire.android.core.network

import com.wire.android.UnitTest
import com.wire.android.core.extension.EMPTY
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Before
import org.junit.Test

class UserAgentInterceptorTest : UnitTest() {

    private lateinit var userAgentInterceptor: UserAgentInterceptor

    @MockK
    private lateinit var requestBuilder: Request.Builder

    @MockK
    private lateinit var chain: Interceptor.Chain

    @MockK
    private lateinit var originalRequest: Request

    @MockK
    private lateinit var newRequest: Request

    @Before
    fun setup() {
        userAgentInterceptor = UserAgentInterceptor()

        every { chain.request() } returns originalRequest
        every { originalRequest.newBuilder() } returns requestBuilder
    }

    @Test
    fun `Given HttpRequest is intercepted when chain request header is null, then proceed with original request`() {
        every { originalRequest.header(USER_AGENT_HEADER_KEY) } returns null

        userAgentInterceptor.intercept(chain)

        verify(exactly = 1) { chain.proceed(originalRequest) }
    }

    @Test
    fun `Given HttpRequest is intercepted when chain request header is empty, then create new request without header`() {
        every { originalRequest.header(USER_AGENT_HEADER_KEY) } returns String.EMPTY
        every { requestBuilder.removeHeader(USER_AGENT_HEADER_KEY) } returns requestBuilder
        every { requestBuilder.build() } returns newRequest

        userAgentInterceptor.intercept(chain)

        verify(exactly = 1) { chain.proceed(newRequest) }
    }

    @Test
    fun `Given HttpRequest is intercepted when chain request header has a value, then create a new request without header`() {
        every { originalRequest.header(USER_AGENT_HEADER_KEY) } returns "OkHttp"
        every { requestBuilder.removeHeader(USER_AGENT_HEADER_KEY) } returns requestBuilder
        every { requestBuilder.build() } returns newRequest

        userAgentInterceptor.intercept(chain)

        verify(exactly = 1) { chain.proceed(newRequest) }
    }

    companion object {
        private const val USER_AGENT_HEADER_KEY = "User-Agent"
    }
}
