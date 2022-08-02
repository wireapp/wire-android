package com.wire.android.notification.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.CallNotificationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallNotificationDismissReceiver(
    private val qualifiedIdMapper: QualifiedIdMapper
) : BroadcastReceiver() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId: String = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
        appLogger.i("CallNotificationDismissReceiver: onReceive, conversationId: $conversationId")

        val userId: QualifiedID? = intent.getStringExtra(EXTRA_RECEIVER_USER_ID)?.let {
            qualifiedIdMapper.fromStringToQualifiedID(it)
        } ?: run { null }

        GlobalScope.launch(dispatcherProvider.io()) {
            val sessionScope =
                if (userId != null) {
                    coreLogic.getSessionScope(userId)
                } else {
                    val currentSession = coreLogic.globalScope { session.currentSession() }
                    if (currentSession is CurrentSessionResult.Success) {
                        coreLogic.getSessionScope(currentSession.authSession.session.userId)
                    } else {
                        null
                    }
                }

            sessionScope?.let {
                it.calls.rejectCall(qualifiedIdMapper.fromStringToQualifiedID(conversationId))
            }
            CallNotificationManager.cancelNotification(context)
        }
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"
        private const val EXTRA_RECEIVER_USER_ID = "user_id_extra"

        fun newIntent(context: Context, conversationId: String?, userId: String?): Intent =
            Intent(context, CallNotificationDismissReceiver::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_RECEIVER_USER_ID, userId)
            }
    }
}
