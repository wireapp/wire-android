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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.time.Instant as JavaInstant
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.util.Calendar
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
private val messageWeekDayFormatter = SimpleDateFormat(
    "EEEE MMM, hh:mm a",
    Locale.getDefault()
)
private val messageLongerThanWeekAndSameYearFormatter = SimpleDateFormat(
    "MMM dd, hh:mm a",
    Locale.getDefault()
)
private val messageMonthDayAndYear = SimpleDateFormat(
    "MMM dd yyyy, hh:mm a",
    Locale.getDefault()
)
private const val oneMinuteFromMillis = 60 * 1000
private const val thirtyMinutes = 30
private const val oneWeekInDays = 7


private val readReceiptDateTimeFormat = SimpleDateFormat(
    "MMM dd yyyy,  hh:mm a",
    Locale.getDefault()
).apply { timeZone = TimeZone.getDefault() }

private val fileDateTimeFormat = SimpleDateFormat(
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

/**
 * Transforms receive Long to Calendar
 *
 * @return Calendar
 */
private fun Long.getCalendar(): Calendar = Calendar.getInstance().apply {
    timeInMillis = this@getCalendar
}

/**
 * Verifies is received dates
 */
private fun isYesterday(date: Long, now: Long): Boolean {
    val d = JavaInstant.ofEpochMilli(date).truncatedTo(ChronoUnit.DAYS)
    val n = JavaInstant.ofEpochMilli(now).truncatedTo(ChronoUnit.DAYS)
    return d.until(n, ChronoUnit.DAYS) == 1L
}

private fun isDatesWithinWeek(date: Long, now: Long): Boolean =
    date.getCalendar().after(
        now.getCalendar().apply {
            add(Calendar.DATE, -oneWeekInDays)
        }
    )

private fun isDatesSameYear(date: Long, now: Long): Boolean =
    date.getCalendar().get(Calendar.YEAR) == now.getCalendar().get(Calendar.YEAR)

fun String.uiMessageDateTime(): String? = this
    .serverDate()?.let { serverDate ->
        val now = Clock.System.now().toEpochMilliseconds()
        val differenceBetweenServerDateAndNow = now - serverDate.time
        val differenceInMinutes: Long = differenceBetweenServerDateAndNow / oneMinuteFromMillis
        val withinWeek = isDatesWithinWeek(date = serverDate.time, now = now)
        val isSameYear = isDatesSameYear(date = serverDate.time, now = now)



        when {
            differenceInMinutes <= thirtyMinutes -> "$differenceInMinutes minutes ago"
            differenceInMinutes > thirtyMinutes && DateUtils.isToday(serverDate.time) -> "Today, ${messageTimeFormatter.format(serverDate.time)}"
            isYesterday(serverDate.time, now) -> "Yesterday, ${messageTimeFormatter.format(serverDate.time)}"
            withinWeek -> messageWeekDayFormatter.format(serverDate)
            !withinWeek && isSameYear -> messageLongerThanWeekAndSameYearFormatter.format(serverDate)
            else -> messageMonthDayAndYear.format(serverDate)
        }
    }

fun Date.toMediumOnlyDateTime(): String = mediumOnlyDateTimeFormat.format(this)

fun Instant.uiReadReceiptDateTime(): String = readReceiptDateTimeFormat.format(Date(this.toEpochMilliseconds()))

fun Instant.fileDateTime(): String = fileDateTimeFormat
    .format(Date(this.toEpochMilliseconds()))

fun getCurrentParsedDateTime(): String = mediumDateTimeFormat.format(System.currentTimeMillis())

fun Long.timestampToServerDate(): String? = try {
    serverDateTimeFormat.format(Date(this))
} catch (e: ParseException) {
    appLogger.e("There was an error parsing the timestamp")
    null
}
