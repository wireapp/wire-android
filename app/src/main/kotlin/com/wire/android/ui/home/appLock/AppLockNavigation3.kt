/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.appLock

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

internal fun resolveAppLockStartRoute(
    sessionId: WireSessionId,
    setTeamAppLock: Boolean,
    canAuthenticateWithBiometrics: Boolean,
): SessionRoute = when {
    setTeamAppLock -> SetLockCodeRoute(sessionId)
    canAuthenticateWithBiometrics -> AppUnlockWithBiometricsRoute(sessionId)
    else -> EnterLockCodeRoute(sessionId)
}

@Serializable
data class SetLockCodeRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/set_lock_code_screen"
    }
}

@Serializable
data class ForgotLockCodeRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/forgot_lock_code_screen"
    }
}

@Serializable
data class EnterLockCodeRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/enter_lock_code_screen"
    }
}

@Serializable
data class AppUnlockWithBiometricsRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/app_unlock_with_biometrics_screen"
    }
}
