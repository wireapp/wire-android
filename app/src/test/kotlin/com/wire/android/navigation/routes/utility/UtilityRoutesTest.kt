/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.routes.utility

import com.wire.android.navigation.routes.auth.InitialSyncRoute
import com.wire.navigation.AuthenticationScreenRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class UtilityRoutesTest {

    @Test
    fun givenInitialSyncRoute_whenClassifyingScreen_thenItSuppressesAuthenticationChrome() {
        assertInstanceOf(AuthenticationScreenRoute::class.java, InitialSyncRoute(SESSION_ID))
    }

    @Test
    fun givenUtilityRoutes_whenReadingRouteIds_thenLegacyAnalyticsIdentitiesArePreserved() {
        assertEquals("app/initial_sync_screen", InitialSyncRoute.ROUTE_ID)
        assertEquals("app/debug_screen", DebugRoute.ROUTE_ID)
        assertEquals("app/log_management_screen", LogManagementRoute.ROUTE_ID)
        assertEquals("app/debug_feature_flags_screen", DebugFeatureFlagsRoute.ROUTE_ID)
        assertEquals("app/conversation_crypto_stats_screen", ConversationCryptoStatsRoute.ROUTE_ID)
        assertEquals("app/security_providers_screen", SecurityProvidersRoute.ROUTE_ID)
    }

    @Test
    fun givenInitialSyncRoute_whenSerializedAndRestored_thenSessionAndEntryIdentityArePreserved() {
        val route = InitialSyncRoute(
            sessionId = SESSION_ID,
            entryId = WireNavEntryId("initial-sync-entry"),
        )

        val restored = Json.decodeFromString<InitialSyncRoute>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals(SESSION_ID, restored.sessionId)
        assertEquals("initial-sync-entry", restored.entryId.value)
    }

    @Test
    fun givenSameDebugDestination_whenCreatedTwice_thenEntryIdentitiesAreDifferent() {
        val first = DebugRoute(SESSION_ID)
        val second = DebugRoute(SESSION_ID)

        assertEquals(first.routeId, second.routeId)
        assertNotEquals(first.entryId, second.entryId)
    }

    private companion object {
        val SESSION_ID = WireSessionId("user", "wire.example")
    }
}
