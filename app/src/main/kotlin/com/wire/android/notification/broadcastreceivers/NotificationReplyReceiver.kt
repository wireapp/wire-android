package com.wire.android.notification.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.NotificationConstants
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReplyReceiver(
    private val qualifiedIdMapper: QualifiedIdMapper
) : BroadcastReceiver() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val conversationId: String? = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        val userId: String? = intent.getStringExtra(EXTRA_USER_ID)

        if (remoteInput != null && conversationId != null && userId != null) {
            val replyText = remoteInput.getCharSequence(NotificationConstants.KEY_TEXT_REPLY).toString()

            GlobalScope.launch(dispatcherProvider.io()) {
                val qualifiedUserId = qualifiedIdMapper.fromStringToQualifiedID(userId)
                val qualifiedConversationId = qualifiedIdMapper.fromStringToQualifiedID(conversationId)
                coreLogic.getSessionScope(qualifiedUserId)
                    .messages
                    .sendTextMessage(qualifiedConversationId, replyText)
            }
        }
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"
        private const val EXTRA_USER_ID = "user_id_extra"

        fun newIntent(context: Context, conversationId: String, userId: String?): Intent =
            Intent(context, NotificationReplyReceiver::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_USER_ID, userId)
            }
    }
}
