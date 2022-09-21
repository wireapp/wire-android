package com.wire.android.notification

import com.wire.kalium.logic.data.id.ConversationId

//TODO: The names need to be localisable
object NotificationConstants {
    const val INCOMING_CALL_CHANNEL_ID = "com.wire.android.notification_incoming_call_channel"
    const val INCOMING_CALL_CHANNEL_NAME = "Incoming calls"
    const val ONGOING_CALL_CHANNEL_ID = "com.wire.android.notification_ongoing_call_channel"
    const val ONGOING_CALL_CHANNEL_NAME = "Ongoing calls"

    const val WEB_SOCKET_CHANNEL_ID = "com.wire.android.persistent_web_socket_channel"
    const val WEB_SOCKET_CHANNEL_NAME = "Persistent Web Socket Channel"

    const val MESSAGE_CHANNEL_ID = "com.wire.android.notification_channel"
    const val MESSAGE_CHANNEL_NAME = "Messages Channel"
    const val MESSAGE_GROUP_KEY = "wire_reloaded_notification_group"
    const val KEY_TEXT_REPLY = "key_text_notification_reply"

    const val OTHER_CHANNEL_ID = "com.wire.android.message_synchronization"
    const val OTHER_CHANNEL_NAME = "Message Synchronization"

    //Notification IDs (has to be unique!)
    val MESSAGE_SUMMARY_ID = "wire_messages_summary_notification".hashCode()
    val CALL_INCOMING_NOTIFICATION_ID = "wire_incoming_call_notification".hashCode()
    val CALL_ONGOING_NOTIFICATION_ID = "wire_ongoing_call_notification".hashCode()
    val PERSISTENT_NOTIFICATION_ID = "wire_persistent_web_socket_notification".hashCode()

    fun getConversationNotificationId(conversationId: ConversationId) = getConversationNotificationId(conversationId.toString())
    fun getConversationNotificationId(conversationIdString: String) = conversationIdString.hashCode()
}
