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

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.common.dialogs.CustomServerDialogState
import com.wire.kalium.logic.data.auth.login.ProxyCredentials

data class LoginState(
    val userIdentifier: TextFieldValue = TextFieldValue(""),
    val userIdentifierEnabled: Boolean = true,
    val password: TextFieldValue = TextFieldValue(""),
    val userInput: TextFieldValue = TextFieldValue(""),
    val proxyIdentifier: TextFieldValue = TextFieldValue(""),
    val proxyPassword: TextFieldValue = TextFieldValue(""),
    val ssoLoginLoading: Boolean = false,
    val emailLoginLoading: Boolean = false,
    val ssoLoginEnabled: Boolean = false,
    val emailLoginEnabled: Boolean = false,
    val isProxyAuthRequired: Boolean = false,
    val loginError: LoginError = LoginError.None,
    val isProxyEnabled: Boolean = false,
    val customServerDialogState: CustomServerDialogState? = null,
) {
    fun getProxyCredentials(): ProxyCredentials? =
        if (proxyIdentifier.text.isNotBlank() && proxyPassword.text.isNotBlank()) {
            ProxyCredentials(proxyIdentifier.text, proxyPassword.text)
        } else {
            null
        }
}

fun LoginState.updateEmailLoginEnabled() =
    copy(
        emailLoginEnabled = userIdentifier.text.isNotEmpty() && password.text.isNotEmpty() && !emailLoginLoading &&
                (!isProxyAuthRequired || (isProxyAuthRequired && proxyIdentifier.text.isNotEmpty() && proxyPassword.text.isNotEmpty()))
    )

fun LoginState.updateSSOLoginEnabled() =
    copy(ssoLoginEnabled = userInput.text.isNotEmpty() && !ssoLoginLoading)
