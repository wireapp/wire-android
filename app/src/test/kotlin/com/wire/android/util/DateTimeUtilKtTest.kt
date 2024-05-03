/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DateTimeUtilKtTest {

    @Test
    fun `given a invalid date, when performing a transformation, then return null`() {
        val result = "NOT_VALID".deviceDateTimeFormat()
        assertEquals(null, result)
    }

    @Test
    fun `given a valid date, when performing a transformation for device, then return with medium format`() {
        val result = "2022-03-24T18:02:30.360Z".deviceDateTimeFormat()
        assertEquals("March 24, 2022, 6:02 PM", result)
    }

    @Test
    fun `given a valid date, when performing a transformation, then return with medium format`() {
        val result = "2022-03-24T18:02:30.360Z".formatMediumDateTime()
        assertEquals("Mar 24, 2022, 6:02:30 PM", result)
    }
}
