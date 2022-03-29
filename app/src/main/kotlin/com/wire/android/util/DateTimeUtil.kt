package com.wire.android.util

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat

private val serverDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
private val mediumDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)

fun String.formatMediumDateTime(): String? =
    try { serverDateTimeFormat.parse(this)?.let { mediumDateTimeFormat.format(it) } } catch (e: ParseException) { null }
