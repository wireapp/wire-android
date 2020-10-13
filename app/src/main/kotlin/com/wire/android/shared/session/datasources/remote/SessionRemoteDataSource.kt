package com.wire.android.shared.session.datasources.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler

class SessionRemoteDataSource(override val networkHandler: NetworkHandler, private val sessionApi: SessionApi) : ApiService() {

    suspend fun accessToken(refreshToken: String): Either<Failure, AccessTokenResponse> = request {
        sessionApi.accessToken("$REFRESH_TOKEN_HEADER_PREFIX$refreshToken")
    }

    companion object {
        private const val REFRESH_TOKEN_HEADER_PREFIX = "zuid="
    }
}
