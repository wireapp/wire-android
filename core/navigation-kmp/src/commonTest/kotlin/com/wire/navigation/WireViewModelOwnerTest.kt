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

package com.wire.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlinx.serialization.json.Json

class WireViewModelOwnerTest {

    @Test
    fun givenEveryOwnerType_whenSerializedAndRestored_thenValueIsPreserved() {
        val owners = listOf(
            WireViewModelOwner.Entry(WireNavEntryId("entry-id")),
            WireViewModelOwner.Flow("authentication"),
            WireViewModelOwner.Session(WireSessionId("user-id", "wire.example")),
            WireViewModelOwner.Application,
        )

        owners.forEach { owner ->
            val serialized = Json.encodeToString(WireViewModelOwner.serializer(), owner)

            assertEquals(
                owner,
                Json.decodeFromString(WireViewModelOwner.serializer(), serialized),
            )
        }
    }

    @Test
    fun givenOwnerValues_whenCreatingStableKeys_thenKeysContainTypedIdentity() {
        assertEquals(
            "entry:entry-id",
            WireViewModelOwner.Entry(WireNavEntryId("entry-id")).stableKey(),
        )
        assertEquals(
            "flow:create-conversation",
            WireViewModelOwner.Flow("create-conversation").stableKey(),
        )
        assertEquals(
            "session:user-id@wire.example",
            WireViewModelOwner.Session(WireSessionId("user-id", "wire.example")).stableKey(),
        )
        assertEquals("application", WireViewModelOwner.Application.stableKey())
    }

    @Test
    fun givenDifferentSessionDomains_whenCreatingStableKeys_thenKeysDoNotCollide() {
        val first = WireViewModelOwner.Session(WireSessionId("user-id", "wire.example"))
        val second = WireViewModelOwner.Session(WireSessionId("user-id", "other.example"))

        assertNotEquals(first.stableKey(), second.stableKey())
    }

    @Test
    fun givenBlankFlowOwnerId_whenCreatingOwner_thenCreationFails() {
        assertFailsWith<IllegalArgumentException> {
            WireViewModelOwner.Flow(" ")
        }
    }

    @Test
    fun givenOrdinaryRoute_whenResolvingEntryOwner_thenConcreteEntryOwnsViewModel() {
        val route = TestRoute(entryId = WireNavEntryId("ordinary"))

        assertEquals(
            WireViewModelOwner.Entry(route.entryId),
            route.entryViewModelOwner(),
        )
    }

    @Test
    fun givenRouteWithFlowId_whenResolvingEntryOwner_thenConcreteEntryStillOwnsViewModel() {
        val route = TestRoute(
            entryId = WireNavEntryId("login-email"),
            flowId = "authentication",
        )

        assertEquals(
            WireViewModelOwner.Entry(route.entryId),
            route.entryViewModelOwner(),
        )
    }

    @Test
    fun givenSessionRoute_whenResolvingEntryOwner_thenConcreteEntryStillOwnsViewModel() {
        val route = TestSessionRoute(entryId = WireNavEntryId("session-screen"))

        assertEquals(
            WireViewModelOwner.Entry(route.entryId),
            route.entryViewModelOwner(),
        )
    }

    @Test
    fun givenOrdinaryRoute_whenListingSharedOwners_thenOnlyApplicationOwnerIsAvailable() {
        val route = TestRoute(entryId = WireNavEntryId("ordinary"))

        assertEquals(
            setOf(WireViewModelOwner.Application),
            route.availableSharedViewModelOwners(),
        )
    }

    @Test
    fun givenRouteWithFlowId_whenListingSharedOwners_thenFlowAndApplicationOwnersAreAvailable() {
        val route = TestRoute(
            entryId = WireNavEntryId("login-email"),
            flowId = "authentication",
        )

        assertEquals(
            setOf(
                WireViewModelOwner.Flow("authentication"),
                WireViewModelOwner.Application,
            ),
            route.availableSharedViewModelOwners(),
        )
    }

    @Test
    fun givenSessionRoute_whenListingSharedOwners_thenSessionAndApplicationOwnersAreAvailable() {
        val route = TestSessionRoute(entryId = WireNavEntryId("session-screen"))

        assertEquals(
            setOf(
                WireViewModelOwner.Session(route.sessionId),
                WireViewModelOwner.Application,
            ),
            route.availableSharedViewModelOwners(),
        )
    }

    @Test
    fun givenSessionRouteWithFlowId_whenListingSharedOwners_thenAllExplicitSharedOwnersAreAvailable() {
        val route = TestSessionRoute(
            entryId = WireNavEntryId("session-flow"),
            flowId = "authentication",
        )

        assertEquals(
            setOf(
                WireViewModelOwner.Flow("authentication"),
                WireViewModelOwner.Session(route.sessionId),
                WireViewModelOwner.Application,
            ),
            route.availableSharedViewModelOwners(),
        )
    }

    private data class TestRoute(
        override val entryId: WireNavEntryId,
        override val routeId: String = "test",
        override val flowId: String? = null,
    ) : WireRoute

    private data class TestSessionRoute(
        override val entryId: WireNavEntryId,
        override val routeId: String = "test-session",
        override val flowId: String? = null,
        override val sessionId: WireSessionId = WireSessionId("user-id", "wire.example"),
    ) : SessionRoute
}
