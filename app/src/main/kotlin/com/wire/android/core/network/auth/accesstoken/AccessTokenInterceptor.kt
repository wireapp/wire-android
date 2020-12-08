package com.wire.android.core.network.auth.accesstoken

import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class AccessTokenInterceptor(private val sessionRepository: SessionRepository) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        chain.proceed(interceptedRequest(chain) ?: chain.request())
    }

    private suspend fun interceptedRequest(chain: Interceptor.Chain): Request? =
        sessionRepository.accessToken().fold({ null }) {
            if (it.isNotEmpty()) addAuthHeader(chain, it)
            else null
        }

    private fun addAuthHeader(chain: Interceptor.Chain, token: String) =
        chain.request()
            .newBuilder()
            .addHeader(AUTH_HEADER_KEY, "$AUTH_HEADER_TOKEN_TYPE $token")
            .build()

    companion object {
        private const val AUTH_HEADER_KEY = "Authorization"
        private const val AUTH_HEADER_TOKEN_TYPE = "Bearer"
    }
}
