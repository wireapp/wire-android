package com.wire.android.util.time

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

class ISOFormatter @Inject constructor() {

    fun fromISO8601ToTimeFormat(utcISO: String): String {
        val instant = Instant.parse(utcISO)
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)

        return formatter.format(instant)
    }

}
