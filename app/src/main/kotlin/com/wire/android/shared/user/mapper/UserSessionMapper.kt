package com.wire.android.shared.user.mapper

import com.wire.android.feature.auth.login.email.datasource.remote.LoginWithEmailResponse
import com.wire.android.shared.user.UserSession
import com.wire.android.shared.user.datasources.local.SessionEntity
import okhttp3.Headers
import retrofit2.Response

class UserSessionMapper {

    @Suppress("ReturnCount")
    fun fromLoginResponse(response: Response<LoginWithEmailResponse>): UserSession {
        val body = response.body() ?: return UserSession.EMPTY
        val refreshToken = extractRefreshToken(response.headers()) ?: return UserSession.EMPTY

        return UserSession(
            userId = body.userId,
            accessToken = body.accessToken,
            tokenType = body.tokenType,
            refreshToken = refreshToken
        )
    }

    private fun extractRefreshToken(headers: Headers): String? =
        headers[LOGIN_REFRESH_TOKEN_HEADER_KEY]?.let {
            LOGIN_REFRESH_TOKEN_REGEX.matchEntire(it)?.groups?.get(1)?.value
        }

    fun toSessionEntity(userSession: UserSession, isCurrent: Boolean) = SessionEntity(
        userId = userSession.userId,
        accessToken = userSession.accessToken,
        tokenType = userSession.tokenType,
        refreshToken = userSession.refreshToken,
        isCurrent = isCurrent
    )

    companion object {
        private const val LOGIN_REFRESH_TOKEN_HEADER_KEY = "Set-Cookie"
        private val LOGIN_REFRESH_TOKEN_REGEX = ".*zuid=([^;]+).*".toRegex()
    }
}
