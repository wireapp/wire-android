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
package com.wire.ios.shared.auth.email

interface LoginEmailBackend {
    suspend fun login(
        userIdentifier: String,
        password: String,
        secondFactorVerificationCode: String?,
        usernameAllowed: Boolean,
    ): LoginEmailBackendResult

    suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailBackendResult
}

sealed interface LoginEmailBackendResult {
    data class Success(
        val initialSyncCompleted: Boolean,
        val isE2EIRequired: Boolean,
    ) : LoginEmailBackendResult

    data class SecondFactorRequired(
        val email: String,
        val isCurrentCodeInvalid: Boolean = false,
    ) : LoginEmailBackendResult

    data object RemoveDeviceNeeded : LoginEmailBackendResult
    data class Failure(val error: LoginEmailError) : LoginEmailBackendResult
}

class LocalLoginEmailBackend : LoginEmailBackend {
    override suspend fun login(
        userIdentifier: String,
        password: String,
        secondFactorVerificationCode: String?,
        usernameAllowed: Boolean,
    ): LoginEmailBackendResult =
        LoginEmailBackendResult.Success(
            initialSyncCompleted = false,
            isE2EIRequired = false,
        )

    override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailBackendResult =
        LoginEmailBackendResult.SecondFactorRequired(email = userIdentifier)
}
