package com.wire.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.wire.android.R
import com.wire.android.ui.WireActivity
import com.wire.android.util.toBitmap

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
class NotificationManager(private val context: Context) {

    fun testIt() {
        showNotification(
            NotificationData(
                listOf(
                    NotificationConversation(
                        "1234",
                        "Test User 1",
                        null,
                        listOf(
                            NotificationMessage.Comment("Sender1", CommentResId.FILE),
                            NotificationMessage.Text("Sender1", "message2"),
                            NotificationMessage.Text("Sender1", "message3"),
                            NotificationMessage.Text("Sender2", "message4 loooong long long message btw"),
                            NotificationMessage.Comment("Sender1", CommentResId.PICTURE)
                        ),
                        true,
                        System.currentTimeMillis() - 1000 * 60 * 5
                    ),
                    NotificationConversation(
                        "1233333",
                        "Testing chat 1",
                        null,
                        listOf(
                            NotificationMessage.Text("Sender1", "message 0"),
                            NotificationMessage.Text("Sender1", "message 1"),
                            NotificationMessage.Text("Sender1", "message 2"),
                            NotificationMessage.Text("Sender2", "message 3"),
                            NotificationMessage.Text("Sender1", "message4 loooong long long message btw"),
                            NotificationMessage.Text("Sender1", "message5")
                        ),
                        false,
                        System.currentTimeMillis()
                    )
                )
            )
        )
    }

    fun testIt2() {
        showNotification(
            NotificationData(
                listOf(
                    NotificationConversation(
                        "123412313",
                        "Test User 2",
                        null,
                        listOf(
                            NotificationMessage.Text("Sender1", "https://www.google.com/"),
                        ),
                        true,
                        System.currentTimeMillis() - 1000 * 60 * 5
                    ),
                )
            )
        )
    }

    fun showNotification(data: NotificationData) {
        val manager = NotificationManagerCompat.from(context)

        createNotificationChannelIfNeeded(manager)
        showSummaryIfNeeded(manager, data)
        data.conversations.forEach { showConversationNotification(manager, it) }
    }

    private fun createNotificationChannelIfNeeded(manager: NotificationManagerCompat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            manager.createNotificationChannel(notificationChannel)
        }
    }

    private fun showSummaryIfNeeded(manager: NotificationManagerCompat, data: NotificationData) {
        if (data.conversations.size <= 1) return

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()

        manager.notify(SUMMARY_ID, summaryNotification)
    }

    private fun showConversationNotification(manager: NotificationManagerCompat, conversation: NotificationConversation) {
        manager.notify(conversation.id.hashCode(), getConversationNotification(conversation))
    }

    private fun getConversationNotification(conversation: NotificationConversation) =
        NotificationCompat.Builder(context.applicationContext, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.notification_icon_small)
            setGroup(GROUP_KEY)
            setAutoCancel(true)

            setContentIntent(getPendingIntent(conversation.id))

            setWhen(conversation.lastMessageTime)

            setLargeIcon(conversation.image?.toBitmap())

            addAction(getActionCall(conversation))
            addAction(getActionReply(conversation.id))

            val body = getFormattedText(conversation.messages, !conversation.isDirectChat)
            setContentText(body)
            setStyle(NotificationCompat.BigTextStyle().bigText(body))

            setContentTitle(conversation.name)
        }.build()

    private fun getFormattedText(messages: List<NotificationMessage>, withAuthorPrefix: Boolean): CharSequence {
        val builder = SpannableStringBuilder()

        messages.forEachIndexed { index, message ->
            if (index != 0) builder.appendLine()
            if (withAuthorPrefix) builder.append(message.author + ": ")

            when (message) {
                is NotificationMessage.Text -> builder.append(message.text)
                is NotificationMessage.Comment -> builder.append(
                    context.getString(message.textResId.value),
                    StyleSpan(Typeface.ITALIC),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return builder
    }

    private fun getActionReply(conversationId: String): NotificationCompat.Action {
        val resultPendingIntent = PendingIntent.getBroadcast(
            context,
            conversationId.hashCode(),
            NotificationReplyReceiver.newIntent(context, conversationId),
            PendingIntent.FLAG_MUTABLE
        )

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).build()

        return NotificationCompat.Action.Builder(null, context.getString(R.string.notification_action_reply), resultPendingIntent)
            .addRemoteInput(remoteInput)
            .build()
    }

    private fun getActionCall(conversation: NotificationConversation) = NotificationCompat.Action(
        null,
        context.getString(R.string.notification_action_call),
        getPendingIntentCall(conversation.id)
    )

    //TODO
    private fun getPendingIntent(conversationId: String): PendingIntent {
        val intent = Intent(context.applicationContext, WireActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context.applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    //TODO
    private fun getPendingIntentCall(conversationId: String): PendingIntent = getPendingIntent(conversationId)

    companion object {
        private const val CHANNEL_ID = "com.wire.android.notification_channel"
        private const val CHANNEL_NAME = "wire_notification_channel"
        private const val GROUP_KEY = "wire_notification_group"
        private const val SUMMARY_ID = 0

        const val KEY_TEXT_REPLY = "key_text_reply"

        fun cancelNotification(context: Context, notificationId: Int) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }
}
