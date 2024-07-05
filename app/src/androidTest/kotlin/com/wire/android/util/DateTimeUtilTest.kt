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
import org.junit.Test
import kotlin.time.measureTime

class DateTimeUtilTest {

    @Test
    fun givenDates_OutputPerformanceOfFormattersInDevice() {
        // warmup
        val date = Clock.System.now().toIsoDateTimeString()
        repeat(ITERATIONS / 2) {
            date.serverDateOld()
            date.serverDate()
        }

        // simple date format
        val duration1 = measureTime {
            repeat(ITERATIONS) {
                date.serverDate()
            }
        }

        // datetime format
        val duration2 = measureTime {
            repeat(ITERATIONS) {
                date.serverDateOld()
            }
        }

        println("The duration of using ServerDateOld/SimpleDateFormat was: $duration1")
        println("The duration of using ServerDate/Instant was: $duration2")
    }

    companion object {
        const val ITERATIONS = 1_000_000
    }
}
