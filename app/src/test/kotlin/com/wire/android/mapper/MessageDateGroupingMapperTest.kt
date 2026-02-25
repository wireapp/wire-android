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
package com.wire.android.mapper

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MessageDateGroupingMapperTest {

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
    fun `return MessageDateTime_NotWithinWeekButSameYear when date over 7 days and same year is calling groupedUIMessageDateTime`() {
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

    @Test
    fun `given messages on different calendar days within less than 24h then should display divider`() {
        val shouldDisplayDivider = "2024-01-20T23:50:00.000Z"
            .shouldDisplayDatesDifferenceDivider("2024-01-21T00:10:00.000Z")

        assertEquals(true, shouldDisplayDivider)
    }

    @Test
    fun `given messages on same calendar day then should not display divider`() {
        val shouldDisplayDivider = "2024-01-20T07:00:00.000Z"
            .shouldDisplayDatesDifferenceDivider("2024-01-20T21:00:00.000Z")

        assertEquals(false, shouldDisplayDivider)
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
