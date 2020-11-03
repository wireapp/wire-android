package com.wire.android.core.network

import com.wire.android.core.config.AppVersionNameConfig
import com.wire.android.core.extension.EMPTY
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor(
    private val userAgentConfig: UserAgentConfig
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        when (chain.request().header(USER_AGENT_HEADER_KEY)) {
            null, String.EMPTY -> determineUserAgentRequest(chain)
            else -> chain.proceed(chain.request())
        }

    private fun determineUserAgentRequest(chain: Interceptor.Chain) =
        chain.proceed(
            chain.request()
                .newBuilder()
                .addHeader(USER_AGENT_HEADER_KEY, newUserAgentHeader())
                .build()
        )

    private fun newUserAgentHeader() = "$androidVersion / $wireVersion / $httpVersion"

    private val androidVersion: String =
        "Android ${userAgentConfig.androidVersion}"

    private val wireVersion: String =
        "Wire ${userAgentConfig.appVersionNameNameConfig.versionName}"

    private val httpVersion: String =
        "HttpLibrary ${userAgentConfig.httpUserAgent}"

    companion object {
        private const val USER_AGENT_HEADER_KEY = "User-Agent"
    }
}

data class UserAgentConfig(
    val appVersionNameNameConfig: AppVersionNameConfig,
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    val httpUserAgent: String = okhttp3.internal.userAgent
)
