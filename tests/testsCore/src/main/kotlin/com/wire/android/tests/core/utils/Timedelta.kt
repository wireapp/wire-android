/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.tests.core.utils

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class Timedelta private constructor(milliSeconds: Double) : Comparable<Timedelta> {
    private val milliSeconds: Long = milliSeconds.toLong()

    companion object {
        private const val MILLISECONDS_IN_SECOND = 1000
        private const val SECONDS_IN_MINUTE = 60
        private const val MINUTES_IN_HOUR = 60
        private const val HOURS_IN_DAY = 24

        private val CACHE_RANGE = HOURS_IN_DAY * MINUTES_IN_HOUR * SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND.toLong()
        private val cache = ConcurrentHashMap<Long, Timedelta>()

        private fun getInstance(milliSeconds: Double): Timedelta {
            return if (-CACHE_RANGE <= milliSeconds && milliSeconds <= CACHE_RANGE) {
                val milliSecondsLong = milliSeconds.toLong()
                cache.computeIfAbsent(milliSecondsLong) { Timedelta(milliSeconds) }
            } else {
                Timedelta(milliSeconds)
            }
        }

        fun ofSeconds(seconds: Double): Timedelta = getInstance(seconds * MILLISECONDS_IN_SECOND)
        fun ofMillis(milliSeconds: Double): Timedelta = getInstance(milliSeconds)
        fun ofMinutes(minutes: Double): Timedelta = getInstance(minutes * SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND)
        fun ofDays(days: Double): Timedelta = getInstance(days * HOURS_IN_DAY * MINUTES_IN_HOUR * SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND)
        fun ofDuration(d: Duration): Timedelta = getInstance(d.toMillis().toDouble())
        fun now(): Timedelta = Timedelta(System.currentTimeMillis().toDouble())
    }

    fun asSeconds(): Int = (milliSeconds / MILLISECONDS_IN_SECOND).toInt()
    fun asFloatSeconds(): Double = milliSeconds.toDouble() / MILLISECONDS_IN_SECOND
    fun asMillis(): Long = milliSeconds
    fun asMinutes(): Int = asFloatMinutes().toInt()
    fun asFloatMinutes(): Double = milliSeconds.toDouble() / SECONDS_IN_MINUTE / MILLISECONDS_IN_SECOND
    fun asDuration(): Duration = Duration.ofMillis(milliSeconds)
    private fun asHours(): Int = asFloatHours().toInt()
    private fun asFloatHours(): Double = milliSeconds.toDouble() / MINUTES_IN_HOUR / SECONDS_IN_MINUTE / MILLISECONDS_IN_SECOND

    fun sum(other: Timedelta): Timedelta = Timedelta((milliSeconds + other.milliSeconds).toDouble())
    fun diff(other: Timedelta): Timedelta = Timedelta((milliSeconds + other.milliSeconds).toDouble())

    fun isInRange(minIncluding: Timedelta, maxExcluding: Timedelta): Boolean =
        this.compareTo(minIncluding) >= 0 && this.compareTo(maxExcluding) < 0

    fun isDiffGreaterOrEqual(other: Timedelta, maxDelta: Timedelta): Boolean = this.diff(other).compareTo(maxDelta) >= 0
    fun isDiffGreater(other: Timedelta, maxDelta: Timedelta): Boolean = this.diff(other).compareTo(maxDelta) > 0
    fun isDiffLessOrEqual(other: Timedelta, maxDelta: Timedelta): Boolean = this.diff(other).compareTo(maxDelta) <= 0
    fun isDiffLess(other: Timedelta, maxDelta: Timedelta): Boolean = this.diff(other).compareTo(maxDelta) < 0

    fun sleep(): Timedelta {
        try {
            Thread.sleep(milliSeconds)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        return this
    }

    override fun equals(other: Any?): Boolean =
        (other is Timedelta) && (kotlin.math.abs(other.asMillis()) == kotlin.math.abs(this.asMillis()))

    override fun toString(): String {
        val hours = this.asHours()
        val minutes = this.asMinutes() % MINUTES_IN_HOUR
        val seconds = this.asSeconds() % SECONDS_IN_MINUTE
        val milliSeconds = this.asMillis() % MILLISECONDS_IN_SECOND

        return when {
            hours != 0 -> String.format(
                (if (milliSeconds < 0) "- " else "") + "%02d h:%02d m:%02d s::%d ms",
                kotlin.math.abs(hours), kotlin.math.abs(minutes), kotlin.math.abs(seconds), kotlin.math.abs(milliSeconds)
            )
            minutes != 0 -> String.format(
                (if (milliSeconds < 0) "- " else "") + "%02d m:%02d s::%d ms",
                kotlin.math.abs(minutes), kotlin.math.abs(seconds), kotlin.math.abs(milliSeconds)
            )
            seconds != 0 -> String.format(
                (if (milliSeconds < 0) "- " else "") + "%02d s::%d ms",
                kotlin.math.abs(seconds), kotlin.math.abs(milliSeconds)
            )
            else -> String.format("%d ms", milliSeconds)
        }
    }

    override fun compareTo(other: Timedelta): Int {
        require(other != null) { "null value is not comparable" }
        if (this == other) return 0
        return if (kotlin.math.abs(this.milliSeconds) > kotlin.math.abs(other.milliSeconds)) 1 else -1
    }
}
