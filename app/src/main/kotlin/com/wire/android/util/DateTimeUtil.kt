package com.wire.android.util

import java.text.DateFormat
import java.text.SimpleDateFormat

val serverDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
val mediumDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)

fun String.formatMediumDateTime(): String? = serverDateTimeFormat.parse(this)?.let { mediumDateTimeFormat.format(it) }
