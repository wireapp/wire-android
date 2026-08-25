/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.navigation.routes.utility

import com.wire.navigation.AuthenticationScreenRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

/** Post-login authentication gate shown until the current account has completed initial sync. */
@Serializable
data class InitialSyncRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute, AuthenticationScreenRoute {
    override val routeId: String get() = ROUTE_ID

    companion object {
        // Preserve the pre-extraction analytics and serialized route identity.
        const val ROUTE_ID = "app/initial_sync_screen"
    }
}
