package com.wire.android.core.network

import com.wire.android.core.config.GlobalConfig
import com.wire.android.core.extension.EMPTY
import okhttp3.Interceptor

class UserAgentInterceptor(private val config: GlobalConfig) : Interceptor {

    override fun intercept(chain: Interceptor.Chain) =
        if (requestHasUserHeader(chain)) {
            chain.proceed(chain.request())
        } else addUserAgentHeader(chain)

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

    private fun buildUserAgentHeader(): String =
        config.OS_VERSION + " / " + config.APP_VERSION + " / " + config.USER_AGENT


    companion object {
        private const val USER_AGENT_HEADER_KEY = "User-Agent"
    }
}
