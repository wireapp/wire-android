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

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.wire.android.R

fun getActionFromOldOne(oldAction: Notification.Action) =
    NotificationCompat.Action.Builder(null, oldAction.title, oldAction.actionIntent).build()

fun getActionReply(
    context: Context,
    conversationId: String,
    userId: String?,
    isAppLocked: Boolean
): NotificationCompat.Action {
    return if (isAppLocked) {
        val resultPendingIntent = messagePendingIntent(context, conversationId, userId)
        NotificationCompat.Action.Builder(null, context.getString(R.string.notification_action_reply), resultPendingIntent)
            .build()
    } else {
        val resultPendingIntent = replyMessagePendingIntent(context, conversationId, userId)
        val remoteInput = RemoteInput.Builder(NotificationConstants.KEY_TEXT_REPLY).build()

        NotificationCompat.Action.Builder(null, context.getString(R.string.notification_action_reply), resultPendingIntent)
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()
    }
}
