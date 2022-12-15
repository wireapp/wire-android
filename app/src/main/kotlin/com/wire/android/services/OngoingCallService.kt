package com.wire.android.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.notification.CallNotificationManager
import com.wire.android.notification.NotificationConstants.CALL_ONGOING_NOTIFICATION_ID
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OngoingCallService : Service() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    @NoSession
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    private val scope by lazy {
        // There's no UI, no need to run anything using the Main/UI Dispatcher
        CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userIdString = intent?.getStringExtra(EXTRA_USER_ID)
        val conversationIdString = intent?.getStringExtra(EXTRA_CONVERSATION_ID)
        val callName = intent?.getStringExtra(EXTRA_CALL_NAME)

        if (userIdString != null && conversationIdString != null && callName != null) {
            val userId = qualifiedIdMapper.fromStringToQualifiedID(userIdString)

            scope.launch {
                coreLogic.getSessionScope(userId).calls.establishedCall().collect { establishedCall ->
                        if (establishedCall.isEmpty()) {
                            appLogger.i("$TAG: stopSelf. Reason: call was ended")
                            stopSelf()
                        }
                    }
            }

            generateForegroundNotification(callName, conversationIdString, userId)
        } else {
            appLogger.w(
                "$TAG: stopSelf. Reason: some of the parameter is absent. " +
                        "userIdString: ${userIdString?.obfuscateId()}, " +
                        "conversationIdString: ${conversationIdString?.obfuscateId()}, " +
                        "callName: $callName"
            )
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        appLogger.i("$TAG: onDestroy")
    }

    private fun generateForegroundNotification(callName: String, conversationId: String, userId: UserId) {
        val notification: Notification = callNotificationManager.getOngoingCallNotification(callName, conversationId, userId)
        startForeground(CALL_ONGOING_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "OngoingCallService"
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
