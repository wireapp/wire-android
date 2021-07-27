package com.wire.android.core.extension

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

fun OffsetDateTime.isSameDay(offsetDateTime: OffsetDateTime): Boolean =
    this.toLocalDate().isEqual(offsetDateTime.toLocalDate())

fun OffsetDateTime.isSameYear(offsetDateTime: OffsetDateTime) =
    this.year == offsetDateTime.year

fun OffsetDateTime.isMoreThanSixtyMinutesApartOf(anotherOffsetDateTime: OffsetDateTime) =
    ChronoUnit.MINUTES.between(this, anotherOffsetDateTime).absoluteValue > SIXTY_MINUTES

fun OffsetDateTime.isWithinTheLastMinutes(minutes: Long) =
    ChronoUnit.MINUTES.between(this, OffsetDateTime.now()) < minutes

fun OffsetDateTime.timeFromOffsetDateTime(): String {
    val fmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
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
