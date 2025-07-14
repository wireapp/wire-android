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
package com.wire.android.notification.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * A [BroadcastReceiver] that cancels execution if it takes too long to avoid ANR.
 */
abstract class CoroutineReceiver : BroadcastReceiver() {

    private companion object {
        // Maximum duration for which the receiver can be executed before ANR is triggered.
        private val MaxDuration = 9.seconds
    }

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val result = goAsync()

        scope.launch {
            try {
                withTimeoutOrNull(MaxDuration) {
                    receive(context, intent)
                } ?: onTimeout(
                    context = context,
                    intent = intent,
                    exception = IllegalStateException(timeoutMessage())
                )
            } finally {
                result.finish()
            }
        }
    }

    private fun timeoutMessage() = "${this::class.java.simpleName} has been suspended for more than $MaxDuration"

    protected abstract suspend fun receive(context: Context, intent: Intent)

    protected open fun onTimeout(context: Context, intent: Intent, exception: Exception) {
        appLogger.e(timeoutMessage(), exception)
    }
}
