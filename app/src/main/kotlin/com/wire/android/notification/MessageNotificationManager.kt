package com.wire.android.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.style.StyleSpan
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import androidx.core.text.toSpannable
import com.wire.android.R
import com.wire.android.ui.WireActivity
import com.wire.android.util.toBitmap
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
@Singleton
class MessageNotificationManager @Inject constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    //TODO remove it
    fun testIt() {
        val sender1 = NotificationMessageAuthor("Sender1", null)
        val sender2 = NotificationMessageAuthor("Sender2", null)
        showNotification(
            NotificationData(
                listOf(
                    NotificationConversation(
                        "1234",
                        "Test User 1",
                        null,
                        listOf(
                            NotificationMessage.Comment(sender1, System.currentTimeMillis(), CommentResId.FILE),
                            NotificationMessage.Text(sender1, System.currentTimeMillis(), "message2"),
                            NotificationMessage.Text(sender1, System.currentTimeMillis(), "message3"),
                            NotificationMessage.Text(
                                sender1,
                                System.currentTimeMillis(),
                                "message4 loooong long long long long message btw"
                            ),
                            NotificationMessage.Comment(sender1, System.currentTimeMillis(), CommentResId.PICTURE)
                        ),
                        true,
                        System.currentTimeMillis()
                    ),
                    NotificationConversation(
                        "1233333",
                        "Testing chat 1",
                        null,
                        listOf(
                            NotificationMessage.Text(sender1, System.currentTimeMillis(), "message 0"),
                            NotificationMessage.Text(sender1, System.currentTimeMillis(), "message 1"),
                            NotificationMessage.Text(sender1, System.currentTimeMillis(), "message 2"),
                            NotificationMessage.Text(sender2, System.currentTimeMillis(), "message 3"),
                            NotificationMessage.Text(
                                sender1,
                                System.currentTimeMillis(),
                                "message4 loooong long  glon glong long message btw"
                            ),
                            NotificationMessage.Text(sender1, System.currentTimeMillis(), "message5")
                        ),
                        false,
                        System.currentTimeMillis()
                    )
                )
            )
        )
    }

    //TODO remove it
    fun testIt2() {
        showNotification(
            NotificationData(
                listOf(
                    NotificationConversation(
                        "1234",
                        "Test User 2",
                        null,
                        listOf(
                            NotificationMessage.Text(
                                NotificationMessageAuthor("Sender1", null),
                                System.currentTimeMillis(),
                                "https://www.google.com/"
                            ),
                        ),
                        true,
                        System.currentTimeMillis()
                    ),
                )
            )
        )
    }

    fun showNotification(data: NotificationData) {
        createNotificationChannelIfNeeded()
        showSummaryIfNeeded(data)
        data.conversations.forEach { showConversationNotification(it) }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MAX)
                .setName(CHANNEL_NAME)
                .build()

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun showSummaryIfNeeded(data: NotificationData) {
        if (data.conversations.size <= 1) return

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(getPendingIntentSummary())
            .build()

        notificationManager.notify(SUMMARY_ID, summaryNotification)
    }

    private fun showConversationNotification(conversation: NotificationConversation) {
        notificationManager.notify(conversation.id.hashCode(), getConversationNotification(conversation))
    }

    private fun getConversationNotification(conversation: NotificationConversation) =
        NotificationCompat.Builder(context.applicationContext, CHANNEL_ID).apply {
            setDefaults(NotificationCompat.DEFAULT_ALL)

            priority = NotificationCompat.PRIORITY_MAX

            setSmallIcon(R.drawable.notification_icon_small)
            setGroup(GROUP_KEY)
            setAutoCancel(true)

            setContentIntent(getPendingIntentMessage(conversation.id))
            addAction(getActionCall(conversation.id))
            addAction(getActionReply(conversation.id))

            setWhen(conversation.lastMessageTime)

            setLargeIcon(conversation.image?.toBitmap())

            setStyle(getMessageStyle(conversation))
        }.build()

    private fun getMessageStyle(conversation: NotificationConversation): NotificationCompat.Style {
        val receiver = Person.Builder().setName("you").build()

        val notificationStyle = NotificationCompat.MessagingStyle(receiver)

        notificationStyle.conversationTitle = if (conversation.isOneToOneConversation) null else conversation.name

        conversation.messages.forEach { messageData ->
            val sender = Person.Builder()
                .apply {
                    setName(messageData.author.name)
                    messageData.author.image?.toBitmap()?.let {
                        setIcon(IconCompat.createWithAdaptiveBitmap(it))
                    }
                }
                .build()

            val message = when (messageData) {
                is NotificationMessage.Text -> messageData.text
                is NotificationMessage.Comment -> {
                    context.getString(messageData.textResId.value).toSpannable()
                        .apply { setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
                }
            }

            val notificationMessage = NotificationCompat.MessagingStyle.Message(message, messageData.time, sender)

            notificationStyle.addMessage(notificationMessage)
        }

        return notificationStyle
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

    private fun getActionCall(conversationId: String) = NotificationCompat.Action(
        null,
        context.getString(R.string.notification_action_call),
        getPendingIntentCall(conversationId)
    )

    //TODO
    private fun getPendingIntentMessage(conversationId: String): PendingIntent {
        return getPendingIntentSummary()
    }

    //TODO
    private fun getPendingIntentCall(conversationId: String): PendingIntent = getPendingIntentSummary()

    private fun getPendingIntentSummary(): PendingIntent {
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

    companion object {
        private const val CHANNEL_ID = "com.wire.android.notification_channel"
        private const val CHANNEL_NAME = "Messages Channel"
        private const val GROUP_KEY = "wire_reloaded_notification_group"
        private const val SUMMARY_ID = 0

        const val KEY_TEXT_REPLY = "key_text_notification_reply"

        fun cancelNotification(context: Context, notificationId: Int) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }
}
