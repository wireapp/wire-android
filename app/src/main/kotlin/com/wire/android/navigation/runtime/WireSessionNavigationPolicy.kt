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

package com.wire.android.navigation.runtime

/**
 * Navigation-runtime-neutral snapshot used to reconcile session state with the visible route.
 *
 * Route classification deliberately happens at the host boundary and is derived exclusively from
 * typed [com.wire.navigation.WireRoute] markers.
 */
internal data class WireSessionNavigationSnapshot(
    val hasCurrentSession: Boolean,
    val hasCurrentRoute: Boolean,
    val isEmptyWelcomeRoute: Boolean,
    val isAuthenticationRoute: Boolean,
    val isSessionBackedAuthenticationRoute: Boolean,
    val isUserUiBlocked: Boolean,
    val isSessionTransitionInProgress: Boolean,
    val isSelfLogoutTransition: Boolean,
)

internal enum class WireSessionNavigationDecision {
    NONE,
    WAIT_FOR_BLOCKING_DIALOG,
    NAVIGATE_HOME_CLEAR_STACK,
    NAVIGATE_LOGIN_CLEAR_STACK,
    RESOLVE_CURRENT_SESSION,
}

internal object WireSessionNavigationPolicy {

    fun resolve(snapshot: WireSessionNavigationSnapshot): WireSessionNavigationDecision = when {
        snapshot.isUserUiBlocked ->
            WireSessionNavigationDecision.WAIT_FOR_BLOCKING_DIALOG

        snapshot.shouldNavigateHome ->
            WireSessionNavigationDecision.NAVIGATE_HOME_CLEAR_STACK

        snapshot.shouldNavigateLogin ->
            WireSessionNavigationDecision.NAVIGATE_LOGIN_CLEAR_STACK

        snapshot.isSessionTransitionInProgress ->
            if (snapshot.shouldResolveTransition) {
                WireSessionNavigationDecision.RESOLVE_CURRENT_SESSION
            } else {
                WireSessionNavigationDecision.NONE
            }

        snapshot.shouldResolveMissingOrdinarySession ->
            WireSessionNavigationDecision.RESOLVE_CURRENT_SESSION

        else -> WireSessionNavigationDecision.NONE
    }
}

private val WireSessionNavigationSnapshot.shouldNavigateHome
    get() = hasCurrentSession && isEmptyWelcomeRoute

private val WireSessionNavigationSnapshot.shouldNavigateLogin
    get() = !hasCurrentSession && (isEmptyWelcomeRoute || isSessionBackedAuthenticationRoute)

private val WireSessionNavigationSnapshot.shouldResolveTransition
    get() = hasCurrentRoute && !isAuthenticationRoute && !isSelfLogoutTransition

private val WireSessionNavigationSnapshot.shouldResolveMissingOrdinarySession
    get() = !hasCurrentSession && hasCurrentRoute && !isAuthenticationRoute
