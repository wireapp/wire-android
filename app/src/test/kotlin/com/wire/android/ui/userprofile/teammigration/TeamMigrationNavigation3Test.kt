/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.teammigration

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TeamMigrationNavigation3Test {

    private val sessionId = WireSessionId("user", "wire.example")
    private val flowId = "team-migration:flow-42"

    @Test
    fun givenTeamMigrationRoutes_whenSerializedAndRestored_thenScopeAndIdentityArePreserved() {
        assertSerializationRoundTrip(
            TeamMigrationTeamPlanRoute(
                sessionId,
                flowId,
                isMigrationDotActive = true,
                entryId = WireNavEntryId("plan"),
            )
        )
        assertSerializationRoundTrip(
            TeamMigrationTeamNameRoute(sessionId, flowId, WireNavEntryId("name"))
        )
        assertSerializationRoundTrip(
            TeamMigrationConfirmationRoute(sessionId, flowId, WireNavEntryId("confirmation"))
        )
        assertSerializationRoundTrip(
            TeamMigrationDoneRoute(sessionId, flowId, WireNavEntryId("done"))
        )
    }

    @Test
    fun givenTeamMigrationRoutes_whenReadingRouteIds_thenStableRouteIdsArePreserved() {
        val expectations = mapOf(
            TeamMigrationTeamPlanRoute(sessionId, flowId) to
                TeamMigrationTeamPlanRoute.ROUTE_ID,
            TeamMigrationTeamNameRoute(sessionId, flowId) to
                TeamMigrationTeamNameRoute.ROUTE_ID,
            TeamMigrationConfirmationRoute(sessionId, flowId) to
                TeamMigrationConfirmationRoute.ROUTE_ID,
            TeamMigrationDoneRoute(sessionId, flowId) to
                TeamMigrationDoneRoute.ROUTE_ID,
        )

        expectations.forEach { (route, expectedRouteId) ->
            assertEquals(expectedRouteId, route.routeId)
        }
    }

    @Test
    fun givenStartFactory_whenCreatingTwoFlows_thenOwnershipAndEntryIdentityAreUnique() {
        val first = TeamMigrationTeamPlanRoute.start(sessionId, isMigrationDotActive = true)
        val second = TeamMigrationTeamPlanRoute.start(sessionId, isMigrationDotActive = true)

        assertNotEquals(first.entryId, second.entryId)
        assertNotEquals(first.flowId, second.flowId)
        assertEquals("team-migration:${first.entryId.value}", first.flowId)
        assertEquals(true, first.isMigrationDotActive)
    }

    @Test
    fun givenEveryStepOfOneFlow_whenReadingScope_thenSessionAndFlowAreIdentical() {
        val routes = allRoutes()

        assertEquals(setOf(sessionId), routes.map { it.sessionId }.toSet())
        assertEquals(setOf(flowId), routes.map { it.flowId }.toSet())
    }

    @Test
    fun givenBlankFlowId_whenCreatingAnyTeamMigrationRoute_thenItIsRejected() {
        val factories: List<() -> TeamMigrationRoute> = listOf(
            { TeamMigrationTeamPlanRoute(sessionId, " ") },
            { TeamMigrationTeamNameRoute(sessionId, " ") },
            { TeamMigrationConfirmationRoute(sessionId, " ") },
            { TeamMigrationDoneRoute(sessionId, " ") },
        )

        factories.forEach { factory ->
            assertThrows(IllegalArgumentException::class.java) {
                factory()
            }
        }
    }

    private fun allRoutes(): List<TeamMigrationRoute> = listOf(
        TeamMigrationTeamPlanRoute(sessionId, flowId),
        TeamMigrationTeamNameRoute(sessionId, flowId),
        TeamMigrationConfirmationRoute(sessionId, flowId),
        TeamMigrationDoneRoute(sessionId, flowId),
    )

    private inline fun <reified T : TeamMigrationRoute> assertSerializationRoundTrip(route: T) {
        val restored = Json.decodeFromString<T>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals(sessionId, restored.sessionId)
        assertEquals(flowId, restored.flowId)
        assertEquals(route.entryId, restored.entryId)
        assertEquals(route.routeId, restored.routeId)
    }
}
