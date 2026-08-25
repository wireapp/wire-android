/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.newauthentication.login.password

import com.wire.android.ui.authentication.login.LoginState

data class NewLoginPasswordPolicy(
    val accountCreationEnabled: Boolean,
    val proxyEnabled: Boolean,
    val cloudCreationPossible: Boolean,
)

fun showCreateAccount(policy: NewLoginPasswordPolicy): Boolean =
    policy.accountCreationEnabled && !policy.proxyEnabled && policy.cloudCreationPossible

sealed interface NewLoginPasswordTerminal<out UserT> {
    data class Success<UserT>(val syncCompleted: Boolean, val e2eiRequired: Boolean, val userId: UserT) : NewLoginPasswordTerminal<UserT>
    data class RemoveDevice<UserT>(val userId: UserT) : NewLoginPasswordTerminal<UserT>
    data object Canceled : NewLoginPasswordTerminal<Nothing>
    data object None : NewLoginPasswordTerminal<Nothing>
}

fun <FailureT, UserT, SsoFailureT> newLoginPasswordTerminal(
    state: LoginState<FailureT, UserT, SsoFailureT>,
): NewLoginPasswordTerminal<UserT> = when (state) {
    is LoginState.Success -> NewLoginPasswordTerminal.Success(state.initialSyncCompleted, state.isE2EIRequired, state.userId)
    is LoginState.Error.TooManyDevicesError -> NewLoginPasswordTerminal.RemoveDevice(state.userId)
    LoginState.Canceled -> NewLoginPasswordTerminal.Canceled
    else -> NewLoginPasswordTerminal.None
}
