/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer

import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.util.ui.UIText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("MagicNumber")
enum class SelfDeletionDuration(val value: Duration?, val longLabel: UIText, val shortLabel: UIText) {
    None(null, UIText.StringResource(R.string.label_off), UIText.StringResource(R.string.label_off)),
    TenSeconds(
        10.seconds,
        UIText.PluralResource(R.plurals.seconds_long_label, 10, 10),
        UIText.StringResource(R.string.ten_seconds_short_label)
    ),
    OneMinute(
        1.minutes,
        UIText.PluralResource(R.plurals.minutes_long_label, 1, 1),
        UIText.StringResource(R.string.one_minute_short_label)
    ),
    FiveMinutes(
        5.minutes,
        UIText.PluralResource(R.plurals.minutes_long_label, 5, 5),
        UIText.StringResource(R.string.five_minutes_short_label)
    ),
    OneHour(1.hours, UIText.PluralResource(R.plurals.hours_long_label, 1, 1), UIText.StringResource(R.string.one_hour_short_label)),
    OneDay(1.days, UIText.PluralResource(R.plurals.days_long_label, 1, 1), UIText.StringResource(R.string.one_day_short_label)),
    OneWeek(7.days, UIText.PluralResource(R.plurals.days_long_label, 7, 7), UIText.StringResource(R.string.one_week_short_label)),
    FourWeeks(28.days, UIText.PluralResource(R.plurals.weeks_long_label, 4, 4), UIText.StringResource(R.string.four_weeks_short_label));

    companion object {
        // list which will filter [OneMinute] for release builds because it is only for testing purposes
        fun customValues(): List<SelfDeletionDuration> = values()
            .filter { !(!BuildConfig.DEVELOPER_FEATURES_ENABLED && it == OneMinute) }
    }
}
