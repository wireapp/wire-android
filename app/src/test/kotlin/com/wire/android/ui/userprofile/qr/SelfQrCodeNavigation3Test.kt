/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.qr

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SelfQrCodeNavigation3Test {

    @Test
    fun givenLegacyQrArgs_whenMappingToNavigationAndBack_thenValueIsUnchanged() {
        val legacy = SelfQrCodeNavArgs(
            userHandle = "alice",
            isTeamMember = true,
        )

        val route = legacy.toSelfQrCodeRoute(
            sessionId = WireSessionId("user", "wire.example"),
            entryId = WireNavEntryId("qr-entry"),
        )

        assertEquals(legacy, route.toLegacyNavArgs())
        assertEquals("app/self_q_r_code_screen", route.routeId)
    }

    @Test
    fun givenQrRoute_whenSerializedAndRestored_thenArgumentsAndIdentityArePreserved() {
        val route = SelfQrCodeRoute(
            sessionId = WireSessionId("user", "wire.example"),
            userHandle = "alice",
            isTeamMember = false,
            entryId = WireNavEntryId("qr-entry"),
        )

        assertEquals(
            route,
            Json.decodeFromString<SelfQrCodeRoute>(Json.encodeToString(route)),
        )
        assertEquals(
            SelfQrCodeViewModelArgs("alice", false),
            route.toViewModelArgs(),
        )
    }
}
