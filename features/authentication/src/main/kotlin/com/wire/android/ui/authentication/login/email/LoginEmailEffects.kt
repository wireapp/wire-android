/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.login.email

import com.wire.android.ui.authentication.login.LoginState

sealed interface LoginTerminalEffect<out UserT, out DomainT> {
    data class Success<UserT>(val syncCompleted: Boolean, val e2eiRequired: Boolean, val userId: UserT) : LoginTerminalEffect<UserT, Nothing>
    data class RemoveDevice<UserT>(val userId: UserT) : LoginTerminalEffect<UserT, Nothing>
    data class ShowClaimedDomain<DomainT>(val domain: DomainT) : LoginTerminalEffect<Nothing, DomainT>
    data object None : LoginTerminalEffect<Nothing, Nothing>
}

fun <FailureT, UserT, SsoFailureT, DomainT> loginTerminalEffect(
    state: LoginState<FailureT, UserT, SsoFailureT>,
    claimedDomain: DomainT?,
): LoginTerminalEffect<UserT, DomainT> = when (state) {
    is LoginState.Success -> claimedDomain?.let(LoginTerminalEffect::ShowClaimedDomain)
        ?: LoginTerminalEffect.Success(state.initialSyncCompleted, state.isE2EIRequired, state.userId)
    is LoginState.Error.TooManyDevicesError -> claimedDomain?.let(LoginTerminalEffect::ShowClaimedDomain)
        ?: LoginTerminalEffect.RemoveDevice(state.userId)
    else -> LoginTerminalEffect.None
}
