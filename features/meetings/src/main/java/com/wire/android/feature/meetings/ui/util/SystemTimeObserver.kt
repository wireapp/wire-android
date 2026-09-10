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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.wire.android.di.ApplicationContext
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SystemTimeObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Emits on system's minute ticks, and when the system date, clock, time zone, or time zone offset changes. */
    operator fun invoke(): Flow<Unit> = callbackFlow {
        val actions = listOf(
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIMEZONE_OFFSET_CHANGED,
        )
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action in actions) trySend(Unit)
            }
        }
        val intentFilter = IntentFilter().apply {
            actions.forEach { addAction(it) }
        }
        context.registerReceiver(receiver, intentFilter)
        awaitClose { context.unregisterReceiver(receiver) }
    }
}
