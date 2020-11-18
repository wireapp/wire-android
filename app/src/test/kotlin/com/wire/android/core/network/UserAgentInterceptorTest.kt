package com.wire.android.core.network

import com.wire.android.UnitTest
import com.wire.android.core.config.GlobalConfig
import com.wire.android.core.extension.EMPTY
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
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
    fun `Given HttpRequest is intercepted when chain request header is null then create new request with header`() {
        every { originalRequest.header(USER_AGENT_HEADER_KEY) } returns null
        every { requestBuilder.addHeader(USER_AGENT_HEADER_KEY, any()) } returns requestBuilder
        every { requestBuilder.build() } returns newRequest

        userAgentInterceptor.intercept(chain)

        verify(exactly = 1) { chain.proceed(newRequest) }
    }

    @Test
    fun `Given HttpRequest is intercepted when chain request header is empty then create new request with header`() {
        every { originalRequest.header(USER_AGENT_HEADER_KEY) } returns String.EMPTY
        every { requestBuilder.addHeader(USER_AGENT_HEADER_KEY, any()) } returns requestBuilder
        every { requestBuilder.build() } returns newRequest

        userAgentInterceptor.intercept(chain)

        verify(exactly = 1) { chain.proceed(newRequest) }
    }

    @Test
    fun `Given HttpRequest is intercepted when chain request header exists then proceed with normal request`() {
        every { originalRequest.header(USER_AGENT_HEADER_KEY) } returns USER_AGENT_HEADER_CONTENT

        userAgentInterceptor.intercept(chain)

        verify(exactly = 1) { chain.proceed(originalRequest) }
    }

    companion object {
        private const val USER_AGENT_HEADER_KEY = "User-Agent"
        private const val ANDROID_DETAILS = "Android 10.0"
        private const val WIRE_DETAILS = "Wire 3.12.300"
        private const val HTTP_DETAILS = "HttpLibrary 4.1.0"
        private const val USER_AGENT_HEADER_CONTENT = "$ANDROID_DETAILS / $WIRE_DETAILS / $HTTP_DETAILS"
    }
}
