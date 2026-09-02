/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversations.details

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ConversationDetailsNavigation3Test {
    private val session = WireSessionId("user", "wire.example")
    private val conversation = ConversationDetailsId("conversation", "wire.example")

    @Test
    fun givenAllConversationDetailsRoutes_whenSerialized_thenIdentityAndArgumentsSurvive() {
        assertRoundTrip(GroupConversationDetailsRoute(session, conversation, WireNavEntryId("details")))
        assertRoundTrip(EditConversationNameRoute(session, conversation, WireNavEntryId("name")))
        assertRoundTrip(EditSelfDeletingMessagesRoute(session, conversation, WireNavEntryId("timer")))
        assertRoundTrip(GroupConversationAllParticipantsRoute(session, conversation, WireNavEntryId("participants")))
        assertRoundTrip(UpdateAppsAccessRoute(session, conversation, true, false, true, WireNavEntryId("apps")))
        assertRoundTrip(
            ChannelAccessOnUpdateRoute(
                session,
                conversation,
                ChannelAccessSelection.PUBLIC,
                ChannelPermissionSelection.EVERYONE,
                WireNavEntryId("channel"),
            )
        )
        assertRoundTrip(EditGuestAccessRoute(session, conversation, true, false, true, WireNavEntryId("guests")))
        assertRoundTrip(CreatePasswordProtectedGuestLinkRoute(session, conversation, WireNavEntryId("guest-link")))
    }

    @Test
    fun givenSameDestination_whenCreatedTwice_thenEntryIdentityIsUnique() {
        val first = GroupConversationDetailsRoute(session, conversation)
        val second = GroupConversationDetailsRoute(session, conversation)

        assertNotEquals(first.entryId, second.entryId)
        assertEquals(first.routeId, second.routeId)
    }

    private inline fun <reified T : ConversationDetailsRoute> assertRoundTrip(route: T) {
        assertEquals(route, Json.decodeFromString<T>(Json.encodeToString(route)))
    }
}
