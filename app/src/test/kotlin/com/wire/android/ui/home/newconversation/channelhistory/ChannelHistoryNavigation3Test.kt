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

package com.wire.android.ui.home.newconversation.channelhistory

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ChannelHistoryNavigation3Test {

    @Test
    fun givenChannelHistoryCustomResultContract_whenAddressed_thenItsIdIsStable() {
        assertEquals(
            "new-conversation.channel-history.custom",
            ChannelHistoryCustomResultContract.id.value,
        )
    }

    @Test
    fun givenChannelHistoryPilotContribution_whenReadByHost_thenResultTypeIsRegisteredOnce() {
        assertEquals(
            listOf(ChannelHistoryCustomResultType),
            ChannelHistoryNavigation3Pilot.resultTypes,
        )
    }

    @Test
    fun givenPositiveCustomResult_whenSerializedAndRestored_thenValueIsPreserved() {
        val result = ChannelHistoryCustomResult(
            ChannelHistorySelection.Specific(9, ChannelHistorySelection.AmountUnit.WEEKS)
        )

        assertEquals(
            result,
            Json.decodeFromString<ChannelHistoryCustomResult>(Json.encodeToString(result)),
        )
    }

    @Test
    fun givenCustomRoute_whenSerializedAndRestored_thenTypedArgumentsAndIdentityArePreserved() {
        val route = ChannelHistoryCustomRoute(
            sessionId = WireSessionId("user", "wire.example"),
            flowId = "new-conversation-42",
            currentType = ChannelHistorySelection.Specific(
                amount = 6,
                unit = ChannelHistorySelection.AmountUnit.MONTHS,
            ),
            entryId = WireNavEntryId("channel-history-custom-entry"),
        )

        val restored = Json.decodeFromString<ChannelHistoryCustomRoute>(
            Json.encodeToString(route)
        )

        assertEquals(route, restored)
        assertEquals("user", restored.sessionId.value)
        assertEquals("wire.example", restored.sessionId.domain)
        assertEquals("new-conversation-42", restored.flowId)
        assertEquals(
            ChannelHistorySelection.Specific(6, ChannelHistorySelection.AmountUnit.MONTHS),
            restored.currentType,
        )
        assertEquals("channel-history-custom-entry", restored.entryId.value)
    }

    @Test
    fun givenSameRouteArguments_whenCreatingTwoEntries_thenTheirEntryIdentityIsDifferent() {
        val first = ChannelHistoryRoute(
            sessionId = WireSessionId("user", "wire.example"),
            flowId = "new-conversation-42",
        )
        val second = ChannelHistoryRoute(
            sessionId = WireSessionId("user", "wire.example"),
            flowId = "new-conversation-42",
        )

        assertNotEquals(first.entryId, second.entryId)
        assertEquals(first.routeId, second.routeId)
        assertEquals("app/channel_history_screen", first.routeId)
    }

    @Test
    fun givenEveryLegacyHistoryType_whenMappingToNavigationAndBack_thenValueIsUnchanged() {
        val values = listOf(
            ChannelHistoryType.Off,
            ChannelHistoryType.On.Unlimited,
            ChannelHistoryType.On.Specific(
                1,
                ChannelHistoryType.On.Specific.AmountType.Days,
            ),
            ChannelHistoryType.On.Specific(
                2,
                ChannelHistoryType.On.Specific.AmountType.Weeks,
            ),
            ChannelHistoryType.On.Specific(
                3,
                ChannelHistoryType.On.Specific.AmountType.Months,
            ),
        )

        values.forEach { value ->
            assertEquals(value, value.toSelection().toLegacy())
        }
    }

    @Test
    fun givenPositiveCustomAmount_whenMappingResult_thenSpecificHistoryTypeIsReturned() {
        assertEquals(
            ChannelHistoryType.On.Specific(
                amount = 12,
                type = ChannelHistoryType.On.Specific.AmountType.Weeks,
            ),
            channelHistorySpecificOrNull(
                amount = "12",
                type = ChannelHistoryType.On.Specific.AmountType.Weeks,
            ),
        )
    }

    @Test
    fun givenInvalidCustomAmount_whenMappingResult_thenCancellationValueIsReturned() {
        assertNull(
            channelHistorySpecificOrNull(
                amount = "",
                type = ChannelHistoryType.On.Specific.AmountType.Days,
            )
        )
        assertNull(
            channelHistorySpecificOrNull(
                amount = "0",
                type = ChannelHistoryType.On.Specific.AmountType.Days,
            )
        )
    }

    @Test
    fun givenNonPositiveAmount_whenCreatingTypedSelection_thenItIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ChannelHistorySelection.Specific(
                amount = 0,
                unit = ChannelHistorySelection.AmountUnit.DAYS,
            )
        }
    }
}
