package com.wire.android.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.notification.CallNotificationManager
import com.wire.android.notification.NotificationConstants.CALL_ONGOING_NOTIFICATION_ID
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OngoingCallService : Service() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    @Inject
    @NoSession
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userIdString = intent?.getStringExtra(EXTRA_USER_ID)
        val conversationIdString = intent?.getStringExtra(EXTRA_CONVERSATION_ID)
        val callName = intent?.getStringExtra(EXTRA_CALL_NAME)

        if (userIdString != null && conversationIdString != null && callName != null) {
            val userId = qualifiedIdMapper.fromStringToQualifiedID(userIdString)

            coreLogic.getSessionScope(userId).calls
            generateForegroundNotification(callName, conversationIdString, userIdString)
        } else {
            stopForeground(true)
        }
        return START_STICKY
    }

    private fun generateForegroundNotification(callName: String, conversationId: String, userId: String) {
        callNotificationManager.createOngoingNotificationChannel()

        val notification: Notification = callNotificationManager.getOngoingCallNotification(callName, conversationId, userId)
        startForeground(CALL_ONGOING_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val EXTRA_USER_ID = "user_id_extra"
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"
        private const val EXTRA_CALL_NAME = "call_name_extra"

        fun newIntent(context: Context, userId: String, conversationId: String, callName: String): Intent =
            Intent(context, OngoingCallService::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_CALL_NAME, callName)
            }
    }
}
