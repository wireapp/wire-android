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

package com.wire.android.navigation.runtime.startup

import com.wire.android.navigation.routes.auth.NewWelcomeEmptyStartRoute
import com.wire.android.navigation.routes.auth.WelcomeRoute
import com.wire.android.ui.InitialAppState
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentRoute
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId

internal enum class WireStartupLoginType {
    LEGACY,
    NEW;

    companion object {
        fun fromCanUseNewLogin(canUseNewLogin: Boolean): WireStartupLoginType =
            if (canUseNewLogin) NEW else LEGACY
    }
}

/**
 * Pure startup policy. It selects a typed route without knowing about NavController, Bundles, or
 * generated Compose Destinations APIs.
 */
internal object WireInitialRouteResolver {

    fun resolve(
        initialAppState: InitialAppState,
        loginType: WireStartupLoginType,
        activeSessionId: WireSessionId?,
    ): WireRoute = when (initialAppState) {
        InitialAppState.NotLoggedIn -> when (loginType) {
            WireStartupLoginType.NEW -> NewWelcomeEmptyStartRoute()
            WireStartupLoginType.LEGACY -> WelcomeRoute()
        }

        is InitialAppState.EnrollE2EI -> E2EIEnrollmentRoute(
            sessionId = initialAppState.userId.toWireSessionId(),
        )

        InitialAppState.LoggedIn -> HomeRoute(
            sessionId = requireNotNull(activeSessionId) {
                "A logged-in startup route requires the active session id"
            },
        )
    }
}

internal fun UserId.toWireSessionId(): WireSessionId = WireSessionId(
    value = value,
    domain = domain,
)
