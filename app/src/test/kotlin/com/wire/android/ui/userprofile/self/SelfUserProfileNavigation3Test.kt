/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.self

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SelfUserProfileNavigation3Test {

    @Test
    fun givenSelfProfileRoute_whenSerializedAndRestored_thenScopeAndIdentityArePreserved() {
        val route = SelfUserProfileRoute(
            sessionId = WireSessionId("user", "wire.example"),
            entryId = WireNavEntryId("self-profile-entry"),
        )

        assertEquals(
            route,
            Json.decodeFromString<SelfUserProfileRoute>(Json.encodeToString(route)),
        )
        assertEquals("app/self_user_profile_screen", route.routeId)
    }

    @Test
    fun givenSameSession_whenCreatingTwoSelfProfileEntries_thenIdentityIsDifferent() {
        val sessionId = WireSessionId("user", "wire.example")

        assertNotEquals(
            SelfUserProfileRoute(sessionId).entryId,
            SelfUserProfileRoute(sessionId).entryId,
        )
    }
}
