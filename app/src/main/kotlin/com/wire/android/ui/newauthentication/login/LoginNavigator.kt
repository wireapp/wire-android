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
package com.wire.android.ui.newauthentication.login

import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.sso.SSOUrlConfig
import com.wire.kalium.logic.configuration.server.ServerConfig

fun interface LoginNavigator {
    fun navigate(command: LoginNavigationCommand)
}

sealed interface LoginNavigationCommand {
    data class EnterpriseLoginNotSupported(val userIdentifier: String) : LoginNavigationCommand
    data class EmailPassword(val userIdentifier: String, val loginPasswordPath: LoginPasswordPath) : LoginNavigationCommand
    data class CustomConfig(val userIdentifier: String, val customServerConfig: ServerConfig.Links) : LoginNavigationCommand
    data class SSO(val url: String, val config: SSOUrlConfig) : LoginNavigationCommand
    data class Success(val nextStep: NextStep) : LoginNavigationCommand {
        enum class NextStep { E2EIEnrollment, InitialSync, TooManyDevices, None }
    }
}

fun NewLoginAction.toLoginNavigationCommand(): LoginNavigationCommand = when (this) {
    is NewLoginAction.EnterpriseLoginNotSupported -> LoginNavigationCommand.EnterpriseLoginNotSupported(userIdentifier)
    is NewLoginAction.EmailPassword -> LoginNavigationCommand.EmailPassword(userIdentifier, loginPasswordPath)
    is NewLoginAction.CustomConfig -> LoginNavigationCommand.CustomConfig(userIdentifier, customServerConfig)
    is NewLoginAction.SSO -> LoginNavigationCommand.SSO(url, config)
    is NewLoginAction.Success -> LoginNavigationCommand.Success(nextStep.toLoginNavigationCommandNextStep())
}

private fun NewLoginAction.Success.NextStep.toLoginNavigationCommandNextStep(): LoginNavigationCommand.Success.NextStep = when (this) {
    NewLoginAction.Success.NextStep.E2EIEnrollment -> LoginNavigationCommand.Success.NextStep.E2EIEnrollment
    NewLoginAction.Success.NextStep.InitialSync -> LoginNavigationCommand.Success.NextStep.InitialSync
    NewLoginAction.Success.NextStep.TooManyDevices -> LoginNavigationCommand.Success.NextStep.TooManyDevices
    NewLoginAction.Success.NextStep.None -> LoginNavigationCommand.Success.NextStep.None
}
