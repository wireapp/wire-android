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
package com.wire.android

import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.util.serialization.toJsonElement

private var appLoggerConfig = KaliumLogger.Config.disabled()

// App wide global logger, carefully initialized when our application is "onCreate"
internal var appLogger = KaliumLogger.disabled()

object AppLogger {
    fun init(config: KaliumLogger.Config) {
        appLoggerConfig = config
        appLogger = KaliumLogger(config = config, tag = "WireAppLogger")
    }

    fun setLogLevel(level: KaliumLogLevel) {
        appLoggerConfig.setLogLevel(level)
    }
}

object AppJsonStyledLogger {
    /**
     * Log a structured JSON message, in the following format:
     *
     * Example:
     * ```
     * leadingMessage: {map of key-value pairs represented as JSON}
     * ```
     * @param level the severity of the log message
     * @param error optional - the throwable error to be logged
     * @param leadingMessage the leading message useful for later grok parsing
     * @param jsonStringKeyValues the map of key-value pairs to be logged in a valid JSON format
     */
    fun log(
        level: KaliumLogLevel,
        error: Throwable? = null,
        leadingMessage: String,
        jsonStringKeyValues: Map<String, Any?>
    ) = with(appLogger) {
        val logJson = jsonStringKeyValues.toJsonElement()
        val sanitizedLeadingMessage = if (leadingMessage.endsWith(":")) leadingMessage else "$leadingMessage:"
        val logMessage = "$sanitizedLeadingMessage $logJson"
        when (level) {
            KaliumLogLevel.DEBUG -> d(logMessage)
            KaliumLogLevel.INFO -> i(logMessage)
            KaliumLogLevel.WARN -> w(logMessage)
            KaliumLogLevel.ERROR -> e(logMessage, throwable = error)
            KaliumLogLevel.DISABLED,
            KaliumLogLevel.VERBOSE -> v(logMessage)
        }
    }
}
