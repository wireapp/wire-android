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

import com.wire.android.navigation.routes.auth.NewWelcomeEmptyStartRoute
import com.wire.android.navigation.style.BackgroundType
import com.wire.navigation.AuthBackgroundRoute
import com.wire.navigation.AuthenticationScreenRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireRoute

/**
 * Typed Activity policy shared by the Navigation 3 host and unit tests.
 *
 * It replaces generated destination style lookup and legacy base-route classification. Session
 * ownership is derived exclusively from route marker interfaces, so restored routes and deep
 * links follow the same policy as ordinary navigation.
 */
internal object WireNavigation3ActivityPolicy {

    fun backgroundType(route: WireRoute?): BackgroundType =
        if (route is AuthBackgroundRoute) BackgroundType.Auth else BackgroundType.Default

    fun sessionSnapshot(
        route: WireRoute?,
        hasCurrentSession: Boolean,
        isUserUiBlocked: Boolean,
        isSessionTransitionInProgress: Boolean,
        isSelfLogoutTransition: Boolean,
    ): WireSessionNavigationSnapshot =
        WireSessionNavigationSnapshot(
            hasCurrentSession = hasCurrentSession,
            hasCurrentRoute = route != null,
            isEmptyWelcomeRoute = route is NewWelcomeEmptyStartRoute,
            isAuthenticationRoute = route is AuthenticationScreenRoute,
            isSessionBackedAuthenticationRoute =
                route is AuthenticationScreenRoute && route is SessionRoute,
            isUserUiBlocked = isUserUiBlocked,
            isSessionTransitionInProgress = isSessionTransitionInProgress,
            isSelfLogoutTransition = isSelfLogoutTransition,
        )
}
