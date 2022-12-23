package com.wire.android.notification.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.notification.MessageNotificationManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.functional.fold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReplyReceiver : BroadcastReceiver() { // requires zero argument constructor

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    @NoSession
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val conversationId: String? = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        val userId: String? = intent.getStringExtra(EXTRA_USER_ID)

        if (remoteInput != null && conversationId != null && userId != null) {
            val replyText = remoteInput.getCharSequence(NotificationConstants.KEY_TEXT_REPLY).toString()
            val qualifiedUserId = qualifiedIdMapper.fromStringToQualifiedID(userId)
            val qualifiedConversationId = qualifiedIdMapper.fromStringToQualifiedID(conversationId)

            with(coreLogic.getSessionScope(qualifiedUserId)) {
                // TODO better to move dispatcher logic into UseCase
                CoroutineScope(coroutineContext + dispatcherProvider.io()).launch {
                    messages.sendTextMessage(qualifiedConversationId, replyText)
                        .fold(
                            { updateNotification(context, conversationId, qualifiedUserId, null) },
                            { updateNotification(context, conversationId, qualifiedUserId, replyText) }
                        )
                }
            }
        }
    }

    private fun updateNotification(context: Context, conversationId: String, userId: QualifiedID, replyText: String?) =
        MessageNotificationManager.updateNotificationAfterQuickReply(context, conversationId, userId, replyText)

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
