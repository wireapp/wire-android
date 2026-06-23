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

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.wire.android.BuildConfig.EMM_SUPPORT_ENABLED
import com.wire.android.appLogger
import com.wire.android.emm.ManagedConfigurationsReceiver
import com.wire.android.di.ApplicationContext
import com.wire.kalium.logic.sync.PendingMessagesForegroundSync
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn

/**
 * Manages dynamic registration and unregistration of broadcast receivers.
 * This are receivers that are active while the app is in foreground only.
 */
@SingleIn(AppScope::class)
class DynamicReceiversManager @Inject constructor(
    @ApplicationContext val context: Context,
    private val managedConfigurationsReceiver: ManagedConfigurationsReceiver,
    private val pendingMessagesScheduledReceiver: PendingMessagesScheduledReceiver,
) {
    @Volatile
    private var isRegistered = false

    fun registerAll() {
        synchronized(this) {
            if (!isRegistered) {
                if (EMM_SUPPORT_ENABLED) {
                    appLogger.i("$TAG Registering Runtime ManagedConfigurations Broadcast receiver")
                    context.registerReceiver(managedConfigurationsReceiver, IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED))
                }

                appLogger.i("$TAG Registering PendingMessagesScheduledReceiver")
                val pendingMessagesIntentFilter = IntentFilter(PendingMessagesForegroundSync.ACTION_SENDING_OF_PENDING_MESSAGES_SCHEDULED)
                    .apply { addAction(PendingMessagesForegroundSync.ACTION_SENDING_OF_PENDING_MESSAGES_CANCELLED) }
                ContextCompat.registerReceiver(
                    context,
                    pendingMessagesScheduledReceiver,
                    pendingMessagesIntentFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )

                isRegistered = true
            } else {
                appLogger.w("$TAG Receiver already registered, skipping")
            }
        }
    }

    fun unregisterAll() {
        synchronized(this) {
            if (isRegistered) {
                if (EMM_SUPPORT_ENABLED) {
                    appLogger.i("$TAG Unregistering Runtime ManagedConfigurations Broadcast receiver")
                    context.unregisterReceiver(managedConfigurationsReceiver)
                }

                appLogger.i("$TAG Unregistering PendingMessagesScheduledReceiver")
                context.unregisterReceiver(pendingMessagesScheduledReceiver)

                isRegistered = false
            } else {
                appLogger.w("$TAG Receiver not registered, skipping unregister")
            }
        }
    }

    companion object {
        const val TAG = "DynamicReceiversManager"
    }
}
