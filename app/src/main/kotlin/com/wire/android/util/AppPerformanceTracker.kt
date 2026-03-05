/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import android.os.SystemClock
import com.wire.android.appLogger

/**
 * Captures timestamps at key app entry points and logs elapsed durations for DataDog Logs.
 */
object AppPerformanceTracker {

    @Volatile private var appStartTime: Long? = null

    @Volatile private var notificationOpenTime: Long? = null

    fun markAppStart() {
        appStartTime = SystemClock.elapsedRealtime()
    }

    // Called when the startup path will NOT lead to the conversations list (e.g. user not logged in).
    fun cancelAppStartTracking() {
        appStartTime = null
    }

    fun logConversationsReady() {
        appStartTime?.let {
            val durationMs = SystemClock.elapsedRealtime() - it
            appLogger.i("Perf | event=conversations_list_ready | duration_ms=$durationMs")
            appStartTime = null
        }
    }

    fun markNotificationOpen() {
        notificationOpenTime = SystemClock.elapsedRealtime()
    }

    fun logConversationMessagesReady() {
        notificationOpenTime?.let {
            val durationMs = SystemClock.elapsedRealtime() - it
            appLogger.i("Perf | event=conversation_messages_ready | duration_ms=$durationMs")
            notificationOpenTime = null
        }
    }
}
