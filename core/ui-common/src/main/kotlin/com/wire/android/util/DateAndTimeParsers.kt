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
@file:Suppress("TooGenericExceptionCaught")

package com.wire.android.util

import androidx.compose.runtime.Stable
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

//region convenience ext functions, for retro compatibility, soon to be deprecated
@Deprecated("Date String parsing is discouraged and will be removed soon for direct Instant/DateTime versions")
@Stable
fun String.serverDate(): Date? = DateAndTimeParsers.serverDate(this)

@Deprecated("Date String parsing is discouraged and will be removed soon for direct Instant/DateTime versions")
@Stable
fun String.deviceDateTimeFormat(): String? = DateAndTimeParsers.deviceDateTimeFormat(this)

@Deprecated("Date String parsing is discouraged and will be removed soon for direct Instant/DateTime versions")
@Stable
fun String.formatMediumDateTime(): String? = DateAndTimeParsers.formatMediumDateTime(this)

@Deprecated("Date String parsing is discouraged and will be removed soon for direct Instant/DateTime versions")
@Stable
fun String.formatFullDateShortTime(): String? = DateAndTimeParsers.formatFullDateShortTime(this)

@Deprecated("Date String parsing is discouraged and will be removed soon for direct Instant/DateTime versions")
@Stable
fun String.uiMessageDateTime(): String? = DateAndTimeParsers.uiMessageDateTime(this)
//endregion

//region convenience ext functions
@Stable
fun Date.toMediumOnlyDateTime(): String = DateAndTimeParsers.toMediumOnlyDateTime(this)

@Stable
fun Instant.fileDateTime(): String = DateAndTimeParsers.fileDateTime(this)

@Stable
fun Instant.cellFileDateTime(): String = DateAndTimeParsers.cellFileDateTime(this)

@Stable
fun Instant.cellFileTime(): String = DateAndTimeParsers.cellTimeFormat(this)

@Stable
fun Instant.uiReadReceiptDateTime(): String = DateAndTimeParsers.uiReadReceiptDateTime(this)

@Stable
fun Long.uiLinkExpirationDate(): String = DateAndTimeParsers.linkExpirationDate(this)

@Stable
fun Long.uiLinkExpirationTime(): String = DateAndTimeParsers.linkExpirationTime(this)
//endregion

/**
 * Date and time parsers between different formats and types.
 */
class DateAndTimeParsers private constructor() {

    @Suppress("TooManyFunctions")
    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC"))

        private val longDateShortTimeFormat =
            java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.LONG, java.text.DateFormat.SHORT, Locale.getDefault()).apply {
                this.timeZone = java.util.TimeZone.getDefault()
            }

        private val longDateFormat =
            java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG, Locale.getDefault()).apply {
                this.timeZone = java.util.TimeZone.getDefault()
            }

        private val shortTimeFormat =
            java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault()).apply {
                this.timeZone = java.util.TimeZone.getDefault()
            }

        private val mediumDateTimeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault()).withLocale(Locale.getDefault())
        private val fullDateShortTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault()).withLocale(Locale.getDefault())
        private val fileDateTimeFormat =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss", Locale.getDefault()).withZone(ZoneId.systemDefault())
        private val readReceiptDateTimeFormat =
            DateTimeFormatter.ofPattern("MMM dd yyyy,  hh:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())
        private val mediumOnlyDateTimeFormat =
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()).withZone(ZoneId.systemDefault())
        private val messageTimeFormatter = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault()).apply {
            this.timeZone = java.util.TimeZone.getDefault()
        }
        private val audioMessageTimeFormat = DateTimeFormatter.ofPattern("mm:ss", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        private val videoMessageTimeFormat = DateTimeFormatter.ofPattern("mm:ss", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        private val linkExpirationDateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM dd", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        private val linkExpirationTimeFormat = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault()).apply {
            this.timeZone = java.util.TimeZone.getDefault()
        }

        @Deprecated("Date String parsing is discouraged and will be removed soon for direct Instant/DateTime versions")
        fun serverDate(stringDate: String): Date? {
            return try {
                Date(LocalDateTime.parse(stringDate, dateTimeFormatter).toInstant(ZoneOffset.UTC).toEpochMilli())
            } catch (e: Exception) {
                null
            }
        }

        @Deprecated("Date String parsing is discouraged and will be removed soon for direct Instant/DateTime versions")
        fun deviceDateTimeFormat(stringDate: String): String? = try {
            longDateShortTimeFormat.format(Date.from(java.time.Instant.parse(stringDate)))
        } catch (e: Exception) {
            null
        }

        @Deprecated("Date String parsing is discouraged and will be removed soon for direct Instant/DateTime versions")
        fun formatMediumDateTime(stringDate: String): String? =
            try {
                stringDate.serverDate()?.let { mediumDateTimeFormat.format(it.toInstant()) }
            } catch (e: Exception) {
                null
            }

        @Deprecated("Date String parsing is discouraged and will be removed soon for direct Instant/DateTime versions")
        fun formatFullDateShortTime(stringDate: String): String? =
            try {
                stringDate.serverDate()?.let { fullDateShortTimeFormatter.format(it.toInstant()) }
            } catch (e: Exception) {
                null
            }

        fun cellTimeFormat(instant: Instant): String {
            val timeFormatter = java.text.DateFormat.getTimeInstance(
                java.text.DateFormat.SHORT,
                Locale.getDefault()
            ).apply {
                timeZone = java.util.TimeZone.getDefault()
            }
            return timeFormatter.format(Date.from(instant.toJavaInstant()))
        }

        fun cellDateFormat(instant: Instant, showYear: Boolean = false): String {
            val pattern = if (showYear) "MMM dd, yyyy" else "MMM dd"

            val formatter = java.text.SimpleDateFormat(pattern, Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getDefault() // system timezone
            }

            return formatter.format(Date.from(instant.toJavaInstant()))
        }

        fun cellFileDateTime(instant: Instant): String {

            val dateString = cellDateFormat(instant)
            val timeString = cellTimeFormat(instant)

            return "$dateString, $timeString"
        }

        fun fileDateTime(instant: Instant): String = fileDateTimeFormat.format(instant.toJavaInstant())

        fun uiReadReceiptDateTime(instant: Instant): String = readReceiptDateTimeFormat.format(instant.toJavaInstant())

        fun toMediumOnlyDateTime(date: Date): String = mediumOnlyDateTimeFormat.format(date.toInstant())

        @Deprecated("Date String parsing is discouraged and will be removed soon for direct Instant/DateTime versions")
        fun uiMessageDateTime(stringDate: String): String? =
            try {
                messageTimeFormatter.format(Date.from(java.time.Instant.parse(stringDate)))
            } catch (e: Exception) {
                null
            }

        fun audioMessageTime(timeMs: Long): String = audioMessageTimeFormat.format(java.time.Instant.ofEpochMilli(timeMs))

        fun videoMessageTime(timeMs: Long): String = videoMessageTimeFormat.format(java.time.Instant.ofEpochMilli(timeMs))

        fun meetingDate(instant: Instant): String = longDateFormat.format(Date.from(instant.toJavaInstant()))
        fun meetingTime(instant: Instant): String = shortTimeFormat.format(Date.from(instant.toJavaInstant()))

        fun linkExpirationDate(timeMs: Long): String = linkExpirationDateFormat.format(java.time.Instant.ofEpochMilli(timeMs))
        fun linkExpirationTime(timeMs: Long): String = linkExpirationTimeFormat.format(Date.from(java.time.Instant.ofEpochMilli(timeMs)))
    }
}
