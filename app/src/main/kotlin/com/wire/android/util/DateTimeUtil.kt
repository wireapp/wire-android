package com.wire.android.util

import android.text.format.DateUtils
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

fun String.formatMediumDateTime(): String? =
    try { this.serverDate()?.let { mediumDateTimeFormat.format(it) } } catch (e: ParseException) { null }
fun String.serverDate(): Date? = serverDateTimeFormat.parse(this)
fun String.uiMessageDateTime(): String? = this
    .serverDate()?.let { serverDate ->
        when(DateUtils.isToday(serverDate.time)) {
            true -> messageTimeFormatter.format(serverDate)
            false -> messageDateTimeFormatter.format(serverDate)
        }
    }

fun getCurrentParsedDateTime(): String = mediumDateTimeFormat.format(System.currentTimeMillis())
