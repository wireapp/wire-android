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
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val serverDateTimeFormat =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
private val mediumDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
private val longDateShortTimeFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
private val mediumOnlyDateTimeFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
private val messageTimeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT).apply { timeZone = TimeZone.getDefault() }
private const val ONE_MINUTE_FROM_MILLIS = 60 * 1000
private const val THIRTY_MINUTES = 30
private const val ONE_WEEK_IN_DAYS = 7
private const val ONE_DAY = 1
private const val FORTY_FIVE_MINUTES_DIFFERENCE = 45
private const val MINIMUM_DAYS_DIFFERENCE = 1

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

fun String.formatFullDateShortTime(): String? =
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

sealed interface MessageDateTimeGroup {
    data object Now : MessageDateTimeGroup
    data object Within30Minutes : MessageDateTimeGroup
    data class Daily(val type: Type, val date: LocalDate) : MessageDateTimeGroup {
        enum class Type {
            Today,
            Yesterday,
            WithinWeek,
            NotWithinWeekButSameYear,
            Other
        }
    }
}

fun String.uiMessageDateTime(): String? = this
    .serverDate()?.let { serverDate ->
        messageTimeFormatter.format(serverDate)
    }

fun String.shouldDisplayDatesDifferenceDivider(previousDate: String): Boolean {
    val currentDate = this@shouldDisplayDatesDifferenceDivider

    val currentLocalDateTime = currentDate.serverDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
    val previousLocalDateTime = previousDate.serverDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

    val differenceInMinutes = ChronoUnit.MINUTES.between(
        currentLocalDateTime,
        previousLocalDateTime
    )

    val differenceInDays = ChronoUnit.DAYS.between(
        currentLocalDateTime,
        previousLocalDateTime
    )

    return differenceInMinutes > FORTY_FIVE_MINUTES_DIFFERENCE || differenceInDays >= MINIMUM_DAYS_DIFFERENCE
}

fun String.groupedUIMessageDateTime(now: Long): MessageDateTimeGroup? = this
    .serverDate()?.let { serverDate ->
        val localDate = serverDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val serverDateInMillis = serverDate.time
        val differenceBetweenServerDateAndNow = now - serverDateInMillis
        val differenceInMinutes: Long = differenceBetweenServerDateAndNow / ONE_MINUTE_FROM_MILLIS
        val isSameDay = isDatesSameDay(date = serverDateInMillis, now = now)
        val withinWeek = isDatesWithinWeek(date = serverDateInMillis, now = now)
        val isSameYear = isDatesSameYear(date = serverDateInMillis, now = now)

        when {
            differenceBetweenServerDateAndNow < ONE_MINUTE_FROM_MILLIS -> MessageDateTimeGroup.Now
            differenceInMinutes <= THIRTY_MINUTES -> MessageDateTimeGroup.Within30Minutes
            differenceInMinutes > THIRTY_MINUTES && isSameDay -> MessageDateTimeGroup.Daily(
                type = MessageDateTimeGroup.Daily.Type.Today,
                date = localDate
            )

            isYesterday(serverDateInMillis, now) -> MessageDateTimeGroup.Daily(
                type = MessageDateTimeGroup.Daily.Type.Yesterday,
                date = localDate
            )

            withinWeek -> MessageDateTimeGroup.Daily(
                type = MessageDateTimeGroup.Daily.Type.WithinWeek,
                date = localDate
            )

            !withinWeek && isSameYear -> MessageDateTimeGroup.Daily(
                type = MessageDateTimeGroup.Daily.Type.NotWithinWeekButSameYear,
                date = localDate
            )

            else -> MessageDateTimeGroup.Daily(
                type = MessageDateTimeGroup.Daily.Type.Other,
                date = localDate
            )
        }
    }

fun Date.toMediumOnlyDateTime(): String = mediumOnlyDateTimeFormat.format(this)
fun Instant.uiReadReceiptDateTime(): String = readReceiptDateTimeFormat.format(Date(this.toEpochMilliseconds()))
fun Instant.fileDateTime(): String = fileDateTimeFormat.format(Date(this.toEpochMilliseconds()))
