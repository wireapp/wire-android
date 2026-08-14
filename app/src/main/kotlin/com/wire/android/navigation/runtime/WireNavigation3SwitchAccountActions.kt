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
