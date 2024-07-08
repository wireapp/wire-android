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

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.Date

class DateTimeUtilKtTest {

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

        @Test
        fun `return MessageDateTime_Now when a valid date is calling groupedUIMessageDateTime`() {
            val result = "2024-01-20T07:00:00.000Z".groupedUIMessageDateTime(getDummyCalendar().timeInMillis)
            assertEquals(MessageDateTimeGroup.Now, result)
        }

        @Test
        fun `return MessageDateTime_Within30Minutes when a valid date within 10 minutes is calling groupedUIMessageDateTime`() {
            val result = "2024-01-20T07:00:00.000Z".groupedUIMessageDateTime(
                getDummyCalendar().apply {
                    add(Calendar.MINUTE, 10)
                }.timeInMillis
            )
            assertEquals(MessageDateTimeGroup.Within30Minutes, result)
        }

        @Test
        fun `return MessageDateTime_Today when a valid date over 30 minutes is calling groupedUIMessageDateTime`() {
            val result = "2024-01-20T07:00:00.000Z".groupedUIMessageDateTime(
                getDummyCalendar().apply {
                    add(Calendar.MINUTE, 31)
                }.timeInMillis
            )
            assertEquals(
                MessageDateTimeGroup.Daily.Type.Today,
                (result as MessageDateTimeGroup.Daily).type
            )
            assertEquals(
                "2024-01-20",
                result.date.toString()
            )
        }

        @Test
        fun `return MessageDateTime_Yesterday when a valid date over 1 day is calling groupedUIMessageDateTime`() {
            val result = "2024-01-20T07:00:00.000Z".groupedUIMessageDateTime(
                getDummyCalendar().apply {
                    add(Calendar.DATE, 1)
                }.timeInMillis
            )

            assertEquals(
                MessageDateTimeGroup.Daily.Type.Yesterday,
                (result as MessageDateTimeGroup.Daily).type
            )
        }

        @Test
        fun `return MessageDateTime_WithinWeek when a valid date within 7 days 1 day is calling groupedUIMessageDateTime`() {
            val result = "2024-01-20T07:00:00.000Z".groupedUIMessageDateTime(
                getDummyCalendar().apply {
                    add(Calendar.DATE, 3)
                }.timeInMillis
            )

            assertEquals(
                MessageDateTimeGroup.Daily.Type.WithinWeek,
                (result as MessageDateTimeGroup.Daily).type
            )
        }

        @Test
        fun `return MessageDateTime_NotWithinWeekButSameYear when a valid date over 7 days and same year is calling groupedUIMessageDateTime`() {
            val result = "2024-01-20T07:00:00.000Z".groupedUIMessageDateTime(
                getDummyCalendar().apply {
                    add(Calendar.DATE, 10)
                }.timeInMillis
            )
            assertEquals(
                MessageDateTimeGroup.Daily.Type.NotWithinWeekButSameYear,
                (result as MessageDateTimeGroup.Daily).type
            )
        }

        @Test
        fun `return MessageDateTime_Other given valid date, when a valid date and different year is calling groupedUIMessageDateTime`() {
            val result = "2024-01-20T07:00:00.000Z".groupedUIMessageDateTime(
                getDummyCalendar().apply {
                    set(Calendar.YEAR, 2025)
                }.timeInMillis
            )
            assertEquals(
                MessageDateTimeGroup.Daily.Type.Other,
                (result as MessageDateTimeGroup.Daily).type
            )
        }

        private fun getDummyCalendar(): Calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.HOUR, 7)
            set(Calendar.AM_PM, Calendar.AM)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 20)
            set(Calendar.YEAR, 2024)
        }
    }


    @Nested
    @DisplayName("DateAndTimeParser for retro compatibility Should")
    inner class DateTimeFormatters {

        private val baseDateString = "2024-01-20T07:00:00.000Z"
        private val baseInstant = Clock.System.now()
        private val baseDate = Date()

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
        fun `return the same MediumOnlyDateTime format result, when calling date with new DateTimeFormatter format`() {
            assertEquals(baseDate.toMediumOnlyDateTime(), baseDate.toMediumOnlyDateTimeOld())
        }
    }
}
