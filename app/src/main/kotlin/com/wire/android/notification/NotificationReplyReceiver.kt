package com.wire.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput

class NotificationReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val conversationId: String = intent.getStringExtra(EXTRA_CONVERSATION_ID)!!

        if (remoteInput != null) {
            val replyText = remoteInput.getCharSequence(MessageNotificationManager.KEY_TEXT_REPLY).toString()
            MessageNotificationManager.cancelNotification(context, conversationId.hashCode())
        }
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"

        fun newIntent(context: Context, conversationId: String): Intent =
            Intent(context, NotificationReplyReceiver::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
            }
    }
}
