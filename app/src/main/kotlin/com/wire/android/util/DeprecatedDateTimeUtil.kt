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

@file:Suppress("TooManyFunctions")

package com.wire.android.util

import com.wire.android.appLogger
import kotlin.time.Instant
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val serverDateTimeFormat =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
private val mediumDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
private val longDateShortTimeFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
private val mediumOnlyDateTimeFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
    .apply { timeZone = TimeZone.getDefault() }
private val messageTimeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
    .apply { timeZone = TimeZone.getDefault() }

private val readReceiptDateTimeFormat = SimpleDateFormat(
    "MMM dd yyyy,  hh:mm a",
    Locale.getDefault()
).apply { timeZone = TimeZone.getDefault() }

private val fileDateTimeFormat = SimpleDateFormat(
    "yyyy-MM-dd-hh-mm-ss",
    Locale.getDefault()
).apply { timeZone = TimeZone.getDefault() }

private val fullDateShortTimeFormatter = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)

@Deprecated(
    message = "This implementation will be removed in the future as it uses discouraged/outdated 'java.text.DateFormat'",
    replaceWith = ReplaceWith("DateAndTimeParsers.formatMediumDateTimeOld() or String.formatMediumDateTime()"),
)
fun String.formatMediumDateTimeOld(): String? =
    try {
        this.serverDate()?.let { mediumDateTimeFormat.format(it) }
    } catch (e: ParseException) {
        null
    }

@Deprecated(
    message = "This implementation will be removed in the future as it uses discouraged/outdated 'java.text.DateFormat'",
    replaceWith = ReplaceWith("DateAndTimeParsers.serverDate() or String.serverDate()"),
)
fun String.deviceDateTimeFormatOld(): String? =
    try {
        this.serverDate()?.let { longDateShortTimeFormat.format(it) }
    } catch (e: ParseException) {
        null
    }

@Deprecated(
    message = "This implementation will be removed in the future as it uses discouraged/outdated 'java.text.DateFormat'",
    replaceWith = ReplaceWith("DateAndTimeParsers.formatFullDateShortTime() or String.formatFullDateShortTime()"),
)
fun String.formatFullDateShortTimeOld(): String? =
    try {
        this.serverDate()?.let { fullDateShortTimeFormatter.format(it) }
    } catch (e: ParseException) {
        null
    }

@Deprecated(
    message = "This implementation will be removed in the future as it uses discouraged/outdated SimpleDateFormat",
    replaceWith = ReplaceWith("DateAndTimeParsers.serverDate() or String.serverDate()"),
)
fun serverDateOld(stringDate: String): Date? = try {
    serverDateTimeFormat.parse(stringDate)
} catch (e: ParseException) {
    appLogger.e("There was an error parsing the server date")
    null
}

@Deprecated(
    message = "This implementation will be removed in the future as it uses discouraged/outdated SimpleDateFormat",
    replaceWith = ReplaceWith("DateAndTimeParsers.uiMessageDateTime() or String.uiMessageDateTime()"),
)
fun String.uiMessageDateTimeOld(): String? = this
    .serverDate()?.let { serverDate ->
        messageTimeFormatter.format(serverDate)
    }

@Deprecated(
    message = "This implementation will be removed in the future as it uses discouraged/outdated SimpleDateFormat",
    replaceWith = ReplaceWith("DateAndTimeParsers.toMediumOnlyDateTime() or Date.toMediumOnlyDateTime()"),
)
fun Date.toMediumOnlyDateTimeOld(): String = mediumOnlyDateTimeFormat.format(this)

@Deprecated(
    message = "This implementation will be removed in the future as it uses discouraged/outdated SimpleDateFormat",
    replaceWith = ReplaceWith("DateAndTimeParsers.uiReadReceiptDateTime() or Instant.uiReadReceiptDateTime()"),
)
fun Instant.uiReadReceiptDateTimeOld(): String = readReceiptDateTimeFormat.format(Date(this.toEpochMilliseconds()))

@Deprecated(
    message = "This implementation will be removed in the future as it uses discouraged/outdated SimpleDateFormat",
    replaceWith = ReplaceWith("DateAndTimeParsers.fileDateTime() or Instant.fileDateTime()"),
)
fun Instant.fileDateTimeOld(): String = fileDateTimeFormat.format(Date(this.toEpochMilliseconds()))
