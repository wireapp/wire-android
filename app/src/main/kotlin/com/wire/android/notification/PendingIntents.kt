package com.wire.android.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import com.wire.android.notification.broadcastreceivers.CallNotificationDismissReceiver
import com.wire.android.notification.broadcastreceivers.MessageNotificationDismissReceiver
import com.wire.android.notification.broadcastreceivers.NotificationReplyReceiver
import com.wire.android.ui.WireActivity

//TODO
fun messagePendingIntent(context: Context, conversationId: String): PendingIntent {
    return summaryMessagePendingIntent(context)
}

fun dismissMessagePendingIntent(context: Context, conversationId: String?, userId: String?): PendingIntent {
    val intent = MessageNotificationDismissReceiver.newIntent(context, conversationId, userId)
    val requestCode = conversationId?.hashCode() ?: DISMISS_MESSAGE_NOTIFICATION_DEFAULT_REQUEST_CODE

    return PendingIntent.getBroadcast(
        context.applicationContext,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

//TODO
fun callMessagePendingIntent(context: Context, conversationId: String): PendingIntent = summaryMessagePendingIntent(context)

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
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

fun replyMessagePendingIntent(context: Context, conversationId: String): PendingIntent = PendingIntent.getBroadcast(
    context,
    conversationId.hashCode(),
    NotificationReplyReceiver.newIntent(context, conversationId),
    PendingIntent.FLAG_MUTABLE
)

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
fun openCallPendingIntent(context: Context, conversationId: String): PendingIntent {
    //TODO
    val intent = Intent(context.applicationContext, WireActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    return PendingIntent.getActivity(
        context.applicationContext,
        OPEN_CALL_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
fun fullScreenCallPendingIntent(context: Context): PendingIntent {
    //TODO
    val intent = Intent(context.applicationContext, WireActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    return PendingIntent.getActivity(
        context,
        FULL_SCREEN_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

private const val MESSAGE_NOTIFICATIONS_SUMMARY_REQUEST_CODE = 0
private const val DISMISS_MESSAGE_NOTIFICATION_DEFAULT_REQUEST_CODE = 1
private const val DECLINE_CALL_REQUEST_CODE = 2
private const val OPEN_CALL_REQUEST_CODE = 3
private const val FULL_SCREEN_REQUEST_CODE = 4
