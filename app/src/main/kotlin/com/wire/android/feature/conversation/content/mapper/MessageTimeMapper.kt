package com.wire.android.feature.conversation.content.mapper

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class MessageTimeMapper {

    fun fromOffsetDateTimeToString(offsetDateTime: OffsetDateTime): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime)

    fun fromStringToOffsetDateTime(timeString: String): OffsetDateTime = OffsetDateTime.parse(timeString)

}
