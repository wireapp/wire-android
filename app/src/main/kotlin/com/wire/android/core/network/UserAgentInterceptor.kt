package com.wire.android.core.network

import com.wire.android.BuildConfig
import com.wire.android.core.extension.EMPTY
import okhttp3.Interceptor

class UserAgentInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain) =
        when (requestHasUserHeader(chain)) {
            true -> chain.proceed(chain.request())
            else -> addUserAgentHeader(chain)
        }

    private fun addUserAgentHeader(chain: Interceptor.Chain) =
        chain.proceed(chain.request()
            .newBuilder()
            .addHeader(USER_AGENT_HEADER_KEY, USER_AGENT_HEADER_CONTENT)
            .build())

    private fun requestHasUserHeader(chain: Interceptor.Chain): Boolean =
        when (chain.request().header(USER_AGENT_HEADER_KEY)) {
            null, String.EMPTY -> false
            else -> true
        }

    companion object {
        private const val USER_AGENT_HEADER_KEY = "User-Agent"

        private val USER_AGENT_HEADER_CONTENT =
            "Android ${android.os.Build.VERSION.RELEASE} / " +
            "Wire ${BuildConfig.VERSION_NAME} / " +
            "HttpLibrary ${okhttp3.internal.userAgent}"
    }
}
