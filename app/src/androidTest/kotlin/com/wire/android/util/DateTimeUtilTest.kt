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

import android.os.Debug
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.measureTime

private data class RuntimeStats(
    val allocatedBytes: Long,
    val freedBytes: Long,
    val gcCount: Long,
    val gcTimeMs: Long,
    val blockingGcCount: Long,
    val blockingGcTimeMs: Long
) {
    operator fun minus(other: RuntimeStats) = RuntimeStats(
        allocatedBytes = allocatedBytes - other.allocatedBytes,
        freedBytes = freedBytes - other.freedBytes,
        gcCount = gcCount - other.gcCount,
        gcTimeMs = gcTimeMs - other.gcTimeMs,
        blockingGcCount = blockingGcCount - other.blockingGcCount,
        blockingGcTimeMs = blockingGcTimeMs - other.blockingGcTimeMs
    )

    companion object {
        fun capture(): RuntimeStats {
            val stats = Debug.getRuntimeStats()
            return RuntimeStats(
                allocatedBytes = stats.getValue("art.gc.bytes-allocated").toLong(),
                freedBytes = stats.getValue("art.gc.bytes-freed").toLong(),
                gcCount = stats.getValue("art.gc.gc-count").toLong(),
                gcTimeMs = stats.getValue("art.gc.gc-time").toLong(),
                blockingGcCount = stats.getValue("art.gc.blocking-gc-count").toLong(),
                blockingGcTimeMs = stats.getValue("art.gc.blocking-gc-time").toLong()
            )
        }
    }
}

private data class FormatterMeasurement(
    val duration: Duration,
    val runtimeStats: RuntimeStats
) {
    fun summary(iterations: Int): String =
        "$duration; allocated=${runtimeStats.allocatedBytes} B " +
            "(${runtimeStats.allocatedBytes / iterations} B/op); " +
            "freed=${runtimeStats.freedBytes} B; " +
            "GC=${runtimeStats.gcCount}/${runtimeStats.gcTimeMs} ms; " +
            "blocking GC=${runtimeStats.blockingGcCount}/${runtimeStats.blockingGcTimeMs} ms"
}

/**
 * Compares the String-based formatting paths used in production before the Instant migration
 * with the current typed paths. The former implementations live only in this test so the
 * deprecated String APIs do not need to remain in production code.
 *
 * This benchmark is intentionally opt-in because its results are device-sensitive and it forces
 * garbage collection between measurements. Enable it with the instrumentation argument
 * `runDateTimeBenchmark=true`.
 */
@RunWith(AndroidJUnit4::class)
class DateTimeUtilTest {

    private val productionDateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC"))
    private val productionLongDateShortTimeFormat =
        java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.LONG,
            java.text.DateFormat.SHORT,
            Locale.getDefault()
        ).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
    private val productionMediumDateTimeFormat =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
            .withLocale(Locale.getDefault())
    private val productionFullDateShortTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .withLocale(Locale.getDefault())
    private val productionMessageTimeFormatter =
        java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getDefault()
        }

    @Volatile
    private var benchmarkSink = 0

    @Before
    fun requireExplicitOptIn() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(
            "Manual date benchmark skipped; pass $RUN_BENCHMARK_ARGUMENT=true to run it",
            arguments.getString(RUN_BENCHMARK_ARGUMENT).toBoolean()
        )
    }

    @Test
    fun givenServerDate_whenFormattingDeviceDate_thenCompareProductionStringAndTypedInstantPerformance() {
        compareFormatters(
            label = "Device date (java.text.DateFormat)",
            productionStringPath = { productionDeviceDateTimeFormat(SERVER_DATE) },
            typedInstantPath = { SERVER_INSTANT.deviceDateTimeFormat() }
        )
    }

    @Test
    fun givenServerDate_whenFormattingMediumDate_thenCompareProductionStringAndTypedInstantPerformance() {
        compareFormatters(
            label = "Medium date (java.time.DateTimeFormatter)",
            productionStringPath = { productionFormatMediumDateTime(SERVER_DATE) },
            typedInstantPath = { SERVER_INSTANT.formatMediumDateTime() }
        )
    }

    @Test
    fun givenServerDate_whenFormattingFullDate_thenCompareProductionStringAndTypedInstantPerformance() {
        compareFormatters(
            label = "Full date (java.time.DateTimeFormatter)",
            productionStringPath = { productionFormatFullDateShortTime(SERVER_DATE) },
            typedInstantPath = { SERVER_INSTANT.formatFullDateShortTime() }
        )
    }

    @Test
    fun givenServerDate_whenFormattingMessageTime_thenCompareProductionStringAndTypedInstantPerformance() {
        compareFormatters(
            label = "Message time (java.text.DateFormat)",
            productionStringPath = { productionUiMessageDateTime(SERVER_DATE) },
            typedInstantPath = { SERVER_INSTANT.uiMessageDateTime() }
        )
    }

    private fun productionServerDate(stringDate: String): Date =
        Date(
            LocalDateTime.parse(stringDate, productionDateTimeFormatter)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )

    private fun productionDeviceDateTimeFormat(stringDate: String): String =
        productionLongDateShortTimeFormat.format(Date.from(java.time.Instant.parse(stringDate)))

    private fun productionFormatMediumDateTime(stringDate: String): String =
        productionMediumDateTimeFormat.format(productionServerDate(stringDate).toInstant())

    private fun productionFormatFullDateShortTime(stringDate: String): String =
        productionFullDateShortTimeFormatter.format(productionServerDate(stringDate).toInstant())

    private fun productionUiMessageDateTime(stringDate: String): String =
        productionMessageTimeFormatter.format(Date.from(java.time.Instant.parse(stringDate)))

    private fun compareFormatters(
        label: String,
        productionStringPath: () -> String,
        typedInstantPath: () -> String
    ) {
        assertEquals(productionStringPath(), typedInstantPath())

        repeat(WARM_UP_ITERATIONS) {
            productionStringPath()
            typedInstantPath()
        }

        val productionDuration = measure(productionStringPath)
        val typedDuration = measure(typedInstantPath)
        val speedup = productionDuration.duration.inWholeNanoseconds.toDouble() /
            typedDuration.duration.inWholeNanoseconds

        Log.d(
            TAG,
            "$label — production String path: ${productionDuration.summary(ITERATIONS)}; " +
                "typed Instant path: ${typedDuration.summary(ITERATIONS)}; " +
                "Instant speedup: ${"%.2f".format(Locale.ROOT, speedup)}x; " +
                "sink: $benchmarkSink"
        )
    }

    @Suppress("ExplicitGarbageCollectionCall")
    private fun measure(formatter: () -> String): FormatterMeasurement {
        // Test-only setup: start each path from comparable heap pressure before capturing counters.
        Runtime.getRuntime().gc()
        val initialStats = RuntimeStats.capture()
        var checksum = 0
        val duration = measureTime {
            repeat(ITERATIONS) {
                checksum = checksum xor formatter().hashCode()
            }
        }
        val runtimeStats = RuntimeStats.capture() - initialStats
        benchmarkSink = benchmarkSink xor checksum
        return FormatterMeasurement(duration, runtimeStats)
    }

    private companion object {
        const val TAG = "DateTimeParsersTest"
        const val ITERATIONS = 800_000
        const val WARM_UP_ITERATIONS = ITERATIONS / 2
        const val RUN_BENCHMARK_ARGUMENT = "runDateTimeBenchmark"
        const val SERVER_DATE = "2026-07-23T12:34:56.789Z"
        val SERVER_INSTANT = Instant.parse(SERVER_DATE)
    }
}
