package com.wire.android.shared.session.mapper

import com.wire.android.feature.auth.login.email.datasource.remote.LoginWithEmailResponse
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.datasources.local.SessionEntity
import com.wire.android.shared.session.datasources.remote.AccessTokenResponse
import okhttp3.Headers
import retrofit2.Response

class SessionMapper {

    @Suppress("ReturnCount")
    fun fromLoginResponse(response: Response<LoginWithEmailResponse>): Session {
        val body = response.body() ?: return Session.EMPTY
        val refreshToken = extractRefreshToken(response.headers()) ?: return Session.EMPTY

        return Session(
            userId = body.userId,
            accessToken = body.accessToken,
            tokenType = body.tokenType,
            refreshToken = refreshToken
        )
    }

    fun extractRefreshToken(headers: Headers): String? =
        headers[AUTH_REFRESH_TOKEN_HEADER_KEY]?.let {
            AUTH_REFRESH_TOKEN_REGEX.matchEntire(it)?.groups?.get(1)?.value
        }

    //TODO: what about expiresIn?
    fun fromAccessTokenResponse(response: AccessTokenResponse, refreshToken: String) = Session(
        userId = response.userId,
        accessToken = response.accessToken,
        tokenType = response.tokenType,
        refreshToken = refreshToken
    )

    fun toSessionEntity(session: Session, isCurrent: Boolean) =
        SessionEntity(
            userId = session.userId,
            accessToken = session.accessToken,
            tokenType = session.tokenType,
            refreshToken = session.refreshToken,
            isCurrent = isCurrent
        )

    companion object {
        private const val AUTH_REFRESH_TOKEN_HEADER_KEY = "Set-Cookie"
        private val AUTH_REFRESH_TOKEN_REGEX = ".*zuid=([^;]+).*".toRegex()
    }
}
