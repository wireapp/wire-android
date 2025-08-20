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
package com.wire.android.util.ui

import android.text.format.DateUtils.formatElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class CountdownTimer @Inject constructor() {
    private var timerJob: Job? = null

    suspend fun start(seconds: Long, onUpdate: (String) -> Unit, onFinish: () -> Unit) {
        timerJob?.cancel()
        timerJob = coroutineScope {
            launch {
                var countdown = seconds
                while (countdown > 0 && isActive) {
                    onUpdate(formatElapsedTime(countdown--))
                    delay(1.seconds)
                }
                onFinish()
            }
        }
    }
}
