package com.wire.android.core.network.auth.accesstoken

import com.wire.android.core.functional.suspending
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Authenticator that attempts to refresh the client's access token.
 * In the event that a refresh fails and a new token can't be issued an error
 * is delivered to the caller. This authenticator blocks all requests while a token
 * refresh is being performed. In-flight requests that fail with a 401 (unauthorized)
 * are automatically retried.
 */
class AccessTokenAuthenticator(
    private val repository: SessionRepository,
    private val authenticationManager: AuthenticationManager
) : Authenticator {

    /**
     * This authenticate() method is called when server returns 401 Unauthorized.
     */
    override fun authenticate(route: Route?, response: Response): Request? = runBlocking {
        authenticatedRequest(response)
    }

    private suspend fun authenticatedRequest(response: Response): Request? = suspending {
        repository.currentSession().map {
            differentRefreshToken(response, it) ?: it.refreshToken
        }.flatMap { refreshToken ->
            repository.newAccessToken(refreshToken)
        }.flatMap { session ->
            repository.save(session, false).map { session }
        }.fold({ null }) {
            val authorizationToken = authenticationManager.authorizationToken(it)
            proceedWithNewAccessToken(response, authorizationToken)
        }
    }

    private fun proceedWithNewAccessToken(response: Response, newAccessToken: String): Request? =
        response.request.header(AUTH_HEADER_KEY)?.let {
            response.request
                .newBuilder()
                .removeHeader(AUTH_HEADER_KEY)
                .addHeader(AUTH_HEADER_KEY, newAccessToken)
                .build()
        }

    private fun differentRefreshToken(response: Response, currentSession: Session): String? {
        val refreshTokenHeader = response.headers[TOKEN_HEADER_KEY]
        return if (!refreshTokenHeader.equals(currentSession.refreshToken, false)) {
            refreshTokenHeader
        } else null
    }

    companion object {
        private const val TOKEN_HEADER_KEY = "Cookie"
        private const val AUTH_HEADER_KEY = "Authorization"
    }
}
