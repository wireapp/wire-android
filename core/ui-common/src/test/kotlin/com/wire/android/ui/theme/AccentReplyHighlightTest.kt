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
package com.wire.android.ui.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class AccentReplyHighlightTest {

    private val schemes = listOf(WireColorSchemeTypes.light, WireColorSchemeTypes.dark)

    @ParameterizedTest
    @EnumSource(Accent::class)
    fun givenAnyAccent_whenApplied_thenSelfBubbleReplyHighlightKeepsThemeStrength(accent: Accent) {
        schemes.forEach { scheme ->
            assertEquals(scheme.selfBubble.replyHighlight.alpha, scheme.withAccent(accent).selfBubble.replyHighlight.alpha)
        }
    }

    @ParameterizedTest
    @EnumSource(Accent::class, names = ["Blue", "Unknown"])
    fun givenBlueAccent_whenApplied_thenSelfBubbleReplyHighlightIsTheDefault(accent: Accent) {
        schemes.forEach { scheme ->
            assertEquals(scheme.selfBubble.replyHighlight, scheme.withAccent(accent).selfBubble.replyHighlight)
        }
    }

    @ParameterizedTest
    @EnumSource(Accent::class, names = ["Blue", "Unknown"], mode = EnumSource.Mode.EXCLUDE)
    fun givenNonBlueAccent_whenApplied_thenSelfBubbleReplyHighlightIsNotBlue(accent: Accent) {
        schemes.forEach { scheme ->
            assertNotEquals(scheme.selfBubble.replyHighlight, scheme.withAccent(accent).selfBubble.replyHighlight)
        }
    }
}
