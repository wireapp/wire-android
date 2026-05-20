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
package com.wire.shared.auth.sso

import com.wire.shared.auth.login.model.LoginServerLinks

interface LoginSsoBackend {
    suspend fun initiateLogin(ssoCode: String): LoginSsoBackendResult

    suspend fun completeLogin(
        cookie: String,
        serverConfigId: String,
    ): LoginSsoBackendResult
}

sealed interface LoginSsoBackendResult {
    data class OpenUrl(
        val url: String,
        val serverLinks: LoginServerLinks,
    ) : LoginSsoBackendResult

    data class Success(
        val initialSyncCompleted: Boolean,
        val e2eiRequired: Boolean,
    ) : LoginSsoBackendResult

    data class Error(val reason: LoginSsoError) : LoginSsoBackendResult
}
