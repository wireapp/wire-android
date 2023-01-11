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

fun String.formatMediumDateTime(): String? =
    try {
        this.serverDate()?.let { mediumDateTimeFormat.format(it) }
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

fun Instant.uiReadReceiptDateTime(): String = readReceiptDateTimeFormat.format(Date(this.toEpochMilliseconds()))

fun getCurrentParsedDateTime(): String = mediumDateTimeFormat.format(System.currentTimeMillis())

fun Long.timestampToServerDate(): String? = try {
    serverDateTimeFormat.format(Date(this))
} catch (e: ParseException) {
    appLogger.e("There was an error parsing the timestamp")
    null
}
