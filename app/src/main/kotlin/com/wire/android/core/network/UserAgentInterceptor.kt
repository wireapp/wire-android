package com.wire.android.core.network

import com.wire.android.core.extension.EMPTY
import com.wire.android.core.network.NetworkConfig.Companion.USER_AGENT_HEADER_KEY
import okhttp3.Interceptor

class UserAgentInterceptor(private val config: NetworkConfig) : Interceptor {

    override fun intercept(chain: Interceptor.Chain) =
        if (requestHasUserHeader(chain)) {
            chain.proceed(chain.request())
        } else addUserAgentHeader(chain)

    private fun addUserAgentHeader(chain: Interceptor.Chain) =
        chain.proceed(chain.request()
            .newBuilder()
            .addHeader(USER_AGENT_HEADER_KEY, buildUserAgentHeader())
            .build())

    private fun requestHasUserHeader(chain: Interceptor.Chain): Boolean =
        when (chain.request().header(USER_AGENT_HEADER_KEY)) {
            null, String.EMPTY -> false
            else -> true
        }

    private fun buildUserAgentHeader(): String =
        config.osVersion + " / " + config.appVersion + " / " + config.userAgent
}
