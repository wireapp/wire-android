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
import kotlinx.datetime.Instant
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
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
    "EEEE MMM dd, hh:mm a",
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
private const val ONE_MINUTE_FROM_MILLIS = 60 * 1000
private const val THIRTY_MINUTES = 30
private const val ONE_WEEK_IN_DAYS = 7
private const val ONE_DAY = 1

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
 * Transforms received Long to Calendar
 *
 * @return Calendar
 */
private fun Long.getCalendar(): Calendar = Calendar.getInstance().apply {
    timeInMillis = this@getCalendar
}

/**
 * Verifies if received dates (date, now) are the same day
 *
 * @param date: Long - message date
 * @param now: Long - current user date when checking the message
 *
 * @return Boolean
 */
private fun isDatesSameDay(date: Long, now: Long): Boolean {
    val messageCalendar = date.getCalendar()
    val nowCalendar = now.getCalendar()

    return nowCalendar.get(Calendar.DAY_OF_MONTH) == messageCalendar.get(Calendar.DAY_OF_MONTH)
            && nowCalendar.get(Calendar.MONTH) == messageCalendar.get(Calendar.MONTH)
            && nowCalendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR)
}

/**
 * Verifies if received dates (date, (now -1 day)) are the same, meaning its yesterday
 *
 * @param date: Long - message date
 * @param now: Long - current user date when checking the message
 *
 * @return Boolean
 */
private fun isYesterday(date: Long, now: Long): Boolean {
    val messageCalendar = date.getCalendar()
    val nowCalendar = now.getCalendar().apply {
        add(Calendar.DATE, -ONE_DAY)
    }

    return nowCalendar.get(Calendar.DAY_OF_MONTH) == messageCalendar.get(Calendar.DAY_OF_MONTH)
            && nowCalendar.get(Calendar.MONTH) == messageCalendar.get(Calendar.MONTH)
            && nowCalendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR)
}

/**
 * Verifies if received dates (date, (now -7 days)) are within the same week.
 * Checks if message date is equals or after (now -7 days)
 *
 * @param date: Long - message date
 * @param now: Long - current user date when checking the message
 *
 * @return Boolean
 */
private fun isDatesWithinWeek(date: Long, now: Long): Boolean =
    date.getCalendar().after(
        now.getCalendar().apply {
            add(Calendar.DATE, -ONE_WEEK_IN_DAYS)
        }
    )

/**
 * Verifies if received dates are the same year
 *
 * @param date: Long - message date
 * @param now: Long - current user date when checking the message
 *
 * @return Boolean
 */
private fun isDatesSameYear(date: Long, now: Long): Boolean =
    date.getCalendar().get(Calendar.YEAR) == now.getCalendar().get(Calendar.YEAR)

sealed interface MessageDateTime {
    data object Now : MessageDateTime
    data class Within30Minutes(val minutes: Int) : MessageDateTime
    data class Today(val time: String) : MessageDateTime
    data class Yesterday(val time: String) : MessageDateTime
    data class WithinWeek(val date: String) : MessageDateTime
    data class NotWithinWeekButSameYear(val date: String) : MessageDateTime
    data class Other(val date: String) : MessageDateTime
}

fun String.uiMessageDateTime(now: Long): MessageDateTime? = this
    .serverDate()?.let { serverDate ->
        val serverDateInMillis = serverDate.time
        val differenceBetweenServerDateAndNow = now - serverDateInMillis
        val differenceInMinutes: Long = differenceBetweenServerDateAndNow / ONE_MINUTE_FROM_MILLIS
        val isSameDay = isDatesSameDay(date = serverDateInMillis, now = now)
        val withinWeek = isDatesWithinWeek(date = serverDateInMillis, now = now)
        val isSameYear = isDatesSameYear(date = serverDateInMillis, now = now)

        when {
            differenceBetweenServerDateAndNow < ONE_MINUTE_FROM_MILLIS -> MessageDateTime.Now
            differenceInMinutes <= THIRTY_MINUTES -> MessageDateTime.Within30Minutes(
                minutes = differenceInMinutes.toInt()
            )
            differenceInMinutes > THIRTY_MINUTES && isSameDay -> MessageDateTime.Today(
                time = messageTimeFormatter.format(serverDateInMillis)
            )
            isYesterday(serverDateInMillis, now) -> MessageDateTime.Yesterday(
                time = messageTimeFormatter.format(serverDateInMillis)
            )
            withinWeek -> MessageDateTime.WithinWeek(
                date = messageWeekDayFormatter.format(serverDate)
            )
            !withinWeek && isSameYear -> MessageDateTime.NotWithinWeekButSameYear(
                date = messageLongerThanWeekAndSameYearFormatter.format(serverDate)
            )
            else -> MessageDateTime.Other(date = messageMonthDayAndYear.format(serverDate))
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
