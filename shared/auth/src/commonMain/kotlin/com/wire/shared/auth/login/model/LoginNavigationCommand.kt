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
package com.wire.shared.auth.login.model

fun interface LoginNavigator {
    fun navigate(command: LoginNavigationCommand)
}

sealed interface LoginNavigationCommand {
    data class EnterpriseLoginNotSupported(val userIdentifier: String) : LoginNavigationCommand
    data class EmailPassword(val userIdentifier: String, val passwordPath: LoginPasswordPath) : LoginNavigationCommand
    data class CustomConfig(val userIdentifier: String, val customServerLinks: LoginServerLinks) : LoginNavigationCommand
    data class Sso(val url: String, val config: LoginSsoUrlConfig) : LoginNavigationCommand
    data class Success(val nextStep: LoginSuccessNextStep) : LoginNavigationCommand
}

data class LoginSsoUrlConfig(val userIdentifier: String = "")

enum class LoginSuccessNextStep {
    E2EIEnrollment,
    InitialSync,
    TooManyDevices,
    None,
}
