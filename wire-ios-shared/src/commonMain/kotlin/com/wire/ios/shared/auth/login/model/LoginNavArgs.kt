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
package com.wire.ios.shared.auth.login.model

data class LoginNavArgs(
    val userIdentifier: LoginUserIdentifier = LoginUserIdentifier.None,
    val ssoLoginResult: LoginSsoResult? = null,
    val passwordPath: LoginPasswordPath? = null,
    val ssoCodeAutoLogin: LoginSsoCodeAutoLogin? = null,
)

sealed interface LoginUserIdentifier {
    val userIdentifierEditable: Boolean

    data object None : LoginUserIdentifier {
        override val userIdentifierEditable: Boolean = true
    }

    data class PreFilled(
        val value: String,
        val editable: Boolean = false,
    ) : LoginUserIdentifier {
        override val userIdentifierEditable: Boolean = editable
    }
}

data class LoginSsoCodeAutoLogin(
    val ssoCode: String,
    val autoInitiateLogin: Boolean = true,
    val nomadServiceUrl: String? = null,
    val cookieLabel: String? = null,
)

data class LoginSsoResult(
    val success: Boolean,
    val cookie: String? = null,
    val error: LoginSsoFailure? = null,
)

enum class LoginSsoFailure {
    InvalidRequest,
    InvalidCookie,
    ServerError,
    Unknown,
}
