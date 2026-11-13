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

package com.wire.android.ui.home.settings.multifactorauthentication

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

/** List and editor share the overview's MFA-area owner, not a copy of its app list. */
@Serializable
data class MultiFactorAuthenticationAuthenticatorAppsRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String get() = "app/multi_factor_authentication_authenticator_apps"
}

@Serializable
data class MultiFactorAuthenticationEditAuthenticatorAppRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    val appId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String get() = "app/multi_factor_authentication_edit_authenticator_app"
}

@Serializable
data class AuthenticatorAppRenameResult(val appId: String, val name: String)

internal val AuthenticatorAppRenameResultContract = WireNavResultContract<AuthenticatorAppRenameResult>(
    WireNavResultContractId("settings.multi-factor-authentication-rename-app")
)
