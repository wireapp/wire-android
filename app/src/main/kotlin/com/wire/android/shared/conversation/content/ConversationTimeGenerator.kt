package com.wire.android.shared.conversation.content

import android.content.Context
import com.wire.android.R
import com.wire.android.core.extension.isWithinTheLastMinutes
import com.wire.android.core.extension.isSameDay
import com.wire.android.core.extension.timeFromOffsetDateTime
import com.wire.android.core.extension.dateWithYear
import com.wire.android.core.extension.isSameYear
import com.wire.android.core.extension.dateWithoutYear
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

import java.time.LocalDateTime

class ConversationTimeGenerator(private val context: Context) {

    fun separatorTimeLabel(offsetDateTime: OffsetDateTime) : String {
        return when {
            offsetDateTime.isWithinTheLastMinutes(TWO_MINUTES) -> context.resources.getString(R.string.conversation_chat_just_now)
            offsetDateTime.isWithinTheLastMinutes(SIXTY_MINUTES) -> minutesAgo(offsetDateTime.toLocalTime())
            else -> fullDateTime(offsetDateTime)
        }
    }

    private fun fullDateTime(offsetDateTime: OffsetDateTime): String {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        return when {
            offsetDateTime.isSameDay(now) -> offsetDateTime.timeFromOffsetDateTime()
            offsetDateTime.isSameYear(now) -> offsetDateTime.dateWithoutYear()
            else -> offsetDateTime.dateWithYear()
        }
    }

    private fun minutesAgo(localTime: LocalTime): String {
        val minutes = Duration.between(localTime, LocalDateTime.now(ZoneOffset.UTC)).toMinutes().toInt()
        return context.resources.getQuantityString(R.plurals.conversation_chat_minutes_ago, minutes, minutes.toString())
    }

    companion object {
        private const val TWO_MINUTES = 2L
        private const val SIXTY_MINUTES = 60L
    }
}
