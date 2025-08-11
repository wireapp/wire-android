/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.util.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StyledStringUtilTest {

    @Test
    fun `given a string with whitespaces, when adding markdown bold part, then style properly preserving whitespaces`() {
        val givenList = listOf(
            "  \t\n\r Text with some leading whitespaces",
            "Text with some trailing whitespaces \t\n\r  ",
            " \t\n\r Text with some leading and trailing whitespaces \t\n\r  ",
            "Text with no leading nor trailing whitespaces",
        )
        val expectedList = listOf(
            "  \t\n\r **Text with some leading whitespaces**",
            "**Text with some trailing whitespaces** \t\n\r  ",
            " \t\n\r **Text with some leading and trailing whitespaces** \t\n\r  ",
            "**Text with no leading nor trailing whitespaces**",
        )
        for ((given, expected) in givenList.zip(expectedList)) {
            val result = given.markdownBold()
            assertEquals(expected, result)
        }
    }
}
