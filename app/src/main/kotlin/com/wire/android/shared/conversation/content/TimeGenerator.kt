package com.wire.android.shared.conversation.content

import android.content.Context
import com.wire.android.R
import com.wire.android.core.extension.isLastSixtyMinutesFromNow
import com.wire.android.core.extension.isLastTwoMinutesFromNow
import com.wire.android.core.extension.isSameDay
import com.wire.android.core.extension.isSameYear
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import java.time.LocalDateTime

class TimeGenerator(private val context: Context) {

    fun separatorTime(offsetDateTime: OffsetDateTime) : String {
        return when {
            isLastTwoMinutesFromNow(offsetDateTime) -> context.resources.getString(R.string.conversation_chat_just_now)
            isLastSixtyMinutesFromNow(offsetDateTime) -> minutesAgo(offsetDateTime.toLocalTime())
            else -> fullTime(offsetDateTime)
        }
    }

    private fun fullTime(offsetDateTime: OffsetDateTime): String {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        return when {
            offsetDateTime.isSameDay(now) -> timeFromOffsetDateTime(offsetDateTime)
            offsetDateTime.isSameYear(now) -> dateWithoutYear(offsetDateTime)
            else -> dateWithYear(offsetDateTime)
        }
    }

    private fun minutesAgo(localTime: LocalTime): String {
        val minutes = Duration.between(localTime, LocalDateTime.now(ZoneOffset.UTC)).toMinutes().toInt()
        return context.resources.getQuantityString(R.plurals.conversation_chat_minutes_ago, minutes, minutes.toString())
    }

    fun timeFromOffsetDateTime(offsetDateTime: OffsetDateTime): String {
        val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        return fmt.format(offsetDateTime)
    }

    private fun dateWithoutYear(offsetDateTime: OffsetDateTime): String {
        val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, HH:mm")
        return fmt.format(offsetDateTime)
    }

    private fun dateWithYear(offsetDateTime: OffsetDateTime): String {
        val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy, HH:mm")
        return fmt.format(offsetDateTime)
    }
}
