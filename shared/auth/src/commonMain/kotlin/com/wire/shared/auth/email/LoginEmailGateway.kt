/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.shared.auth.email

import com.wire.shared.auth.AuthLoginSuccessPayload

/**
 * Platform boundary for email login.
 *
 * This keeps the shared ViewModel free from iOS/Android runtime setup while the implementation still
 * delegates to Kalium. Remove this boundary once the shared auth module can depend on a stable Kalium
 * login orchestration API directly and both Android and iOS create the same shared ViewModel from that graph.
 */
interface LoginEmailGateway {
    suspend fun login(
        userIdentifier: String,
        password: String,
        secondFactorVerificationCode: String?,
        usernameAllowed: Boolean,
    ): LoginEmailGatewayResult

    suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailGatewayResult
}

sealed interface LoginEmailGatewayResult {
    data class Success(
        val initialSyncCompleted: Boolean,
        val isE2EIRequired: Boolean,
        val payload: AuthLoginSuccessPayload,
    ) : LoginEmailGatewayResult

    data class SecondFactorRequired(
        val email: String,
        val isCurrentCodeInvalid: Boolean = false,
    ) : LoginEmailGatewayResult

    data object RemoveDeviceNeeded : LoginEmailGatewayResult
    data class Failure(val error: LoginEmailError) : LoginEmailGatewayResult
}

class LocalLoginEmailGateway : LoginEmailGateway {
    override suspend fun login(
        userIdentifier: String,
        password: String,
        secondFactorVerificationCode: String?,
        usernameAllowed: Boolean,
    ): LoginEmailGatewayResult =
        LoginEmailGatewayResult.Success(
            initialSyncCompleted = false,
            isE2EIRequired = false,
            payload = AuthLoginSuccessPayload(
                userIdValue = "",
                userIdDomain = null,
                accessTokenValue = "",
                accessTokenType = "",
                accessTokenExpiresInSeconds = null,
                refreshTokenValue = "",
                refreshTokenCookieDomain = null,
                email = userIdentifier,
                password = password,
                secondFactorCode = secondFactorVerificationCode,
                initialSyncCompleted = false,
                isE2EIRequired = false,
                clientId = null,
            ),
        )

    override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailGatewayResult =
        LoginEmailGatewayResult.SecondFactorRequired(email = userIdentifier)
}
