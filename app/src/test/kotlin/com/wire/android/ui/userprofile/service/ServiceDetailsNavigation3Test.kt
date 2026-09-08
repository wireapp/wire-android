/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.service

import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ServiceDetailsNavigation3Test {

    @Test
    fun givenLegacyAppArgs_whenMappingToNavigationAndBack_thenValueIsUnchanged() {
        assertLegacyRoundTrip(
            ServiceDetailsNavArgs(
                conversationId = ConversationId("conversation", "wire.example"),
                id = ServiceDetailsNavArgs.Id.AppId(UserId("app", "provider.example")),
            )
        )
    }

    @Test
    fun givenLegacyBotArgs_whenMappingToNavigationAndBack_thenValueIsUnchanged() {
        assertLegacyRoundTrip(
            ServiceDetailsNavArgs(
                conversationId = null,
                id = ServiceDetailsNavArgs.Id.BotServiceId(
                    BotService("bot", "provider.example")
                ),
            )
        )
    }

    @Test
    fun givenServiceRoute_whenSerializedAndRestored_thenTargetAndIdentityArePreserved() {
        val route = ServiceDetailsRoute(
            sessionId = WireSessionId("user", "wire.example"),
            conversationId = null,
            target = ServiceProfileTarget.Bot(
                com.wire.android.ui.userprofile.UserProfileQualifiedId(
                    "bot",
                    "provider.example",
                )
            ),
            entryId = WireNavEntryId("service-entry"),
        )

        val restored = Json.decodeFromString<ServiceDetailsRoute>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals("app/service_details_screen", restored.routeId)
        assertEquals(
            ServiceDetailsViewModelArgs(
                conversationId = route.conversationId,
                target = route.target,
            ),
            route.toViewModelArgs(),
        )
    }

    private fun assertLegacyRoundTrip(legacy: ServiceDetailsNavArgs) {
        val route = legacy.toServiceDetailsRoute(
            sessionId = WireSessionId("user", "wire.example"),
            entryId = WireNavEntryId("service-entry"),
        )

        assertEquals(legacy, route.toLegacyNavArgs())
    }
}
