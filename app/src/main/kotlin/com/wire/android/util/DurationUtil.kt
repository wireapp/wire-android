/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.util

import com.wire.android.R
import com.wire.android.util.ui.UIText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

fun Duration.toTimeLongLabelUiText(): UIText.PluralResource = when {
    inWholeDays >= DAYS_IN_WEEK -> {
        val weeks = inWholeDays.toInt() / DAYS_IN_WEEK
        UIText.PluralResource(R.plurals.weeks_long_label, weeks, weeks)
    }

    inWholeDays >= 1 -> UIText.PluralResource(R.plurals.days_long_label, inWholeDays.toInt(), inWholeDays)
    inWholeHours >= 1 -> UIText.PluralResource(R.plurals.hours_long_label, inWholeHours.toInt(), inWholeHours)
    inWholeMinutes >= 1 -> UIText.PluralResource(R.plurals.minutes_long_label, inWholeMinutes.toInt(), inWholeMinutes)
    else -> UIText.PluralResource(R.plurals.seconds_long_label, inWholeSeconds.toInt(), inWholeSeconds)
}

fun Duration.compactLabel(): String {
    val duration = this
    return when {
        duration < MINUTES_IN_HOUR.minutes -> {
            val totalSec = duration.inWholeSeconds.coerceAtLeast(0)
            val m = totalSec / MINUTES_IN_HOUR
            val s = totalSec % MINUTES_IN_HOUR
            "%d:%02d".format(m, s)
        }
        duration < HOURS_IN_DAY.hours -> {
            val h = duration.inWholeHours
            val m = (duration - h.hours).inWholeMinutes
            "%d:%02d".format(h, m)
        }
        duration < DAYS_IN_WEEK.days -> {
            "${duration.inWholeDays}d"
        }
        else -> {
            val weeks = duration.inWholeDays / DAYS_IN_WEEK
            "${weeks}w"
        }
    }
}

private const val DAYS_IN_WEEK = 7
private const val HOURS_IN_DAY = 24

private const val MINUTES_IN_HOUR = 60
