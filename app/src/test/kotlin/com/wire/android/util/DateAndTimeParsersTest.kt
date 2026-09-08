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

import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Locale
import java.util.TimeZone

class DateAndTimeParsersTest {

    @Test
    fun givenInstant_whenDeviceDateTimeIsFormatted_thenReturnLongDateAndShortTime() {
        val result = TEST_INSTANT.deviceDateTimeFormat()

        assertEquals("March 24, 2022, 6:02 PM", result.normalizeSpaces())
    }

    @Test
    fun givenInstant_whenMediumDateTimeIsFormatted_thenReturnMediumDateAndTime() {
        val result = TEST_INSTANT.formatMediumDateTime()

        assertEquals("Mar 24, 2022, 6:02:30 PM", result.normalizeSpaces())
    }

    @Test
    fun givenInstant_whenFullDateShortTimeIsFormatted_thenReturnFullDateAndShortTime() {
        val result = TEST_INSTANT.formatFullDateShortTime()

        assertEquals("Thursday, March 24, 2022, 6:02 PM", result.normalizeSpaces())
    }

    @Test
    fun givenInstant_whenMessageTimeIsFormatted_thenReturnShortTime() {
        val result = TEST_INSTANT.uiMessageDateTime()

        assertEquals("6:02 PM", result.normalizeSpaces())
    }

    @Test
    fun givenInstant_whenFileDateTimeIsFormatted_thenReturnFileSafeTimestamp() {
        assertEquals("2022-03-24-06-02-30", TEST_INSTANT.fileDateTime())
    }

    @Test
    fun givenInstant_whenReadReceiptDateTimeIsFormatted_thenReturnReadReceiptTimestamp() {
        val result = TEST_INSTANT.uiReadReceiptDateTime()

        assertEquals("Mar 24 2022,  06:02 PM", result.normalizeSpaces())
    }

    private fun String.normalizeSpaces(): String = replace('\u202f', ' ')

    companion object {
        private val TEST_INSTANT = Instant.parse("2022-03-24T18:02:30.360Z")
        private var systemDefaultLocale: Locale? = null
        private var systemDefaultTimeZone: TimeZone? = null

        @JvmStatic
        @BeforeAll
        fun setup() {
            systemDefaultTimeZone = TimeZone.getDefault()
            systemDefaultLocale = Locale.getDefault()
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            Locale.setDefault(Locale.US)
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            TimeZone.setDefault(systemDefaultTimeZone!!)
            Locale.setDefault(systemDefaultLocale!!)
        }
    }
}
