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

package com.wire.android.util

import android.app.Application
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class AdminlessReminderDateFormatTest {
    private val originalLocale = Locale.getDefault()
    private val originalTimeZone = TimeZone.getDefault()
    private val deletionTime = Instant.parse("2026-08-30T23:15:00Z")

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun given24HourPreference_whenFormattingReminder_thenUses24HourTime() {
        assertEquals("August 30, 23:15", deletionTime.formatMonthDayShortTime(is24Hour = true))
    }

    @Test
    fun given12HourPreference_whenFormattingReminder_thenUsesAmPm() {
        val formatted = deletionTime.formatMonthDayShortTime(is24Hour = false).replace('\u202f', ' ')
        assertEquals("August 30, 11:15 PM", formatted)
    }

    @Test
    fun givenGermanLocaleAndBerlinTimeZone_whenFormattingReminder_thenUsesLocalDateAndTime() {
        Locale.setDefault(Locale.GERMANY)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        assertEquals("31. August, 01:15", deletionTime.formatMonthDayShortTime(is24Hour = true))
    }
}
