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

import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString
import kotlinx.datetime.Clock
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.measureTime

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

        println("The duration of using ServerDateOld/SimpleDateFormat was: $duration1")
        println("The duration of using ServerDate/LocalDateTimeFormat was: $duration2")
        assertTrue(duration1 > duration2)
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

        println("The duration of using TextApi/DateFormat was: $duration1")
        println("The duration of using TimeApi/DateTimeFormatter was: $duration2")
        assertTrue(duration1 > duration2)
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

        println("The duration of using TextApi/DateFormat was: $duration1")
        println("The duration of using TimeApi/DateTimeFormatter was: $duration2")
        assertTrue(duration1 > duration2)
    }


    companion object {
        const val ITERATIONS = 1_000_000
    }
}
