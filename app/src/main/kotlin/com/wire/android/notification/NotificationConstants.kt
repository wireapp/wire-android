package com.wire.android.notification

import com.wire.kalium.logic.data.id.ConversationId

object NotificationConstants {
    const val CALL_CHANNEL_ID = "com.wire.android.notification_call_channel"
    const val CALL_CHANNEL_NAME = "Call Channel"

    const val WEB_SOCKET_CHANNEL_ID = "com.wire.android.web_socket_channel"
    const val WEB_SOCKET_CHANNEL_NAME = "Web Socket Service"

    const val MESSAGE_CHANNEL_ID = "com.wire.android.notification_channel"
    const val MESSAGE_CHANNEL_NAME = "Messages Channel"
    const val MESSAGE_GROUP_KEY = "wire_reloaded_notification_group"
    const val KEY_TEXT_REPLY = "key_text_notification_reply"

    //Notification IDs (has to be unique!)
    val MESSAGE_SUMMARY_ID = "wire_messages_summary_notification".hashCode()
    val CALL_NOTIFICATION_ID = "wire_call_notification".hashCode()

    fun getConversationNotificationId(conversationId: ConversationId) = getConversationNotificationId(conversationId.toString())
    fun getConversationNotificationId(conversationIdString: String) = conversationIdString.hashCode()
}
