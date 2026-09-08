/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.wire.android.appLogger
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Router
import com.wire.android.navigation.routes.auth.AuthenticationNavigationTransition
import com.wire.android.navigation.routes.auth.NewLoginRoute
import com.wire.android.navigation.runtime.WireNavigation3ActivityPolicy
import com.wire.android.navigation.runtime.WireSessionNavigationDecision
import com.wire.android.navigation.runtime.WireSessionNavigationPolicy
import com.wire.android.navigation.runtime.WireSessionNavigationSnapshot
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId

/**
 * Activity-level session policy for the Navigation 3 host.
 *
 * The current route owns its Metro session through [SessionRoute]. The globally selected account
 * remains an application policy input only; it no longer determines a NavHost or ViewModel graph
 * key and therefore cannot tear down navigation during a transient account switch.
 */
@Composable
internal fun HandleNavigation3SessionEffects(
    runtime: WireNavigation3Runtime,
    authenticationRouter: AuthenticationNavigation3Router,
    currentUserId: WireSessionId?,
    isUserUiBlocked: Boolean,
    isSessionTransitionInProgress: Boolean,
    isSelfLogoutTransition: Boolean,
    finishSessionTransition: () -> Unit,
    resolveMissingCurrentSession: () -> Unit,
) {
    val currentRoute = runtime.navigator.currentRoute

    LaunchedEffect(
        currentRoute,
        currentUserId,
        isSessionTransitionInProgress,
        isUserUiBlocked,
        isSelfLogoutTransition,
    ) {
        val snapshot = WireNavigation3ActivityPolicy.sessionSnapshot(
            route = currentRoute,
            hasCurrentSession = currentUserId != null,
            isUserUiBlocked = isUserUiBlocked,
            isSessionTransitionInProgress = isSessionTransitionInProgress,
            isSelfLogoutTransition = isSelfLogoutTransition,
        )
        appLogger.i(
            "WireActivity graph route=${currentRoute?.routeId} " +
                "routeSession=${(currentRoute as? SessionRoute)?.sessionId} " +
                "currentSession=$currentUserId"
        )
        when (
            val effect = WireActivityNavigation3SessionEffectResolver.resolve(
                snapshot = snapshot,
                currentSessionId = currentUserId,
                routeSessionId = (currentRoute as? SessionRoute)?.sessionId,
            )
        ) {
            WireActivityNavigation3SessionEffect.FinishTransition ->
                finishSessionTransition()

            WireActivityNavigation3SessionEffect.WaitForBlockingDialog ->
                appLogger.i("WireActivity blocking session dialog visible; waiting for user action")

            is WireActivityNavigation3SessionEffect.Navigate ->
                authenticationRouter.navigate(
                    AuthenticationNavigationTransition.SESSION_POLICY,
                    effect.command,
                )

            WireActivityNavigation3SessionEffect.ResolveCurrentSession ->
                resolveMissingCurrentSession()

            WireActivityNavigation3SessionEffect.None -> Unit
        }
    }
}

internal sealed interface WireActivityNavigation3SessionEffect {
    data object FinishTransition : WireActivityNavigation3SessionEffect
    data object WaitForBlockingDialog : WireActivityNavigation3SessionEffect
    data class Navigate(val command: WireNavigationCommand) : WireActivityNavigation3SessionEffect
    data object ResolveCurrentSession : WireActivityNavigation3SessionEffect
    data object None : WireActivityNavigation3SessionEffect
}

internal object WireActivityNavigation3SessionEffectResolver {
    fun resolve(
        snapshot: WireSessionNavigationSnapshot,
        currentSessionId: WireSessionId?,
        routeSessionId: WireSessionId? = null,
    ): WireActivityNavigation3SessionEffect {
        if (snapshot.isSessionTransitionInProgress && snapshot.isAuthenticationRoute) {
            return WireActivityNavigation3SessionEffect.FinishTransition
        }
        return when (WireSessionNavigationPolicy.resolve(snapshot)) {
            WireSessionNavigationDecision.WAIT_FOR_BLOCKING_DIALOG ->
                WireActivityNavigation3SessionEffect.WaitForBlockingDialog

            WireSessionNavigationDecision.NAVIGATE_HOME_CLEAR_STACK ->
                currentSessionId?.let {
                    navigate(HomeRoute(it))
                } ?: WireActivityNavigation3SessionEffect.None

            WireSessionNavigationDecision.NAVIGATE_LOGIN_CLEAR_STACK ->
                navigate(NewLoginRoute.start())

            WireSessionNavigationDecision.RESOLVE_CURRENT_SESSION ->
                WireActivityNavigation3SessionEffect.ResolveCurrentSession

            WireSessionNavigationDecision.NONE ->
                if (hasMismatchedOrdinarySession(snapshot, currentSessionId, routeSessionId)) {
                    navigate(HomeRoute(checkNotNull(currentSessionId)))
                } else {
                    WireActivityNavigation3SessionEffect.None
                }
        }
    }

    private fun hasMismatchedOrdinarySession(
        snapshot: WireSessionNavigationSnapshot,
        currentSessionId: WireSessionId?,
        routeSessionId: WireSessionId?,
    ): Boolean = currentSessionId != null &&
        routeSessionId != null &&
        currentSessionId != routeSessionId &&
        !snapshot.isSessionBackedAuthenticationRoute

    private fun navigate(route: WireRoute) =
        WireActivityNavigation3SessionEffect.Navigate(
            WireNavigationCommand(route, WireBackStackMode.CLEAR_WHOLE)
        )
}
