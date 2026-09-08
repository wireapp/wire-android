/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.avatarpicker

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AvatarPickerNavigation3Test {

    @Test
    fun givenAvatarPickerRoute_whenSerializedAndRestored_thenScopeAndIdentityArePreserved() {
        val route = AvatarPickerRoute(
            sessionId = WireSessionId("user", "wire.example"),
            entryId = WireNavEntryId("avatar-entry"),
        )

        val restored = Json.decodeFromString<AvatarPickerRoute>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals("app/avatar_picker_screen", restored.routeId)
    }

    @Test
    fun givenSameArguments_whenCreatingTwoAvatarEntries_thenIdentityIsDifferent() {
        val sessionId = WireSessionId("user", "wire.example")

        assertNotEquals(
            AvatarPickerRoute(sessionId).entryId,
            AvatarPickerRoute(sessionId).entryId,
        )
    }

    @Test
    fun givenLegacyNullableResult_whenMappingToNavigationAndBack_thenNullAndValueArePreserved() {
        assertNull(null.toAvatarPickerResult().toLegacyResult())
        assertEquals(
            "asset-id",
            "asset-id".toAvatarPickerResult().toLegacyResult(),
        )
        assertEquals("user-profile.avatar-picker", AvatarPickerResultContract.id.value)
    }

    @Test
    fun givenSuccessfulNullAvatarResult_whenComparedWithCancellation_thenTheyRemainDistinct() {
        val successfulNull = com.wire.navigation.WireNavResult.Value(
            AvatarPickerResult(assetId = null)
        )

        assertNotEquals(successfulNull, com.wire.navigation.WireNavResult.Canceled)
        assertNull(successfulNull.value.assetId)
    }
}
