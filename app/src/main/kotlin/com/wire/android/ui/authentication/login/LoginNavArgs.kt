/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.authentication.login

import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.serialization.Serializable

@Serializable
data class LoginNavArgs(
    val userHandle: PreFilledUserIdentifierType.PreFilled? = null,
    val ssoLoginResult: DeepLinkResult.SSOLogin? = null,
    val loginPasswordPath: LoginPasswordPath? = null,
    val ssoCode: String? = null,
)

@Serializable
sealed interface PreFilledUserIdentifierType {

    @Serializable
    data object None : PreFilledUserIdentifierType

    @Serializable
    data class PreFilled(val userIdentifier: String, val editable: Boolean = false) : PreFilledUserIdentifierType

    val userIdentifierEditable: Boolean get() = when (this) {
        is PreFilled -> this.editable
        is None -> true
    }
}

@Serializable
data class LoginPasswordPath(
    val customServerConfig: ServerConfig.Links? = null,
    val isCloudAccountCreationPossible: Boolean? = null,
    val isDomainClaimedByOrg: DomainClaimedByOrg = DomainClaimedByOrg.NotClaimed,
)

@Serializable
sealed interface DomainClaimedByOrg {

    @Serializable
    data object NotClaimed : DomainClaimedByOrg

    @Serializable
    data class Claimed(val domain: String) : DomainClaimedByOrg
}
