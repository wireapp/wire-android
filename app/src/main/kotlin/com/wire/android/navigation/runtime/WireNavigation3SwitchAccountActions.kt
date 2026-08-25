/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.runtime

import com.wire.android.feature.SwitchAccountActions
import com.wire.android.navigation.routes.auth.AuthenticationLoginArguments
import com.wire.android.navigation.routes.auth.AuthenticationLoginPasswordPath
import com.wire.android.navigation.routes.auth.AuthenticationServerLinks
import com.wire.android.navigation.routes.auth.NewLoginRoute
import com.wire.android.navigation.routes.auth.WelcomeRoute
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId

/**
 * Navigation 3 account-switch bridge.
 *
 * Account use cases keep emitting semantic outcomes while the production action owner decides
 * which typed route and back-stack mutation each outcome requires.
 */
internal class WireNavigation3SwitchAccountActions(
    private val onSwitchedToAnotherAccount: () -> Unit,
    private val onNoOtherAccountToSwitch: () -> Unit,
) : SwitchAccountActions {
    override fun switchedToAnotherAccount() = onSwitchedToAnotherAccount()

    override fun noOtherAccountToSwitch() = onNoOtherAccountToSwitch()
}

internal fun WireNavigation3ProductionActions.asSwitchAccountActions(): SwitchAccountActions =
    WireNavigation3SwitchAccountActions(
        onSwitchedToAnotherAccount = ::switchedToAnotherAccount,
        onNoOtherAccountToSwitch = ::noOtherAccountToSwitch,
    )

/**
 * Completes navigation away from session-backed authentication before its temporary session is
 * deleted. Both outcomes must close the same graph generation: either another account becomes
 * active or the app returns to login with no active account.
 */
internal fun sessionCancellationSwitchAccountActions(
    delegate: SwitchAccountActions,
    sessionId: WireSessionId,
    onNavigationCompleted: (WireSessionId) -> Unit,
): SwitchAccountActions = WireNavigation3SwitchAccountActions(
    onSwitchedToAnotherAccount = {
        completeSessionCancellation(sessionId, delegate::switchedToAnotherAccount, onNavigationCompleted)
    },
    onNoOtherAccountToSwitch = {
        completeSessionCancellation(sessionId, delegate::noOtherAccountToSwitch, onNavigationCompleted)
    },
)

/**
 * Graph teardown is mandatory even if the navigation callback fails: ClearSessionViewModel deletes
 * the temporary session from its own NonCancellable finally block, so leaving its graph generation
 * active would allow a later login to resolve stale state. The original failure is still rethrown.
 */
private inline fun completeSessionCancellation(
    sessionId: WireSessionId,
    navigateAway: () -> Unit,
    onNavigationCompleted: (WireSessionId) -> Unit,
) {
    try {
        navigateAway()
    } finally {
        onNavigationCompleted(sessionId)
    }
}

internal fun noOtherAccountNavigationCommand(
    currentRoute: WireRoute?,
    useNewLogin: Boolean,
    recoveryServerLinks: AuthenticationServerLinks? = null,
): WireNavigationCommand? {
    val loginArguments = AuthenticationLoginArguments(
        loginPasswordPath = recoveryServerLinks?.let(::AuthenticationLoginPasswordPath),
    )
    val destination = if (useNewLogin) {
        NewLoginRoute.start(loginArguments)
    } else {
        WelcomeRoute(customServerConfig = recoveryServerLinks)
    }
    return destination
        .takeUnless { currentRoute.hasEquivalentAuthenticationTarget(it) }
        ?.let { WireNavigationCommand(it, WireBackStackMode.CLEAR_WHOLE) }
}

private fun WireRoute?.hasEquivalentAuthenticationTarget(target: WireRoute): Boolean = when {
    this is NewLoginRoute && target is NewLoginRoute -> args == target.args
    this is WelcomeRoute && target is WelcomeRoute -> customServerConfig == target.customServerConfig
    else -> false
}
