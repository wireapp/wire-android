package com.wire.android.services

import android.app.Service
import android.content.Context
import android.content.Intent
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * This is helper class that should be used for starting/stopping any services.
 * The idea is that we don't want to inject, or provide any context into ViewModel,
 * but to have an ability start Service from it.
 */
class ServicesManager @Inject constructor(private val context: Context) {

    // Ongoing call
    fun startOngoingCallService(notificationTitle: String, conversationId: ConversationId, userId: UserId) {
        startService(OngoingCallService.newIntent(context, userId.toString(), conversationId.toString(), notificationTitle))
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
        context.startService(intent)
    }

    private fun stopService(serviceClass: KClass<out Service>) {
        context.stopService(Intent(context, serviceClass.java))
    }
}
