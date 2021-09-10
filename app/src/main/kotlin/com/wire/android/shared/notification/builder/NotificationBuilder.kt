package com.wire.android.shared.notification.builder

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.wire.android.R
import com.wire.android.feature.conversation.content.mapper.MessageContentMapper
import com.wire.android.feature.conversation.content.ui.CombinedMessageContact

class NotificationBuilder(private val applicationContext: Context, private val messageContentMapper: MessageContentMapper) {

    fun displayNotification(
        conversationId: String,
        conversationName: String,
        combinedMessageContactList: List<CombinedMessageContact>
    ) {

        val messagingStyle = messageStyle(combinedMessageContactList)

        messagingStyle.conversationTitle = conversationName

        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(conversationName)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_WIRE_NOTIFICATIONS)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(applicationContext)
            .notify(conversationId.hashCode(), builder.build())
    }

    private fun messageStyle(messages: List<CombinedMessageContact>): NotificationCompat.MessagingStyle {
        val messageOnePerson = Person.Builder().also {
            it.setKey(messages.first().contact.id)
            it.setName(messages.first().contact.name)
        }.build()

        val messagingStyle = NotificationCompat.MessagingStyle(messageOnePerson)

        messages.asReversed().forEach { combinedMessageContact ->
            val person = Person.Builder().also {
                it.setKey(combinedMessageContact.contact.id)
                it.setName(combinedMessageContact.contact.name)
            }.build()
            val content = messageContentMapper.fromContentToString(combinedMessageContact.message.content)
            val time = combinedMessageContact.message.time.toEpochSecond()
            val notificationMessage = NotificationCompat.MessagingStyle.Message(content, time, person)

            messagingStyle.addMessage(notificationMessage)
        }
        return messagingStyle
    }

    companion object {
        const val GROUP_KEY_WIRE_NOTIFICATIONS = "wire_notifications_group"
        const val NOTIFICATION_MESSAGE_CHANNEL_ID = "notification_message_id"
    }
}
