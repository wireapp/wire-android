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
import java.util.Calendar

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

    @Test
    fun `given valid date, when transforming to ui message date time, then return MessageDateTime_Now`() {
        val result = "2024-01-20T07:00:00.000Z".uiMessageDateTime(getDummyCalendar().timeInMillis)
        assertEquals(MessageDateTime.Now, result)
    }

    @Test
    fun `given valid date, when transforming to ui message date time, then return MessageDateTime_Within30Minutes`() {
        val result = "2024-01-20T07:00:00.000Z".uiMessageDateTime(
            getDummyCalendar().apply {
                add(Calendar.MINUTE, 10)
            }.timeInMillis
        )
        assertEquals(MessageDateTime.Within30Minutes(10), result)
    }

    @Test
    fun `given valid date, when transforming to ui message date time, then return MessageDateTime_Today`() {
        val result = "2024-01-20T07:00:00.000Z".uiMessageDateTime(
            getDummyCalendar().apply {
                add(Calendar.MINUTE, 31)
            }.timeInMillis
        )
        assertEquals(MessageDateTime.Today("07:00"), result)
    }

    @Test
    fun `given valid date, when transforming to ui message date time, then return MessageDateTime_Yesterday`() {
        val result = "2024-01-20T07:00:00.000Z".uiMessageDateTime(
            getDummyCalendar().apply {
                add(Calendar.DATE, 1)
            }.timeInMillis
        )
        assertEquals(MessageDateTime.Yesterday("07:00"), result)
    }

    @Test
    fun `given valid date, when transforming to ui message date time, then return MessageDateTime_WithinWeek`() {
        val result = "2024-01-20T07:00:00.000Z".uiMessageDateTime(
            getDummyCalendar().apply {
                add(Calendar.DATE, 3)
            }.timeInMillis
        )
        assertEquals(MessageDateTime.WithinWeek("Saturday Jan 20, 07:00 am"), result)
    }

    @Test
    fun `given valid date, when transforming to ui message date time, then return MessageDateTime_NotWithinWeekButSameYear`() {
        val result = "2024-01-20T07:00:00.000Z".uiMessageDateTime(
            getDummyCalendar().apply {
                add(Calendar.DATE, 10)
            }.timeInMillis
        )
        assertEquals(MessageDateTime.NotWithinWeekButSameYear("Jan 20, 07:00 am"), result)
    }

    @Test
    fun `given valid date, when transforming to ui message date time, then return MessageDateTime_Other`() {
        val result = "2024-01-20T07:00:00.000Z".uiMessageDateTime(
            getDummyCalendar().apply {
                set(Calendar.YEAR, 2025)
            }.timeInMillis
        )
        assertEquals(MessageDateTime.Other("Jan 20 2024, 07:00 am"), result)
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
