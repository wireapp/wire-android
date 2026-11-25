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
package com.wire.android.util

import android.text.format.DateFormat
import androidx.compose.runtime.Stable
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import java.time.Year

//region convenience ext functions
@Stable
fun Date.toMediumOnlyDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.toMediumOnlyDateTime(this, locale, zoneId)

@Stable
fun Instant.deviceDateTimeFormat(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.deviceDateTimeFormat(this, locale, zoneId)

@Stable
fun Instant.formatMediumDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.formatMediumDateTime(this, locale, zoneId)

@Stable
fun Instant.formatFullDateShortTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.formatFullDateShortTime(this, locale, zoneId)

@Stable
fun Instant.formatMonthDayShortTime(
    is24Hour: Boolean,
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.formatMonthDayShortTime(this, is24Hour, locale, zoneId)

@Stable
fun Instant.uiMessageDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.uiMessageDateTime(this, locale, zoneId)

@Stable
fun Instant.fileDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.fileDateTime(this, locale, zoneId)

@Stable
fun Instant.cellFileDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
    currentYear: Int = Year.now(zoneId).value,
): String = DateAndTimeParsers.cellFileDateTime(this, locale, zoneId, currentYear)

@Stable
fun Instant.cellFileTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.cellTimeFormat(this, locale, zoneId)

@Stable
fun Instant.uiReadReceiptDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.uiReadReceiptDateTime(this, locale, zoneId)

@Stable
fun Long.uiLinkExpirationDate(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.linkExpirationDate(this, locale, zoneId)

@Stable
fun Long.uiLinkExpirationTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.linkExpirationTime(this, locale, zoneId)
//endregion

/**
 * Date and time parsers between different formats and types.
 */
class DateAndTimeParsers private constructor() {

    @Suppress("TooManyFunctions")
    companion object {
        private const val LONG_MONTH_DAY_DATE_PATTERN = "MMMMd"
        private const val TIME_24_HOUR_PATTERN = "Hm"
        private const val TIME_12_HOUR_PATTERN = "hm"
        private const val MONTH_DAY_DATE_PATTERN = "MMM dd"
        private const val MONTH_DAY_YEAR_DATE_PATTERN = "MMM dd, yyyy"
        private const val MEETING_DATE_SHORT_PATTERN = "EEE, d MMMM"
        private const val MEETING_DATE_SHORT_WITH_YEAR_PATTERN = "EEE, d MMMM yyyy"
        private const val FILE_DATE_TIME_PATTERN = "yyyy-MM-dd-hh-mm-ss"
        private const val READ_RECEIPT_DATE_TIME_PATTERN = "MMM dd yyyy,  hh:mm a"
        private const val DURATION_MESSAGE_TIME_PATTERN = "mm:ss"
        private const val DAY_OF_WEEK_MONTH_DAY_DATE_PATTERN = "EEEE, MMMM d"
        private const val DAY_OF_WEEK_MONTH_DAY_YEAR_DATE_PATTERN = "EEEE, MMMM d, yyyy"

        private val longDateShortTimeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
        private val shortTimeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        private val mediumDateTimeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
        private val fullDateShortTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)

        fun deviceDateTimeFormat(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = longDateShortTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun formatMediumDateTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = mediumDateTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun formatFullDateShortTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = fullDateShortTimeFormatter.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun formatMonthDayShortTime(
            instant: Instant,
            is24Hour: Boolean,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String {
            val date = instant.toJavaInstant()
            val datePattern = DateFormat.getBestDateTimePattern(locale, LONG_MONTH_DAY_DATE_PATTERN)
            val timePattern = DateFormat.getBestDateTimePattern(locale, if (is24Hour) TIME_24_HOUR_PATTERN else TIME_12_HOUR_PATTERN)
            val formattedDate = DateTimeFormatter.ofPattern(datePattern).withLocale(locale).withZone(zoneId).format(date)
            val formattedTime = DateTimeFormatter.ofPattern(timePattern).withLocale(locale).withZone(zoneId).format(date)
            return "$formattedDate, $formattedTime"
        }

        fun cellTimeFormat(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = shortTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun cellDateFormat(
            instant: Instant,
            showYear: Boolean = false,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String {
            val pattern = if (showYear) MONTH_DAY_YEAR_DATE_PATTERN else MONTH_DAY_DATE_PATTERN
            val formatter = DateTimeFormatter.ofPattern(pattern).withLocale(locale).withZone(zoneId)
            return formatter.format(instant.toJavaInstant())
        }

        fun cellFileDateTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
            currentYear: Int = Year.now(zoneId).value,
        ): String {
            val isFromAnotherYear = instant.toJavaInstant().atZone(zoneId).year != currentYear

            val dateString = cellDateFormat(
                instant = instant,
                showYear = isFromAnotherYear,
                locale = locale,
                zoneId = zoneId
            )
            val timeString = cellTimeFormat(instant = instant, locale = locale, zoneId = zoneId)

            return "$dateString, $timeString"
        }

        fun fileDateTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = DateTimeFormatter.ofPattern(FILE_DATE_TIME_PATTERN, locale).withZone(zoneId).format(instant.toJavaInstant())

        fun uiReadReceiptDateTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = DateTimeFormatter.ofPattern(READ_RECEIPT_DATE_TIME_PATTERN, locale).withZone(zoneId).format(instant.toJavaInstant())

        fun toMediumOnlyDateTime(
            date: Date,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = DateTimeFormatter.ofPattern(MONTH_DAY_YEAR_DATE_PATTERN, locale).withZone(zoneId).format(date.toInstant())

        fun uiMessageDateTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = shortTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun audioMessageTime(
            timeMs: Long,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = DateTimeFormatter.ofPattern(DURATION_MESSAGE_TIME_PATTERN, locale)
            .withZone(zoneId).format(java.time.Instant.ofEpochMilli(timeMs))

        fun videoMessageTime(
            timeMs: Long,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = DateTimeFormatter.ofPattern(DURATION_MESSAGE_TIME_PATTERN, locale)
            .withZone(zoneId).format(java.time.Instant.ofEpochMilli(timeMs))

        fun meetingDate(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
            includeYear: Boolean = false,
        ): String {
            val pattern = if (includeYear) DAY_OF_WEEK_MONTH_DAY_YEAR_DATE_PATTERN else DAY_OF_WEEK_MONTH_DAY_DATE_PATTERN
            return DateTimeFormatter.ofPattern(pattern, locale).withZone(zoneId).format(instant.toJavaInstant())
        }

        fun meetingDateShort(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
            includeYear: Boolean = false,
        ): String {
            val requestedPattern = if (includeYear) MEETING_DATE_SHORT_WITH_YEAR_PATTERN else MEETING_DATE_SHORT_PATTERN
            val pattern = DateFormat.getBestDateTimePattern(locale, requestedPattern)
            return DateTimeFormatter.ofPattern(pattern).withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())
        }

        fun meetingTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = shortTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun linkExpirationDate(
            timeMs: Long,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = DateTimeFormatter.ofPattern(DAY_OF_WEEK_MONTH_DAY_DATE_PATTERN, locale)
            .withZone(zoneId).format(java.time.Instant.ofEpochMilli(timeMs))

        fun linkExpirationTime(
            timeMs: Long,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = shortTimeFormat.withLocale(locale).withZone(zoneId).format(java.time.Instant.ofEpochMilli(timeMs))
    }
}
