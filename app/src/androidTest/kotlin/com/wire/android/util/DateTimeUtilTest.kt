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

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString
import kotlin.time.Clock
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.measureTime

@RunWith(AndroidJUnit4::class)
class DateTimeUtilTest {

    @Test
    fun givenDates_OutputPerformanceForServerDateFormattersInDevice() {
        // warmup
        val date = Clock.System.now().toIsoDateTimeString()
        repeat(ITERATIONS / 2) {
            serverDateOld(date)
            date.serverDate()
        }

        // simple date format
        val duration1 = measureTime {
            repeat(ITERATIONS) {
                serverDateOld(date)
            }
        }

        // datetime format
        val duration2 = measureTime {
            repeat(ITERATIONS) {
                date.serverDate()
            }
        }

        Log.d("DateTimeParsersTest", "The duration of using ServerDateOld/SimpleDateFormat was: $duration1")
        Log.d("DateTimeParsersTest", "The duration of using ServerDate/LocalDateTimeFormat was: $duration2")
    }

    @Test
    fun givenDates_OutputPerformanceForDeviceDateFormattersInDevice() {
        // warmup
        val date = Clock.System.now().toIsoDateTimeString()
        repeat(ITERATIONS / 2) {
            date.deviceDateTimeFormat()
            date.deviceDateTimeFormatOld()
        }

        // Old DateFormat from text api
        val duration1 = measureTime {
            repeat(ITERATIONS) {
                date.deviceDateTimeFormatOld()
            }
        }

        // New DateTimeFormatter from time api
        val duration2 = measureTime {
            repeat(ITERATIONS) {
                date.deviceDateTimeFormat()
            }
        }

        Log.d("DateTimeParsersTest", "The duration of using TextApi/DateFormat was: $duration1")
        Log.d("DateTimeParsersTest", "The duration of using TimeApi/DateTimeFormatter was: $duration2")
    }

    @Test
    fun givenDates_OutputPerformanceForMediumDateFormattersInDevice() {
        // warmup
        val date = Clock.System.now().toIsoDateTimeString()
        repeat(ITERATIONS / 2) {
            date.formatMediumDateTime()
            date.formatMediumDateTimeOld()
        }

        // Old DateFormat from text api
        val duration1 = measureTime {
            repeat(ITERATIONS) {
                date.formatMediumDateTimeOld()
            }
        }

        // New DateTimeFormatter from time api
        val duration2 = measureTime {
            repeat(ITERATIONS) {
                date.formatMediumDateTime()
            }
        }

        Log.d("DateTimeParsersTest", "The duration of using TextApi/DateFormat was: $duration1")
        Log.d("DateTimeParsersTest", "The duration of using TimeApi/DateTimeFormatter was: $duration2")
    }

    companion object {
        const val ITERATIONS = 800_000
    }
}
