package com.wire.android.core.network

import com.wire.android.core.config.AppVersionNameConfig
import com.wire.android.core.extension.EMPTY
import okhttp3.Interceptor

class UserAgentInterceptor(userAgentConfig: UserAgentConfig) : Interceptor {

    private val newUserAgentHeader =
        "Android ${userAgentConfig.androidVersion} / " +
        "Wire ${userAgentConfig.appVersionNameNameConfig.versionName} / " +
        "HttpLibrary ${userAgentConfig.httpUserAgent}"

    override fun intercept(chain: Interceptor.Chain) =
        when (requestHasUserHeader(chain)) {
            true -> chain.proceed(chain.request())
            else -> addUserAgentHeader(chain)
        }

    private fun addUserAgentHeader(chain: Interceptor.Chain) =
        chain.proceed(chain.request()
            .newBuilder()
            .addHeader(USER_AGENT_HEADER_KEY, newUserAgentHeader)
            .build())

    private fun requestHasUserHeader(chain: Interceptor.Chain): Boolean =
        when (chain.request().header(USER_AGENT_HEADER_KEY)) {
            null, String.EMPTY -> false
            else -> true
        }

    companion object {
        private const val USER_AGENT_HEADER_KEY = "User-Agent"
    }
}

data class UserAgentConfig(
    val appVersionNameNameConfig: AppVersionNameConfig,
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    val httpUserAgent: String = okhttp3.internal.userAgent
)
