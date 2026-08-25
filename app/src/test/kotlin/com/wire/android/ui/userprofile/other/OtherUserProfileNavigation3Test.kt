/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.other

import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class OtherUserProfileNavigation3Test {

    @Test
    fun givenLegacyOtherProfileArgs_whenMappingToNavigationAndBack_thenValueIsUnchanged() {
        val legacy = OtherUserProfileNavArgs(
            userId = UserId("other", "remote.example"),
            groupConversationId = ConversationId("group", "wire.example"),
        )

        val route = legacy.toOtherUserProfileRoute(
            sessionId = WireSessionId("self", "wire.example"),
            entryId = WireNavEntryId("other-profile-entry"),
        )

        assertEquals(legacy, route.toLegacyNavArgs())
        assertEquals("self", route.sessionId.value)
        assertEquals("other", route.targetUserId.value)
        assertEquals("app/other_user_profile_screen", route.routeId)
    }

    @Test
    fun givenOtherProfileRoute_whenSerializedAndRestored_thenArgumentsAndIdentityArePreserved() {
        val route = OtherUserProfileNavArgs(
            userId = UserId("other", "remote.example"),
        ).toOtherUserProfileRoute(
            sessionId = WireSessionId("self", "wire.example"),
            entryId = WireNavEntryId("other-profile-entry"),
        )

        assertEquals(
            route,
            Json.decodeFromString<OtherUserProfileRoute>(Json.encodeToString(route)),
        )
        assertEquals(
            OtherUserProfileViewModelArgs(
                targetUserId = route.targetUserId,
                groupConversationId = null,
            ),
            route.toViewModelArgs(),
        )
    }

    @Test
    fun givenLegacyIgnoredRequestResult_whenMappingToNavigationAndBack_thenValueIsUnchanged() {
        val result = "other-user".toConnectionRequestIgnoredResult()

        assertEquals("other-user", result.toLegacyResult())
        assertEquals("other-user", result.userName)
        assertEquals(
            "user-profile.connection-request-ignored",
            ConnectionRequestIgnoredResultContract.id.value,
        )
    }

    @Test
    fun givenIgnoredRequestUserName_whenSerialized_thenPersistedResultRemainsCompatible() {
        val result = ConnectionRequestIgnoredResult("Alice")

        assertEquals("""{"userId":"Alice"}""", Json.encodeToString(result))
        assertEquals(result, Json.decodeFromString<ConnectionRequestIgnoredResult>("""{"userId":"Alice"}"""))
    }

    @Test
    fun givenBlankIgnoredRequestUserName_whenCreatingResult_thenItIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionRequestIgnoredResult("")
        }
    }
}
