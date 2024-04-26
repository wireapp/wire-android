/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.notification

import com.wire.kalium.logic.data.user.UserId

// TODO: The names need to be localisable
object NotificationConstants {

    private const val INCOMING_CALL_CHANNEL_ID = "com.wire.android.notification_incoming_call_channel"
    const val INCOMING_CALL_CHANNEL_NAME = "Incoming calls"
    const val ONGOING_CALL_CHANNEL_ID = "com.wire.android.notification_ongoing_call_channel"
    const val ONGOING_CALL_CHANNEL_NAME = "Ongoing calls"

    const val WEB_SOCKET_CHANNEL_ID = "com.wire.android.persistent_web_socket_channel"
    const val WEB_SOCKET_CHANNEL_NAME = "Persistent WebSocket"

    private const val MESSAGE_CHANNEL_ID = "com.wire.android.notification_channel"
    const val MESSAGE_CHANNEL_NAME = "Messages"

    private const val PING_CHANNEL_ID = "com.wire.android.notification_ping_channel"
    const val PING_CHANNEL_NAME = "Pings"
    private const val MESSAGE_GROUP_KEY_PREFIX = "wire_reloaded_notification_group_"
    const val KEY_TEXT_REPLY = "key_text_notification_reply"

    const val MESSAGE_SYNC_CHANNEL_ID = "com.wire.android.message_synchronization"
    const val MESSAGE_SYNC_CHANNEL_NAME = "Message synchronization"

    const val OTHER_CHANNEL_ID = "com.wire.android.other"
    const val OTHER_CHANNEL_NAME = "Other essential actions"

    private const val CHANNEL_GROUP_ID_PREFIX = "com.wire.notification_channel_group"

    // Notification IDs (has to be unique!)
    val CALL_INCOMING_NOTIFICATION_ID = "wire_incoming_call_notification".hashCode()
    val CALL_ONGOING_NOTIFICATION_ID = "wire_ongoing_call_notification".hashCode()
    val PERSISTENT_NOTIFICATION_ID = "wire_persistent_web_socket_notification".hashCode()
    val MESSAGE_SYNC_NOTIFICATION_ID = "wire_notification_fetch_notification".hashCode()
    val MIGRATION_NOTIFICATION_ID = "wire_migration_notification".hashCode()
    val SINGLE_USER_MIGRATION_NOTIFICATION_ID = "wire_single_user_migration_notification".hashCode()
    val MIGRATION_ERROR_NOTIFICATION_ID = "wire_migration_error_notification".hashCode()

    // MessagesSummaryNotification ID depends on User, use fun getMessagesSummaryId(userId: UserId) to get it
    private const val MESSAGE_SUMMARY_ID_STRING = "wire_messages_summary_notification"

    fun getConversationNotificationId(conversationIdString: String, userIdString: String) = (conversationIdString + userIdString).hashCode()
    fun getMessagesGroupKey(userId: UserId?): String = "$MESSAGE_GROUP_KEY_PREFIX${userId?.toString() ?: ""}"
    fun getMessagesSummaryId(userId: UserId): Int = "$MESSAGE_SUMMARY_ID_STRING$userId".hashCode()
    fun getChanelGroupIdForUser(userId: UserId): String = "$CHANNEL_GROUP_ID_PREFIX.$userId"
    fun getMessagesChannelId(userId: UserId): String = getChanelIdForUser(userId, MESSAGE_CHANNEL_ID)
    fun getPingsChannelId(userId: UserId): String = getChanelIdForUser(userId, PING_CHANNEL_ID)
    fun getIncomingChannelId(userId: UserId): String = getChanelIdForUser(userId, INCOMING_CALL_CHANNEL_ID)

    /**
     * @return NotificationChannelId [String] specific for user, use it to post a notifications.
     * @param userId [UserId] which received the notification
     * @param channelIdPrefix prefix of the NotificationChannelId,
     * one of [NotificationConstants.INCOMING_CALL_CHANNEL_ID], [NotificationConstants.MESSAGE_CHANNEL_ID].
     */
    private fun getChanelIdForUser(userId: UserId, channelIdPrefix: String): String = "$channelIdPrefix.$userId"
}
