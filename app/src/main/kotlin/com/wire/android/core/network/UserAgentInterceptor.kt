package com.wire.android.core.network

import com.wire.android.core.network.NetworkConfig.Companion.USER_AGENT_HEADER_KEY
import okhttp3.Interceptor

class UserAgentInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain) = if (requestHasUserHeader(chain))
        removeUserAgent(chain)
    else
        chain.proceed(chain.request())

    private fun removeUserAgent(chain: Interceptor.Chain) = chain.proceed(
        chain.request()
            .newBuilder()
            .removeHeader(USER_AGENT_HEADER_KEY)
            .build()
    )

    private fun requestHasUserHeader(chain: Interceptor.Chain): Boolean =
        when (chain.request().header(USER_AGENT_HEADER_KEY)) {
            null -> false
            else -> true
        }
}
