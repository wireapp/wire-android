package com.wire.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.constraintlayout.compose.override
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.toConversationId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.util.toStringDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationDismissReceiver : BroadcastReceiver() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId: String? = intent.getStringExtra(EXTRA_CONVERSATION_ID)

        GlobalScope.launch(dispatcherProvider.io()) {
            val currentSession = coreLogic.getAuthenticationScope().session.currentSession.invoke()

            if (currentSession is CurrentSessionResult.Success) {
                coreLogic.getSessionScope(currentSession.authSession.userId)
                    .messages
                    //TODO change date //TODO Failure is ignored
                    .markMessagesAsNotified(conversationId?.toConversationId(), System.currentTimeMillis().toStringDate())
            }
        }
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"

        fun newIntent(context: Context, conversationId: String?): Intent =
            Intent(context, NotificationDismissReceiver::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
            }
    }
}
