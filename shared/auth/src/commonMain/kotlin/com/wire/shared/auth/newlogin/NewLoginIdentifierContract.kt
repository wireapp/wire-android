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
package com.wire.shared.auth.newlogin

import com.wire.shared.auth.login.model.LoginServerLinks

/**
 * Platform-neutral state for the first new-login screen where the user enters an email or SSO code.
 */
data class NewLoginIdentifierState(
    val userIdentifier: String = "",
    val isThereActiveSession: Boolean = false,
    val userIdentifierEnabled: Boolean = true,
    val nextEnabled: Boolean = false,
    val flowState: NewLoginIdentifierFlowState = NewLoginIdentifierFlowState.Default,
) {
    val isLoading: Boolean
        get() = flowState is NewLoginIdentifierFlowState.Loading

    val isCustomConfigDialogVisible: Boolean
        get() = flowState is NewLoginIdentifierFlowState.CustomConfigDialog

    val customConfigServerLinks: LoginServerLinks?
        get() = (flowState as? NewLoginIdentifierFlowState.CustomConfigDialog)?.serverLinks

    val hasTextFieldError: Boolean
        get() = flowState is NewLoginIdentifierFlowState.TextFieldError

    val textFieldError: NewLoginIdentifierTextFieldError?
        get() = (flowState as? NewLoginIdentifierFlowState.TextFieldError)?.error

    val hasDialogError: Boolean
        get() = flowState is NewLoginIdentifierFlowState.DialogError

    val dialogError: NewLoginIdentifierDialogError?
        get() = (flowState as? NewLoginIdentifierFlowState.DialogError)?.error
}

sealed interface NewLoginIdentifierFlowState {
    data object Default : NewLoginIdentifierFlowState
    data object Loading : NewLoginIdentifierFlowState
    data class CustomConfigDialog(val serverLinks: LoginServerLinks) : NewLoginIdentifierFlowState
    data class TextFieldError(val error: NewLoginIdentifierTextFieldError) : NewLoginIdentifierFlowState
    data class DialogError(val error: NewLoginIdentifierDialogError) : NewLoginIdentifierFlowState
}

enum class NewLoginIdentifierTextFieldError {
    InvalidValue,
}

sealed interface NewLoginIdentifierDialogError {
    data object ServerVersionNotSupported : NewLoginIdentifierDialogError
    data object ClientUpdateRequired : NewLoginIdentifierDialogError
    data class SSOResultFailure(val code: NewLoginSsoFailureCode) : NewLoginIdentifierDialogError
    data object InvalidSSOCode : NewLoginIdentifierDialogError
    data object InvalidSSOCookie : NewLoginIdentifierDialogError
    data object UserAlreadyExists : NewLoginIdentifierDialogError
    data class GenericError(val message: String? = null) : NewLoginIdentifierDialogError
}

enum class NewLoginSsoFailureCode {
    Unknown,
    InvalidCode,
    InvalidCookie,
    Cancelled,
}

sealed interface NewLoginIdentifierIntent {
    data class UserIdentifierChanged(val userIdentifier: String) : NewLoginIdentifierIntent
    data object Submit : NewLoginIdentifierIntent
    data object DismissDialog : NewLoginIdentifierIntent
    data class ConfirmCustomServer(val serverLinks: LoginServerLinks) : NewLoginIdentifierIntent
    data class SSOResultReceived(val result: NewLoginSsoResult) : NewLoginIdentifierIntent
}

sealed interface NewLoginIdentifierEffect {
    data class EnterpriseLoginNotSupported(val userIdentifier: String) : NewLoginIdentifierEffect
    data class OpenEmailPassword(val userIdentifier: String, val path: NewLoginPasswordPath) : NewLoginIdentifierEffect
    data class OpenCustomConfig(val userIdentifier: String, val serverLinks: LoginServerLinks) : NewLoginIdentifierEffect
    data class OpenSSO(val url: String, val config: NewLoginSsoUrlConfig) : NewLoginIdentifierEffect
    data class LoginSucceeded(val nextStep: NewLoginSuccessNextStep) : NewLoginIdentifierEffect
}

enum class NewLoginSuccessNextStep {
    E2EIEnrollment,
    InitialSync,
    TooManyDevices,
    None,
}

data class NewLoginPasswordPath(
    val customServerConfig: LoginServerLinks? = null,
    val isCloudAccountCreationPossible: Boolean = true,
    val domainClaimedByOrg: NewLoginDomainClaimedByOrg = NewLoginDomainClaimedByOrg.NotClaimed,
)

sealed interface NewLoginDomainClaimedByOrg {
    data object NotClaimed : NewLoginDomainClaimedByOrg
    data class Claimed(val domain: String) : NewLoginDomainClaimedByOrg
}

data class NewLoginSsoUrlConfig(
    val userIdentifier: String,
)

sealed interface NewLoginSsoResult {
    data class Success(
        val cookie: String,
        val serverConfigId: String,
    ) : NewLoginSsoResult

    data class Failure(val code: NewLoginSsoFailureCode) : NewLoginSsoResult
}
