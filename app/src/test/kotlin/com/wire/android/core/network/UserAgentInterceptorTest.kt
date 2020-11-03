package com.wire.android.core.network

import com.wire.android.UnitTest
import com.wire.android.core.config.AppVersionNameConfig
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Before
import org.junit.Test

class UserAgentInterceptorTest : UnitTest() {

    private lateinit var userAgentInterceptor: UserAgentInterceptor

    @MockK
    private lateinit var userAgentConfig: UserAgentConfig

    @MockK
    private lateinit var appVersionNameConfig: AppVersionNameConfig

    @Before
    fun setup() {
        userAgentInterceptor = UserAgentInterceptor(userAgentConfig)
        every { userAgentConfig.appVersionNameNameConfig } returns appVersionNameConfig
        every { userAgentConfig.androidVersion } returns ANDROID_VERSION
        every { userAgentConfig.httpUserAgent } returns HTTP_LIBRARY_VERSION

        every { appVersionNameConfig.versionName } returns WIRE_VERSION
    }


    @Test
    fun `Given HttpRequest header User-Agent does not exist already, then add new header to request with user-agent details`() {
        val chain = mockk<Interceptor.Chain>(relaxed = true)
        val initialRequest = mockk<Request>(relaxed = true)
        every { chain.request() } returns initialRequest

        val requestBuilder = mockk<Request.Builder>()
        every { initialRequest.newBuilder() } returns requestBuilder
        every { requestBuilder.addHeader(any(), any()) } returns requestBuilder

        val requestWithoutHeader = mockk<Request>()
        every { requestBuilder.build() } returns requestWithoutHeader
        every { chain.proceed(requestWithoutHeader) } returns mockk()

        userAgentInterceptor.intercept(chain)

        verify(exactly = 1) { requestBuilder.addHeader(USER_AGENT_HEADER_KEY, USER_AGENT) }
        verify(exactly = 1) { chain.proceed(requestWithoutHeader) }
        verify(exactly = 0) { requestBuilder.removeHeader(USER_AGENT_HEADER_KEY) }
    }

    @Test
    fun `Given current User-Agent header does exist already, then proceed with initial request`() {
        val chain = mockk<Interceptor.Chain>(relaxed = true)
        val initialRequest = mockk<Request>(relaxed = true)
        every { chain.request() } returns initialRequest

        val requestBuilder = mockk<Request.Builder>()
        every { initialRequest.newBuilder() } returns requestBuilder
        every { requestBuilder.addHeader(any(), any()) } returns requestBuilder

        val requestWithHeader = mockk<Request>()
        requestBuilder.addHeader(USER_AGENT_HEADER_KEY, USER_AGENT)
        every { requestBuilder.build() } returns requestWithHeader
        every { chain.proceed(requestWithHeader) } returns mockk()

        userAgentInterceptor.intercept(chain)

        verify(exactly = 1) { chain.proceed(requestWithHeader) }
        verify(exactly = 0) { requestBuilder.removeHeader(USER_AGENT_HEADER_KEY) }
    }

    companion object {
        private const val USER_AGENT_HEADER_KEY = "User-Agent"
        private const val ANDROID_VERSION = "10.0"
        private const val WIRE_VERSION = "3.12.300"
        private const val HTTP_LIBRARY_VERSION = "4.1.0"
        private const val ANDROID_DETAILS = "Android $ANDROID_VERSION"
        private const val WIRE_DETAILS = "Wire $WIRE_VERSION"
        private const val HTTP_DETAILS = "HttpLibrary $HTTP_LIBRARY_VERSION"
        private const val USER_AGENT = "$ANDROID_DETAILS / $WIRE_DETAILS / $HTTP_DETAILS"
    }
}
