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

package com.wire.android.ui.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.notification.broadcastreceivers.broadcastReceiverDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * This BroadcastReceiver will restart the persistentWebSocket Service after restarting the device.
 */
class StartServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        appLogger.i("$TAG: onReceive called with action ${intent?.action}")
        context ?: return

        val dependencies = context.broadcastReceiverDependencies
        CoroutineScope(SupervisorJob() + dependencies.dispatcherProvider().io()).launch {
            dependencies.startPersistentWebsocketIfNecessary()()
        }
    }

    companion object {
        const val TAG = "StartServiceReceiver"
    }
}
