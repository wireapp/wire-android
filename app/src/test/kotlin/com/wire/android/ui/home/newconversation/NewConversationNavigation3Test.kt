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

package com.wire.android.ui.home.newconversation

import java.io.File
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NewConversationNavigation3Test {

    @Test
    fun givenNewConversationStartFactory_whenCreatingRoute_thenEntryAndFlowOwnershipMatch() {
        val route = NewConversationSearchPeopleRoute.start(sessionId)

        assertEquals(route.entryId.value, route.flowId)
    }

    private val sessionId = WireSessionId("user", "wire.example")
    private val flowId = "new-conversation-42"

    @Test
    fun givenNewConversationRoutes_whenSerializedAndRestored_thenScopeAndIdentityArePreserved() {
        assertSerializationRoundTrip(
            NewConversationSearchPeopleRoute(
                sessionId,
                flowId,
                WireNavEntryId("search-entry"),
            )
        )
        assertSerializationRoundTrip(
            NewGroupConversationSearchPeopleRoute(
                sessionId,
                flowId,
                WireNavEntryId("group-search-entry"),
            )
        )
        assertSerializationRoundTrip(
            NewGroupNameRoute(
                sessionId,
                flowId,
                WireNavEntryId("group-name-entry"),
            )
        )
        assertSerializationRoundTrip(
            GroupOptionRoute(
                sessionId,
                flowId,
                WireNavEntryId("group-option-entry"),
            )
        )
        assertSerializationRoundTrip(
            ChannelAccessOnCreateRoute(
                sessionId,
                flowId,
                WireNavEntryId("channel-access-entry"),
            )
        )
    }

    @Test
    fun givenNewConversationRoutes_whenReadingRouteIds_thenStableRouteIdsArePreserved() {
        val expectations = mapOf(
            NewConversationSearchPeopleRoute(sessionId, flowId) to
                NewConversationSearchPeopleRoute.ROUTE_ID,
            NewGroupConversationSearchPeopleRoute(sessionId, flowId) to
                NewGroupConversationSearchPeopleRoute.ROUTE_ID,
            NewGroupNameRoute(sessionId, flowId) to NewGroupNameRoute.ROUTE_ID,
            GroupOptionRoute(sessionId, flowId) to GroupOptionRoute.ROUTE_ID,
            ChannelAccessOnCreateRoute(sessionId, flowId) to
                ChannelAccessOnCreateRoute.ROUTE_ID,
        )

        expectations.forEach { (route, expectedRouteId) ->
            assertEquals(expectedRouteId, route.routeId)
        }
    }

    @Test
    fun givenEveryStepOfOneFlow_whenReadingScope_thenSessionAndFlowAreIdentical() {
        val routes = allRoutes()

        assertEquals(setOf(sessionId), routes.map { it.sessionId }.toSet())
        assertEquals(setOf(flowId), routes.map { it.flowId }.toSet())
    }

    @Test
    fun givenSameDestinationAndScope_whenCreatingTwoEntries_thenEntryIdentityIsUnique() {
        val first = GroupOptionRoute(sessionId, flowId)
        val second = GroupOptionRoute(sessionId, flowId)

        assertNotEquals(first.entryId, second.entryId)
        assertEquals(first.routeId, second.routeId)
        assertEquals(first.flowId, second.flowId)
    }

    @Test
    fun givenBlankFlowId_whenCreatingAnyNewConversationRoute_thenItIsRejected() {
        val factories: List<() -> NewConversationRoute> = listOf(
            { NewConversationSearchPeopleRoute(sessionId, " ") },
            { NewGroupConversationSearchPeopleRoute(sessionId, " ") },
            { NewGroupNameRoute(sessionId, " ") },
            { GroupOptionRoute(sessionId, " ") },
            { ChannelAccessOnCreateRoute(sessionId, " ") },
        )

        factories.forEach { factory ->
            assertThrows(IllegalArgumentException::class.java) {
                factory()
            }
        }
    }

    @Test
    fun givenNewConversationStartEntry_whenRegistered_thenItRetainsBottomUpPresentation() {
        val source = sourceFile("NewConversationNavigation3Entries.kt").readText()

        assertTrue(
            "wireEntry<NewConversationSearchPeopleRoute>(" in source &&
                "presentation = WireEntryPresentation.PopUp" in source
        )
    }

    private fun allRoutes(): List<NewConversationRoute> = listOf(
        NewConversationSearchPeopleRoute(sessionId, flowId),
        NewGroupConversationSearchPeopleRoute(sessionId, flowId),
        NewGroupNameRoute(sessionId, flowId),
        GroupOptionRoute(sessionId, flowId),
        ChannelAccessOnCreateRoute(sessionId, flowId),
    )

    private inline fun <reified T : NewConversationRoute> assertSerializationRoundTrip(
        route: T,
    ) {
        val restored = Json.decodeFromString<T>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals(sessionId, restored.sessionId)
        assertEquals(flowId, restored.flowId)
        assertEquals(route.entryId, restored.entryId)
        assertEquals(route.routeId, restored.routeId)
    }

    private fun sourceFile(name: String): File {
        val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(root, "app/src/main/kotlin/com/wire/android/ui/home/newconversation/$name")
    }
}
