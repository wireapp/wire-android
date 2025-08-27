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
    val d = this
    return when {
        d < 1.hours -> {
            val totalSec = d.inWholeSeconds.coerceAtLeast(0)
            val m = totalSec / 60
            val s = totalSec % 60
            "%d:%02d".format(m, s)
        }
        d < 24.hours -> {
            val h = d.inWholeHours
            val m = (d - h.hours).inWholeMinutes
            "%d:%02d".format(h, m)
        }
        d < 7.days -> {
            "${d.inWholeDays}d"
        }
        else -> {
            val weeks = d.inWholeDays / 7
            "${weeks}w"
        }
    }
}

private const val DAYS_IN_WEEK = 7
