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
package com.wire.ios.shared.auth.newlogin

import com.wire.ios.shared.auth.login.model.LoginServerLinks

/**
 * Pilot adapter for the export-ios login probe.
 *
 * This keeps the current POC easy to exercise from Swift while the real migration shape is
 * validated. It is not the target pattern for migrating Android ViewModels; shared ViewModels
 * should eventually depend on Kalium use cases/facades directly through Metro, with only
 * platform-specific capabilities extracted behind small gateways.
 */
interface NewLoginIdentifierBackend {
    suspend fun resolveEmail(userIdentifier: String): NewLoginIdentifierBackendResult

    suspend fun initiateSso(ssoCode: String): NewLoginIdentifierBackendResult
}

sealed interface NewLoginIdentifierBackendResult {
    data class OpenEmailPassword(
        val userIdentifier: String,
        val path: NewLoginPasswordPath,
    ) : NewLoginIdentifierBackendResult

    data class OpenCustomConfig(
        val userIdentifier: String,
        val serverLinks: LoginServerLinks,
    ) : NewLoginIdentifierBackendResult

    data class OpenSso(
        val url: String,
        val config: NewLoginSsoUrlConfig,
    ) : NewLoginIdentifierBackendResult

    data class EnterpriseLoginNotSupported(
        val userIdentifier: String,
    ) : NewLoginIdentifierBackendResult

    data class Error(
        val error: NewLoginIdentifierDialogError,
    ) : NewLoginIdentifierBackendResult
}

class LocalNewLoginIdentifierBackend : NewLoginIdentifierBackend {
    override suspend fun resolveEmail(userIdentifier: String): NewLoginIdentifierBackendResult =
        NewLoginIdentifierBackendResult.OpenEmailPassword(
            userIdentifier = userIdentifier,
            path = NewLoginPasswordPath(),
        )

    override suspend fun initiateSso(ssoCode: String): NewLoginIdentifierBackendResult =
        NewLoginIdentifierBackendResult.OpenSso(
            url = "",
            config = NewLoginSsoUrlConfig(userIdentifier = ssoCode),
        )
}
