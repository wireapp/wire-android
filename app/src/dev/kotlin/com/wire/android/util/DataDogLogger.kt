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
 *
 *
 */

package com.wire.android.util

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.datadog.android.log.Logger
import com.wire.kalium.logger.KaliumLogger

object DataDogLogger : LogWriter() {

    private val logger = Logger.Builder()
        .setNetworkInfoEnabled(true)
        .setLogcatLogsEnabled(true)
        .setLogcatLogsEnabled(false) // we already use platformLogWriter() along with DataDogLogger, don't need duplicates in LogCat
        .setBundleWithTraceEnabled(true)
        .setLoggerName("DATADOG")
        .build()

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val attributes = KaliumLogger.UserClientData.getFromTag(tag)?.let { userClientData ->
            mapOf(
                "userId" to userClientData.userId,
                "clientId" to userClientData.clientId,
            )
        } ?: emptyMap<String, Any?>()
        logger.log(severity.ordinal, message, throwable, attributes)
    }
}
