package com.wire.android.core.network.auth.accesstoken

import com.wire.android.core.extension.EMPTY
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AccessTokenInterceptor(private val repository: SessionRepository) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = synchronized(this) {
        runBlocking {
            repository.currentSession().coFold({ null }
            ) { currentSession ->
                repository.accessToken(currentSession.refreshToken).fold({ null }) { accessTokenSession ->
                    when (accessTokenSession.accessToken != String.EMPTY) {
                        true -> addAuthHeader(chain, accessTokenSession.accessToken)
                        false -> chain.proceed(chain.request())
                    }
                }
            } ?: chain.proceed(chain.request())
        }
    }

    private fun addAuthHeader(chain: Interceptor.Chain, token: String): Response {
        val authenticatedRequest = chain.request()
            .newBuilder()
            .addHeader(AUTH_HEADER_KEY, "$AUTH_HEADER_TOKEN_TYPE $token")
            .build()
        return chain.proceed(authenticatedRequest)
    }

    companion object {
        private const val AUTH_HEADER_KEY = "Authorization"
        private const val AUTH_HEADER_TOKEN_TYPE = "Bearer"
    }

}
