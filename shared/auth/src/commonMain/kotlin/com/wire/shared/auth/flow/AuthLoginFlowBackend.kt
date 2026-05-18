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
package com.wire.shared.auth.flow

import com.wire.shared.auth.AuthLoginSuccessPayload

/**
 * Backend boundary for the shared auth flow coordinator.
 *
 * The common ViewModel owns UI flow state only. The real Kalium-backed implementation should
 * live in a platform source set and map Kalium results to these backend-agnostic results.
 */
interface AuthLoginFlowBackend {
    suspend fun resolveIdentifier(identifier: String): AuthLoginFlowIdentifierResult

    suspend fun initiateSso(ssoCode: String): AuthLoginFlowIdentifierResult

    suspend fun loginWithEmail(
        identifier: String,
        password: String,
        secondFactorCode: String?,
        usernameAllowed: Boolean,
    ): AuthLoginFlowLoginResult
}

sealed interface AuthLoginFlowIdentifierResult {
    data class EmailCredentialsRequired(val identifier: String) : AuthLoginFlowIdentifierResult
    data class OpenSso(val url: String, val userIdentifier: String) : AuthLoginFlowIdentifierResult
    data class Failure(val error: AuthLoginFlowError) : AuthLoginFlowIdentifierResult
}

sealed interface AuthLoginFlowLoginResult {
    data class Success(
        val initialSyncCompleted: Boolean,
        val isE2EIRequired: Boolean,
        val payload: AuthLoginSuccessPayload,
    ) : AuthLoginFlowLoginResult

    data class SecondFactorRequired(
        val email: String,
        val isCurrentCodeInvalid: Boolean = false,
    ) : AuthLoginFlowLoginResult

    data object RemoveDeviceNeeded : AuthLoginFlowLoginResult
    data class Failure(val error: AuthLoginFlowError) : AuthLoginFlowLoginResult
}

class LocalAuthLoginFlowBackend : AuthLoginFlowBackend {
    override suspend fun resolveIdentifier(identifier: String): AuthLoginFlowIdentifierResult =
        AuthLoginFlowIdentifierResult.EmailCredentialsRequired(identifier)

    override suspend fun initiateSso(ssoCode: String): AuthLoginFlowIdentifierResult =
        AuthLoginFlowIdentifierResult.OpenSso(
            url = "",
            userIdentifier = ssoCode,
        )

    override suspend fun loginWithEmail(
        identifier: String,
        password: String,
        secondFactorCode: String?,
        usernameAllowed: Boolean,
    ): AuthLoginFlowLoginResult =
        AuthLoginFlowLoginResult.Success(
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
                email = identifier,
                password = password,
                secondFactorCode = secondFactorCode,
                initialSyncCompleted = false,
                isE2EIRequired = false,
                clientId = null,
            ),
        )
}
