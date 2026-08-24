/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.login.sso

import com.wire.android.ui.authentication.login.LoginState

sealed interface LoginSSOEffect<out UserT> {
    data class Success<UserT>(val syncCompleted: Boolean, val e2eiRequired: Boolean, val userId: UserT) : LoginSSOEffect<UserT>
    data class RemoveDevice<UserT>(val userId: UserT) : LoginSSOEffect<UserT>
    data object ShowError : LoginSSOEffect<Nothing>
    data object None : LoginSSOEffect<Nothing>
}

fun <FailureT, UserT, SsoFailureT> loginSsoEffect(
    state: LoginState<FailureT, UserT, SsoFailureT>,
): LoginSSOEffect<UserT> = when (state) {
    is LoginState.Success -> LoginSSOEffect.Success(state.initialSyncCompleted, state.isE2EIRequired, state.userId)
    is LoginState.Error.TooManyDevicesError -> LoginSSOEffect.RemoveDevice(state.userId)
    is LoginState.Error.DialogError -> LoginSSOEffect.ShowError
    else -> LoginSSOEffect.None
}
