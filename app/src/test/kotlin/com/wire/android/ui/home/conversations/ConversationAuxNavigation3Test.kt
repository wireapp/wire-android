/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversations

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ConversationAuxNavigation3Test {
    private val session = WireSessionId("user", "wire.example")
    private val conversation = ConversationAuxId("conversation", "wire.example")

    @Test
    fun givenConversationAuxRoutes_whenSerialized_thenArgumentsAndIdentitySurvive() {
        assertRoundTrip(BrowseChannelsRoute(session, WireNavEntryId("browse")))
        assertRoundTrip(
            ConversationFoldersRoute(session, conversation, "name", "folder", WireNavEntryId("folders"))
        )
        assertRoundTrip(NewConversationFolderRoute(session, "folders", WireNavEntryId("new-folder")))
        assertRoundTrip(
            SearchConversationMessagesRoute(session, conversation, "name", true, WireNavEntryId("search"))
        )
        assertRoundTrip(
            PromoteAdminRoute(
                session,
                conversation,
                listOf(ConversationAuxId("admin", "wire.example")),
                WireNavEntryId("promote"),
            )
        )
        assertRoundTrip(
            AddMembersSearchRoute(
                session,
                conversation,
                isConversationAppsEnabled = true,
                isSelfPartOfATeam = true,
                protocol = ConversationProtocolSelection.Mixed(
                    groupId = "group",
                    groupState = ConversationProtocolSelection.GroupState.ESTABLISHED,
                    epoch = 42u,
                    keyingMaterialLastUpdate = "2026-01-01T00:00:00Z",
                    cipherSuiteTag = 1,
                ),
                entryId = WireNavEntryId("members"),
            )
        )
        assertRoundTrip(DebugConversationRoute(session, conversation, WireNavEntryId("debug")))
    }

    @Test
    fun givenSameRoute_whenCreatedTwice_thenEntryIdentityIsUnique() {
        val first = BrowseChannelsRoute(session)
        val second = BrowseChannelsRoute(session)

        assertNotEquals(first.entryId, second.entryId)
        assertEquals(first.routeId, second.routeId)
    }

    private inline fun <reified T> assertRoundTrip(value: T) {
        assertEquals(value, Json.decodeFromString<T>(Json.encodeToString(value)))
    }
}
