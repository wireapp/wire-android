/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.newauthentication.login

import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.kalium.logic.feature.auth.LoginRedirectPath

sealed interface NewLoginDestination {
    data class EmailPassword(val loginPasswordPath: LoginPasswordPath) : NewLoginDestination
    data class SSO(val ssoCode: String) : NewLoginDestination
}

/**
 * Map the [LoginRedirectPath] to a [NewLoginDestination] -> EmailPassword or SSO.
 * Params are passed down accordingly that the destination needs.
 */
fun LoginRedirectPath.toPasswordOrSsoDestination(): NewLoginDestination {
    return when (this) {
        is LoginRedirectPath.CustomBackend -> NewLoginDestination.EmailPassword(
            LoginPasswordPath(
                customServerConfig = serverLinks,
                isCloudAccountCreationPossible = isCloudAccountCreationPossible
            )
        )

        is LoginRedirectPath.NoRegistration,
        is LoginRedirectPath.Default -> {
            NewLoginDestination.EmailPassword(
                LoginPasswordPath(
                    isCloudAccountCreationPossible = isCloudAccountCreationPossible,
                )
            )
        }

        is LoginRedirectPath.ExistingAccountWithClaimedDomain -> NewLoginDestination.EmailPassword(
            LoginPasswordPath(
                isCloudAccountCreationPossible = isCloudAccountCreationPossible,
                isDomainClaimedByOrg = DomainClaimedByOrg.Claimed(domain)
            )
        )

        is LoginRedirectPath.SSO -> NewLoginDestination.SSO(ssoCode)
    }
}
