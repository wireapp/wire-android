package com.wire.android.core.date

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class DateStringMapper {

    fun fromOffsetDateTimeToString(offsetDateTime: OffsetDateTime): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime)

    fun fromStringToOffsetDateTime(timeString: String): OffsetDateTime = OffsetDateTime.parse(timeString)

}
