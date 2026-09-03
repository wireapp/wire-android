/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.navigation.routes.media

import com.wire.navigation.AuthenticationRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaNavigation3Test {
    private val session = WireSessionId("user", "wire.example")
    private val conversation = MediaConversationId("conversation", "wire.example")

    @Test
    fun givenImportMediaVariants_whenInspectingScope_thenEachHasOneUnambiguousOwner() {
        val loggedOut = LoggedOutImportMediaRoute()
        val authenticated = AuthenticatedImportMediaRoute(session)

        assertTrue(loggedOut is AuthenticationRoute)
        assertTrue(authenticated is SessionRoute)
        assertEquals(loggedOut.routeId, authenticated.routeId)
        assertEquals(session, authenticated.sessionId)
    }

    @Test
    fun givenMediaRoutes_whenSerialized_thenArgumentsAndIdentitySurvive() {
        assertRoundTrip(ConversationMediaRoute(session, conversation))
        assertRoundTrip(ImagesPreviewRoute(session, conversation, "name", listOf("content://asset")))
        assertRoundTrip(MediaGalleryRoute(session, conversation, "message", true, false, true))
        assertRoundTrip(MessageDetailsRoute(session, conversation, "message", true))
        assertRoundTrip(LoggedOutImportMediaRoute())
        assertRoundTrip(AuthenticatedImportMediaRoute(session))
        assertRoundTrip(
            AuthenticatedImportMediaRoute(
                sessionId = session,
                internalAssetUriStrings = listOf("content://com.wire.android.share/logs.zip"),
            )
        )
    }

    private inline fun <reified T> assertRoundTrip(value: T) {
        assertEquals(value, Json.decodeFromString<T>(Json.encodeToString(value)))
    }
}
