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

import android.text.format.DateUtils
import com.wire.android.appLogger
import kotlinx.datetime.Instant
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val serverDateTimeFormat = SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    Locale.getDefault()
).apply { timeZone = TimeZone.getTimeZone("UTC") }
private val mediumDateTimeFormat = DateFormat
    .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
private val longDateShortTimeFormat = DateFormat
    .getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
private val mediumOnlyDateTimeFormat = DateFormat
    .getDateInstance(DateFormat.MEDIUM)
private val messageTimeFormatter = DateFormat
    .getTimeInstance(DateFormat.SHORT)
    .apply { timeZone = TimeZone.getDefault() }
private val messageDateTimeFormatter = DateFormat
    .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    .apply { timeZone = TimeZone.getDefault() }

private val readReceiptDateTimeFormat = SimpleDateFormat(
    "MMM dd yyyy,  hh:mm a",
    Locale.getDefault()
).apply { timeZone = TimeZone.getDefault() }

private val audioFileDateTimeFormat = SimpleDateFormat(
    "yyyy-MM-dd-hh-mm-ss",
    Locale.getDefault()
).apply { timeZone = TimeZone.getDefault() }

private val fullDateShortTimeFormatter = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
fun String.formatMediumDateTime(): String? =
    try {
        this.serverDate()?.let { mediumDateTimeFormat.format(it) }
    } catch (e: ParseException) {
        null
    }

fun String.deviceDateTimeFormat(): String? =
    try {
        this.serverDate()?.let { longDateShortTimeFormat.format(it) }
    } catch (e: ParseException) {
        null
    }

fun String.formatFullDateShortTime(): String? =
    try {
        this.serverDate()?.let { fullDateShortTimeFormatter.format(it) }
    } catch (e: ParseException) {
        null
    }

fun String.serverDate(): Date? = try {
    serverDateTimeFormat.parse(this)
} catch (e: ParseException) {
    appLogger.e("There was an error parsing the server date")
    null
}

fun String.uiMessageDateTime(): String? = this
    .serverDate()?.let { serverDate ->
        when (DateUtils.isToday(serverDate.time)) {
            true -> messageTimeFormatter.format(serverDate)
            false -> messageDateTimeFormatter.format(serverDate)
        }
    }

fun Date.toMediumOnlyDateTime(): String = mediumOnlyDateTimeFormat.format(this)

fun Instant.uiReadReceiptDateTime(): String = readReceiptDateTimeFormat.format(Date(this.toEpochMilliseconds()))

fun Instant.audioFileDateTime(): String = audioFileDateTimeFormat
    .format(Date(this.toEpochMilliseconds()))

fun getCurrentParsedDateTime(): String = mediumDateTimeFormat.format(System.currentTimeMillis())

fun Long.timestampToServerDate(): String? = try {
    serverDateTimeFormat.format(Date(this))
} catch (e: ParseException) {
    appLogger.e("There was an error parsing the timestamp")
    null
}
