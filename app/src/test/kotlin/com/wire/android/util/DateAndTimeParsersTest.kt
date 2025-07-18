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

import kotlinx.datetime.toKotlinInstant
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateAndTimeParsersTest {

    @Nested
    @DisplayName("DateAndTimeParser Should")
    inner class DateAndTimeParsers {
        @Test
        fun `return null when an invalid date`() {
            val result = "NOT_VALID".deviceDateTimeFormat()
            assertEquals(null, result)
        }

        @Test
        fun `return a medium format date, when a valid date`() {
            val result = "2022-03-24T18:02:30.360Z".deviceDateTimeFormat()
            assertEquals("March 24, 2022, 6:02 PM", result)
        }

        @Test
        fun `return a medium format, when formatMediumDateTime is called`() {
            val result = "2022-03-24T18:02:30.360Z".formatMediumDateTime()
            assertEquals("Mar 24, 2022, 6:02:30 PM", result)
        }
    }

    @Nested
    @DisplayName("DateAndTimeParser for retro compatibility Should")
    inner class DateTimeFormatters {

        private val baseDateString = "2020-01-20T07:00:00.000Z"
        private val baseDate = Date(
            Calendar.getInstance(
                TimeZone.getTimeZone("UTC")
            ).apply {
                set(2020, 0, 20, 7, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
        private val baseInstant = baseDate.toInstant().toKotlinInstant()

        @Test
        fun `return the same serverDate format result, when calling with new LocalDateTime format`() {
            assertEquals(serverDateOld(baseDateString), baseDateString.serverDate())
        }

        @Test
        fun `return the same deviceDate format result, when calling with new DateTimeFormatter format`() {
            assertEquals(baseDateString.deviceDateTimeFormat(), baseDateString.deviceDateTimeFormatOld())
        }

        @Test
        fun `return the same mediumDateTime format result, when calling with new DateTimeFormatter format`() {
            assertEquals(baseDateString.formatMediumDateTime(), baseDateString.formatMediumDateTimeOld())
        }

        @Test
        fun `return the same fullDateShortTime format result, when calling with new DateTimeFormatter format`() {
            assertEquals(baseDateString.formatFullDateShortTime(), baseDateString.formatFullDateShortTimeOld())
        }

        @Test
        fun `return the same fileDateTime format result, when calling with new DateTimeFormatter format`() {
            assertEquals(baseInstant.fileDateTime(), baseInstant.fileDateTimeOld())
        }

        @Test
        fun `return the same readReceiptDateTime format result, when calling instant with new DateTimeFormatter format`() {
            assertEquals(baseInstant.uiReadReceiptDateTime(), baseInstant.uiReadReceiptDateTimeOld())
        }

        @Test
        fun `return the same MessageDateTime format result, when calling date with new DateTimeFormatter format`() {
            assertEquals(baseDateString.uiMessageDateTime(), baseDateString.uiMessageDateTimeOld())
        }
    }

    companion object {
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
