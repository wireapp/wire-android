package com.wire.android.core.extension

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun OffsetDateTime.isSameDay(offsetDateTime: OffsetDateTime): Boolean =
    this.toLocalDate().isEqual(offsetDateTime.toLocalDate())

fun OffsetDateTime.isSameYear(offsetDateTime: OffsetDateTime) =
    this.year == offsetDateTime.year

fun OffsetDateTime.isLastSixtyMinutes(offsetDateTime: OffsetDateTime) =
    this.until(offsetDateTime, ChronoUnit.MINUTES) > SIXTY_MINUTES

fun OffsetDateTime.isLastXMinutesFromNow(minutes: Long): Boolean {
    val now = LocalDateTime.now()
    val localTime = LocalDateTime.ofInstant(this.toInstant(), ZoneId.systemDefault())
    return now.minusMinutes(minutes).isBefore(localTime)
}

fun OffsetDateTime.timeFromOffsetDateTime(): String {
    val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    return fmt.format(this)
}

fun OffsetDateTime.dateWithoutYear(): String {
    val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, HH:mm")
    return fmt.format(this)
}

fun OffsetDateTime.dateWithYear(): String {
    val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy, HH:mm")
    return fmt.format(this)
}

private const val SIXTY_MINUTES = 60L
