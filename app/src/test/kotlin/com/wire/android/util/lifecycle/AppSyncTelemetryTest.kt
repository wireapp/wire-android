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
package com.wire.android.util.lifecycle

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppSyncTelemetryTest {

    @Test
    fun givenAppSyncTelemetry_whenLogging_thenShouldWriteStableStructuredFieldsAndOmitNulls() {
        val writer = RecordingLogWriter()
        val logger = KaliumLogger(
            config = KaliumLogger.Config(
                initialLevel = KaliumLogLevel.DEBUG,
                initialLogWriterList = listOf(writer),
            ),
            tag = "AppSyncTelemetryTest",
        )

        logger.logAppSyncTelemetry(
            event = AppSyncTelemetryEvent.APP_SYNC_REQUEST_STARTED,
            data = mapOf(
                "trigger" to AppSyncTelemetryTrigger.APP_FOREGROUND.name,
                "failureType" to null,
            )
        )

        val entry = writer.entries.single()
        assertEquals(Severity.Info, entry.severity)
        assertTrue(entry.message.contains("Sync telemetry:"))
        assertTrue(entry.message.contains("\"schemaVersion\":1"))
        assertTrue(entry.message.contains("\"event\":\"APP_SYNC_REQUEST_STARTED\""))
        assertTrue(entry.message.contains("\"component\":\"APP_LIFECYCLE\""))
        assertTrue(entry.message.contains("\"trigger\":\"APP_FOREGROUND\""))
        assertFalse(entry.message.contains("failureType"))
    }

    private class RecordingLogWriter : LogWriter() {
        val entries = mutableListOf<Entry>()

        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            entries += Entry(severity, message)
        }
    }

    private data class Entry(
        val severity: Severity,
        val message: String,
    )
}
