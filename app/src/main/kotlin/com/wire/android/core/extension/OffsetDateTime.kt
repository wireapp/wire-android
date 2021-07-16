package com.wire.android.core.extension

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun OffsetDateTime.isSameDay(offsetDateTime: OffsetDateTime): Boolean =
    this.toLocalDate().isEqual(offsetDateTime.toLocalDate())

fun OffsetDateTime.isSameYear(offsetDateTime: OffsetDateTime) =
    this.year == offsetDateTime.year

fun isLastTwoMinutesFromNow(offsetDateTime: OffsetDateTime): Boolean {
    val now = LocalDateTime.now()
    val localTime = LocalDateTime.ofInstant(offsetDateTime.toInstant(), ZoneId.systemDefault())
    return now.minusMinutes(2).isBefore(localTime)
}

fun isLastSixtyMinutesFromNow(offsetDateTime: OffsetDateTime): Boolean {
    val now = LocalDateTime.now()
    val localTime = LocalDateTime.ofInstant(offsetDateTime.toInstant(), ZoneId.systemDefault())
    return now.minusMinutes(SIXTY_MINUTES).isBefore(localTime)
}

fun OffsetDateTime.isLastSixtyMinutes(offsetDateTime: OffsetDateTime) =
    this.until(offsetDateTime, ChronoUnit.MINUTES) > SIXTY_MINUTES

private const val SIXTY_MINUTES = 60L
