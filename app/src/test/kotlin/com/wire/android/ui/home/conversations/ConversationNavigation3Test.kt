/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */

package com.wire.android.ui.home.conversations

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationNavigation3Test {
    private val session = WireSessionId("user", "wire.example")

    @Test
    fun givenConversationRouteWithPendingShare_whenSerialized_thenAllTypedArgumentsSurvive() {
        val route = ConversationRoute(
            sessionId = session,
            conversationId = ConversationRouteId("conversation", "wire.example"),
            searchedMessageId = "message",
            pendingAssets = listOf(
                ConversationPendingAsset(
                    key = "asset",
                    mimeType = "image/png",
                    dataPath = "/cache/image",
                    dataSize = 42,
                    fileName = "image.png",
                    assetType = "IMAGE",
                    audioWavesMask = listOf(1, 2),
                )
            ),
            pendingText = "shared text",
            entryId = WireNavEntryId("conversation-entry"),
        )

        assertEquals(route, Json.decodeFromString<ConversationRoute>(Json.encodeToString(route)))
        assertTrue(route is SessionRoute)
        assertEquals(session, route.sessionId)
    }

    @Test
    fun givenConversationCompletion_whenSerialized_thenActionAndNameSurvive() {
        ConversationCompletionAction.entries.forEach { action ->
            val result = ConversationCompletionResult(action, "Conversation")
            assertEquals(
                result,
                Json.decodeFromString<ConversationCompletionResult>(Json.encodeToString(result)),
            )
        }
    }

    @Test
    fun givenSameConversation_whenOpenedTwice_thenEntryIdentityIsUnique() {
        val id = ConversationRouteId("conversation", "wire.example")
        assertNotEquals(
            ConversationRoute(session, id).entryId,
            ConversationRoute(session, id).entryId,
        )
    }
}
