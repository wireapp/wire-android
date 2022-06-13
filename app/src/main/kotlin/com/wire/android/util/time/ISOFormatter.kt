package com.wire.android.util.time

import java.text.DateFormat
import java.time.Instant
import java.util.Date
import javax.inject.Inject

class ISOFormatter @Inject constructor() {

    fun fromISO8601ToTimeFormat(utcISO: String): String {
        val formatter = DateFormat.getDateTimeInstance()
        val date = Date.from(Instant.parse(utcISO))

        return formatter.format(date)
    }

}
