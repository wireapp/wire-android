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
import com.wire.android.BuildConfig.EMM_SUPPORT_ENABLED
import com.wire.android.appLogger
import com.wire.android.emm.ManagedConfigurationsReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages dynamic registration and unregistration of broadcast receivers.
 * This are receivers that are active while the app is in foreground only.
 */
@Singleton
class DynamicReceiversManager @Inject constructor(
    @ApplicationContext val context: Context,
    private val managedConfigurationsReceiver: ManagedConfigurationsReceiver
) {
    private var isRegistered = false

    fun registerAll() {
        if (EMM_SUPPORT_ENABLED) {
            synchronized(this) {
                if (!isRegistered) {
                    appLogger.i("$TAG Registering Runtime ManagedConfigurations Broadcast receiver")
                    context.registerReceiver(managedConfigurationsReceiver, IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED))
                    isRegistered = true
                } else {
                    appLogger.w("$TAG Receiver already registered, skipping")
                }
            }
        }
    }

    fun unregisterAll() {
        if (EMM_SUPPORT_ENABLED) {
            synchronized(this) {
                if (isRegistered) {
                    appLogger.i("$TAG Unregistering Runtime ManagedConfigurations Broadcast receiver")
                    context.unregisterReceiver(managedConfigurationsReceiver)
                    isRegistered = false
                } else {
                    appLogger.w("$TAG Receiver not registered, skipping unregister")
                }
            }
        }
    }

    companion object {
        const val TAG = "DynamicReceiversManager"
    }
}
