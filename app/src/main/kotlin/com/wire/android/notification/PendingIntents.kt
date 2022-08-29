@file:Suppress("TooManyFunctions")
package com.wire.android.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wire.android.notification.broadcastreceivers.CallNotificationDismissReceiver
import com.wire.android.notification.broadcastreceivers.EndOngoingCallReceiver
import com.wire.android.notification.broadcastreceivers.NotificationReplyReceiver
import com.wire.android.ui.WireActivity
import com.wire.android.util.deeplink.DeepLinkProcessor

fun messagePendingIntent(context: Context, conversationId: String): PendingIntent {
    val intent = Intent(context.applicationContext, WireActivity::class.java).apply {
        data = Uri.Builder()
            .scheme(DeepLinkProcessor.DEEP_LINK_SCHEME)
            .authority(DeepLinkProcessor.CONVERSATION_DEEPLINK_HOST)
            .appendPath(conversationId)
            .build()
    }
    val requestCode = getRequestCode(conversationId, OPEN_MESSAGE_REQUEST_CODE_PREFIX)

    return PendingIntent.getActivity(
        context.applicationContext,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun otherUserProfilePendingIntent(context: Context, userId: String): PendingIntent {
    val intent = Intent(context.applicationContext, WireActivity::class.java).apply {
        data = Uri.Builder()
            .scheme(DeepLinkProcessor.DEEP_LINK_SCHEME)
            .authority(DeepLinkProcessor.OTHER_USER_PROFILE_DEEPLINK_HOST)
            .appendPath(userId)
            .build()
    }
    val requestCode = getRequestCode(userId, OPEN_OTHER_USER_PROFILE_CODE_PREFIX)

    return PendingIntent.getActivity(
        context.applicationContext,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

// TODO
fun callMessagePendingIntent(context: Context, conversationId: String): PendingIntent = messagePendingIntent(context, conversationId)

fun summaryMessagePendingIntent(context: Context): PendingIntent {
    val intent = Intent(context.applicationContext, WireActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    return PendingIntent.getActivity(
        context.applicationContext,
        MESSAGE_NOTIFICATIONS_SUMMARY_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun replyMessagePendingIntent(context: Context, conversationId: String, userId: String?): PendingIntent = PendingIntent.getBroadcast(
    context,
    getRequestCode(conversationId, REPLY_MESSAGE_REQUEST_CODE_PREFIX),
    NotificationReplyReceiver.newIntent(context, conversationId, userId),
    PendingIntent.FLAG_MUTABLE
)

fun openIncomingCallPendingIntent(context: Context, conversationId: String): PendingIntent {
    val intent = openIncomingCallIntent(context, conversationId)

    return PendingIntent.getActivity(
        context.applicationContext,
        OPEN_INCOMING_CALL_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
fun openOngoingCallPendingIntent(context: Context, conversationId: String): PendingIntent {
    val intent = openOngoingCallIntent(context, conversationId)

    return PendingIntent.getActivity(
        context.applicationContext,
        OPEN_ONGOING_CALL_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun endOngoingCallPendingIntent(context: Context, conversationId: String, userId: String): PendingIntent {
    val intent = EndOngoingCallReceiver.newIntent(context, conversationId, userId)

    return PendingIntent.getBroadcast(
        context.applicationContext,
        END_ONGOING_CALL_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

fun declineCallPendingIntent(context: Context, conversationId: String, userId: String): PendingIntent {
    val intent = CallNotificationDismissReceiver.newIntent(context, conversationId, userId)

    return PendingIntent.getBroadcast(
        context.applicationContext,
        DECLINE_CALL_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

fun fullScreenIncomingCallPendingIntent(context: Context, conversationId: String): PendingIntent {
    val intent = openIncomingCallIntent(context, conversationId)

    return PendingIntent.getActivity(
        context,
        FULL_SCREEN_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

private fun openIncomingCallIntent(context: Context, conversationId: String) =
    Intent(context.applicationContext, WireActivity::class.java).apply {
        data = Uri.Builder()
            .scheme(DeepLinkProcessor.DEEP_LINK_SCHEME)
            .authority(DeepLinkProcessor.INCOMING_CALL_DEEPLINK_HOST)
            .appendPath(conversationId)
            .build()
    }
private fun openOngoingCallIntent(context: Context, conversationId: String) =
    Intent(context.applicationContext, WireActivity::class.java).apply {
        data = Uri.Builder()
            .scheme(DeepLinkProcessor.DEEP_LINK_SCHEME)
            .authority(DeepLinkProcessor.ONGOING_CALL_DEEPLINK_HOST)
            .appendPath(conversationId)
            .build()
    }

fun openAppPendingIntent(context: Context): PendingIntent {
    val appIntent = Intent(context.applicationContext, WireActivity::class.java)
    return PendingIntent.getActivity(
        context.applicationContext,
        MESSAGE_NOTIFICATIONS_SUMMARY_REQUEST_CODE,
        appIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

private const val MESSAGE_NOTIFICATIONS_SUMMARY_REQUEST_CODE = 0
private const val DECLINE_CALL_REQUEST_CODE = 1
private const val OPEN_INCOMING_CALL_REQUEST_CODE = 2
private const val FULL_SCREEN_REQUEST_CODE = 3
private const val OPEN_ONGOING_CALL_REQUEST_CODE = 4
private const val END_ONGOING_CALL_REQUEST_CODE = 5
private const val OPEN_MESSAGE_REQUEST_CODE_PREFIX = "open_message_"
private const val OPEN_OTHER_USER_PROFILE_CODE_PREFIX = "open_other_user_profile_"
private const val CALL_REQUEST_CODE_PREFIX = "call_"
private const val REPLY_MESSAGE_REQUEST_CODE_PREFIX = "reply_"

private fun getRequestCode(conversationId: String, prefix: String): Int = (prefix + conversationId).hashCode()
