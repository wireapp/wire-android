/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.meetings.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn

class CurrentTimeProvider(val currentTime: () -> Instant) {
    operator fun invoke(): Instant = currentTime()

    companion object {
        @Suppress("MagicNumber")
        val Preview: CurrentTimeProvider = CurrentTimeProvider(currentTime = { // mocked fixed current time for preview purposes
            Clock.System.todayIn(TimeZone.currentSystemDefault()).atTime(12, 0).toInstant(TimeZone.currentSystemDefault())
        })
        val Default: CurrentTimeProvider = CurrentTimeProvider(currentTime = Clock.System::now)
    }
}

@Composable
fun rememberCurrentTimeProvider(): CurrentTimeProvider = LocalInspectionMode.current.let { isPreview: Boolean ->
    remember { if (isPreview) CurrentTimeProvider.Preview else CurrentTimeProvider.Default }
}
