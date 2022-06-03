package com.wire.android.util.time

import android.content.Context
import android.text.format.DateFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.Date
import javax.inject.Inject

class ISOFormatter @Inject constructor(@ApplicationContext val context: Context) {

    fun fromISO8601ToTimeFormat(utcISO: String): String {
        val date = Date.from(Instant.parse(utcISO))

        val localDateFormat = DateFormat.getDateFormat(context)
        val localTimeFormat = DateFormat.getTimeFormat(context)

        return "${localDateFormat.format(date)}, ${localTimeFormat.format(date)}"
    }

}
