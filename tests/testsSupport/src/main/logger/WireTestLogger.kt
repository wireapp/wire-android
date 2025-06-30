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
package logger

import java.util.logging.Logger
import java.time.ZoneId
import java.time.Instant
import java.time.ZonedDateTime
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.io.PrintWriter
import java.io.StringWriter

object WireTestLogger {
    fun getLog(className: String): Logger {
        Logger.getLogger("").getHandlers()[0].setFormatter(MinimalFormatter())
        return Logger.getLogger(className)
    }
}

class MinimalFormatter : Formatter() {
    override fun format(record: LogRecord): String {
        val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.millis), ZoneId.systemDefault())

        return String.format(
            "%1\$tT %2\$.4s [%3\$s] (%4\$.9s) %5\$s %n%6\$s",
            dateTime,
            record.level.name,
            record.threadID,
            stripPackageNameAndTrim(record.sourceClassName),
            record.message,
            stackTraceToString(record)
        )
    }

    private fun stackTraceToString(record: LogRecord): String {
        return record.thrown?.let { throwable ->
            StringWriter().use { stringWriter ->
                PrintWriter(stringWriter).use { printWriter ->
                    printWriter.println()
                    throwable.printStackTrace(printWriter)
                    stringWriter.toString()
                }
            }
        } ?: ""
    }

    private fun stripPackageNameAndTrim(name: String): String {
        val index = name.lastIndexOf('.')
        return if (index != -1) name.substring(index + 1) else name
    }
}
