package com.wire.android.core.network.auth.accesstoken

import com.wire.android.core.functional.suspending
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AccessTokenInterceptor(private val sessionRepository: SessionRepository) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        interceptRequest(chain) ?: chain.proceed(chain.request())
    }

    private suspend fun interceptRequest(chain: Interceptor.Chain): Response? = suspending {
        sessionRepository.currentSession().coFold({ null }) { currentSession ->
            sessionRepository.accessToken(currentSession.refreshToken).fold({ null }) { accessTokenSession ->
                when (accessTokenSession.accessToken.isNotEmpty()) {
                    true -> addAuthHeader(chain, accessTokenSession.accessToken)
                    false -> chain.proceed(chain.request())
                }
            }
        }
    }

    private fun addAuthHeader(chain: Interceptor.Chain, token: String) =
        chain.proceed(chain.request()
            .newBuilder()
            .addHeader(AUTH_HEADER_KEY, "$AUTH_HEADER_TOKEN_TYPE $token")
            .build())

    companion object {
        private const val AUTH_HEADER_KEY = "Authorization"
        private const val AUTH_HEADER_TOKEN_TYPE = "Bearer"
    }
}
