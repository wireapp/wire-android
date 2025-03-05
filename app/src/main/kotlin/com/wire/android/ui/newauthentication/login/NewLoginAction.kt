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

import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.sso.SSOUrlConfig
import com.wire.kalium.logic.configuration.server.ServerConfig

sealed interface NewLoginAction {
    data object EnterpriseLoginNotSupported : NewLoginAction
    data class EmailPassword(val userIdentifier: String, val loginPasswordPath: LoginPasswordPath) : NewLoginAction
    data class CustomConfig(val userIdentifier: String, val customServerConfig: ServerConfig.Links) : NewLoginAction
    data class SSO(val url: String, val config: SSOUrlConfig) : NewLoginAction
    data class Success(val nextStep: NextStep) : NewLoginAction {
        enum class NextStep { E2EIEnrollment, InitialSync, TooManyDevices, None; }
    }
}
