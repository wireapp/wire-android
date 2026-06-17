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

package com.wire.android.ui.userprofile.self.status

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SelfUserStatusViewModelTest {

    @Test
    fun givenTextStatusWithEmoji_whenParsing_thenSplitsEmojiAndMessage() {
        val result = parseTextStatus("\uD83C\uDFDD\uFE0F On vacation")

        assertEquals("\uD83C\uDFDD\uFE0F", result.emoji)
        assertEquals("On vacation", result.message)
    }

    @Test
    fun givenTextStatusWithoutEmoji_whenParsing_thenUsesDefaultEmojiAndFullText() {
        val result = parseTextStatus("Working remotely")

        assertEquals(DEFAULT_STATUS_EMOJI, result.emoji)
        assertEquals("Working remotely", result.message)
    }

    @Test
    fun givenBlankTextStatus_whenParsing_thenUsesEmptyMessage() {
        val result = parseTextStatus(null)

        assertEquals(DEFAULT_STATUS_EMOJI, result.emoji)
        assertEquals("", result.message)
    }
}
