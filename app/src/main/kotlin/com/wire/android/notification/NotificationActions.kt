package com.wire.android.notification

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.wire.android.R

fun getActionReply(context: Context, conversationId: String, userId: String?): NotificationCompat.Action {
    val resultPendingIntent = replyMessagePendingIntent(context, conversationId, userId)

    val remoteInput = RemoteInput.Builder(NotificationConstants.KEY_TEXT_REPLY).build()

    return NotificationCompat.Action.Builder(null, context.getString(R.string.notification_action_reply), resultPendingIntent)
        .addRemoteInput(remoteInput)
        .setAllowGeneratedReplies(true)
        .build()
}

fun getActionCall(context: Context, conversationId: String, userId: String?) = getAction(
    context.getString(R.string.notification_action_call),
    callMessagePendingIntent(context, conversationId, userId)
)

fun getOpenIncomingCallAction(context: Context, conversationId: String) = getAction(
    context.getString(R.string.notification_action_open_call),
    openIncomingCallPendingIntent(context, conversationId)
)

fun getDeclineCallAction(context: Context, conversationId: String, userId: String) = getAction(
    context.getString(R.string.notification_action_decline_call),
    declineCallPendingIntent(context, conversationId, userId)
)

fun getOpenOngoingCallAction(context: Context, conversationId: String) = getAction(
    context.getString(R.string.notification_action_open_call),
    openOngoingCallPendingIntent(context, conversationId)
)

fun getHangUpCallAction(context: Context, conversationId: String, userId: String) = getAction(
    context.getString(R.string.notification_action_hang_up_call),
    endOngoingCallPendingIntent(context, conversationId, userId)
)

private fun getAction(title: String, intent: PendingIntent) = NotificationCompat.Action
    .Builder(null, title, intent)
    .build()
