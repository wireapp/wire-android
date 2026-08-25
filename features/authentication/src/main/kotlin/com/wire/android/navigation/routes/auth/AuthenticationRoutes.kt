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

package com.wire.android.navigation.routes.auth

import com.wire.navigation.AuthenticationRoute
import com.wire.navigation.WireNavEntryId
import kotlinx.serialization.Serializable

/**
 * Navigation 3 contracts for the authentication entry points.
 *
 * The route ids intentionally preserve the former generated `baseRoute` values so existing screen
 * analytics remain stable.
 * Arguments are expressed only as serializable value DTOs so the contracts can move to common
 * code without carrying Android, Compose Destinations, or Kalium implementation types with them.
 */
@Serializable
data class WelcomeChooserRoute(
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/welcome_chooser_screen"
    }
}

@Serializable
data class NewWelcomeEmptyStartRoute(
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/new_welcome_empty_start_screen"
    }
}

@Serializable
data class WelcomeRoute(
    val customServerConfig: AuthenticationServerLinks? = null,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
    override val flowId: String = entryId.value,
) : AuthenticationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A legacy-login flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/welcome_screen"
    }
}

@Serializable
data class LoginRoute(
    val args: AuthenticationLoginArguments = AuthenticationLoginArguments(),
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
    override val flowId: String = entryId.value,
) : AuthenticationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A legacy-login flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/login_screen"
    }
}

@Serializable
data class NewLoginRoute(
    val args: AuthenticationLoginArguments = AuthenticationLoginArguments(),
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A new-login flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/new_login_screen"

        fun start(
            args: AuthenticationLoginArguments = AuthenticationLoginArguments(),
        ): NewLoginRoute {
            val entryId = WireNavEntryId.random()
            return NewLoginRoute(args = args, flowId = entryId.value, entryId = entryId)
        }
    }
}

@Serializable
data class NewLoginPasswordRoute(
    val args: AuthenticationLoginArguments,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A new-login flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/new_login_password_screen"
    }
}

@Serializable
data class NewLoginVerificationCodeRoute(
    val args: AuthenticationLoginArguments,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A new-login flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/new_login_verification_code_screen"
    }
}

@Serializable
data class AuthenticationLoginArguments(
    val userHandle: AuthenticationPrefilledUserIdentifier? = null,
    val ssoLoginResult: AuthenticationSsoLoginResult? = null,
    val loginPasswordPath: AuthenticationLoginPasswordPath? = null,
    val ssoCodeAutoLogin: AuthenticationSsoCodeAutoLogin? = null,
    val showBackendConfigSuccess: Boolean = false,
)

@Serializable
data class AuthenticationPrefilledUserIdentifier(
    val value: String,
    val editable: Boolean = false,
)

@Serializable
sealed interface AuthenticationSsoLoginResult {
    @Serializable
    data class Success(
        val cookie: String,
        val serverConfigId: String,
    ) : AuthenticationSsoLoginResult

    @Serializable
    data class Failure(
        val code: AuthenticationSsoFailureCode,
    ) : AuthenticationSsoLoginResult
}

@Serializable
enum class AuthenticationSsoFailureCode {
    SERVER_ERROR_UNSUPPORTED_SAML,
    BAD_SUCCESS_REDIRECT,
    BAD_FAILURE_REDIRECT,
    BAD_USERNAME,
    BAD_UPSTREAM,
    SERVER_ERROR,
    NOT_FOUND,
    FORBIDDEN,
    NO_MATCHING_AUTH_REQUEST,
    INSUFFICIENT_PERMISSIONS,
    UNKNOWN,
}

@Serializable
data class AuthenticationSsoCodeAutoLogin(
    val ssoCode: String,
    val autoInitiateLogin: Boolean = true,
    val nomadServiceUrl: String? = null,
    val cookieLabel: String? = null,
)

@Serializable
data class AuthenticationLoginPasswordPath(
    val customServerConfig: AuthenticationServerLinks? = null,
    val isCloudAccountCreationPossible: Boolean? = null,
    val domainClaim: AuthenticationDomainClaim = AuthenticationDomainClaim.NotClaimed,
)

@Serializable
sealed interface AuthenticationDomainClaim {
    @Serializable
    data object NotClaimed : AuthenticationDomainClaim

    @Serializable
    data class Claimed(
        val domain: String,
    ) : AuthenticationDomainClaim {
        init {
            require(domain.isNotBlank()) { "A claimed authentication domain cannot be blank" }
        }
    }
}

@Serializable
data class AuthenticationServerLinks(
    val api: String,
    val accounts: String,
    val webSocket: String,
    val blackList: String,
    val teams: String,
    val website: String,
    val title: String,
    val isOnPremises: Boolean,
    val apiProxy: AuthenticationApiProxy?,
    val supportEmail: String? = null,
)

@Serializable
data class AuthenticationApiProxy(
    val needsAuthentication: Boolean,
    val host: String,
    val port: Int,
)
