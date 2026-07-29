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

package com.wire.android.notification.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.services.ServicesManager
import com.wire.kalium.logic.sync.PendingMessagesForegroundSync
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
class PendingMessagesScheduledReceiver @Inject constructor(
    private val servicesManager: ServicesManager,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PendingMessagesForegroundSync.ACTION_SENDING_OF_PENDING_MESSAGES_SCHEDULED ->
                servicesManager.startPendingMessagesForegroundService()

            PendingMessagesForegroundSync.ACTION_SENDING_OF_PENDING_MESSAGES_CANCELLED ->
                servicesManager.stopPendingMessagesForegroundService()

            else -> {
                appLogger.w("$TAG: unexpected action ${intent.action}")
                return
            }
        }
    }

    companion object {
        private const val TAG = "PendingMessagesScheduledReceiver"
    }
}
