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
 *
 *
 */

package com.wire.android.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wire.android.appLogger
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * This is helper class that should be used for starting/stopping any services.
 * The idea is that we don't want to inject, or provide any context into ViewModel,
 * but to have an ability start Service from it.
 */
class ServicesManager @Inject constructor(private val context: Context) {

    // Ongoing call
    suspend fun startOngoingCallService(notificationTitle: String, conversationId: ConversationId, userId: UserId) {
        val onGoingCallService = OngoingCallService.newIntent(context, userId.toString(), conversationId.toString(), notificationTitle)
        delay(DELAY_START_SERVICE)
        startService(onGoingCallService)
    }

    fun stopOngoingCallService() {
        stopService(OngoingCallService::class)
    }

    // Persistent WebSocket
    fun startPersistentWebSocketService() {
        startService(PersistentWebSocketService.newIntent(context))
    }

    fun stopPersistentWebSocketService() {
        stopService(PersistentWebSocketService::class)
    }

    fun isPersistentWebSocketServiceRunning(): Boolean =
        PersistentWebSocketService.isServiceStarted

    private fun startService(intent: Intent) {
        appLogger.i("ServicesManager: starting service for $intent")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopService(serviceClass: KClass<out Service>) {
        context.stopService(Intent(context, serviceClass.java))
    }
    companion object {
        const val DELAY_START_SERVICE = 300L
    }
}
