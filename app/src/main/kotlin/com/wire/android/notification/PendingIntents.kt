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

@file:Suppress("TooManyFunctions")

package com.wire.android.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wire.android.notification.broadcastreceivers.EndOngoingCallReceiver
import com.wire.android.notification.broadcastreceivers.IncomingCallActionReceiver
import com.wire.android.notification.broadcastreceivers.NotificationReplyReceiver
import com.wire.android.notification.broadcastreceivers.PlayPauseAudioMessageReceiver
import com.wire.android.notification.broadcastreceivers.StopAudioMessageReceiver
import com.wire.android.ui.WireActivity
import com.wire.android.ui.calling.getIncomingCallIntent
import com.wire.android.ui.calling.getOutgoingCallIntent
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.util.deeplink.DeepLinkProcessor

fun messagePendingIntent(context: Context, conversationId: String, userId: String?): PendingIntent {
    val intent = Intent(context.applicationContext, WireActivity::class.java).apply {
        data = Uri.Builder()
            .scheme(DeepLinkProcessor.DEEP_LINK_SCHEME)
            .authority(DeepLinkProcessor.CONVERSATION_DEEPLINK_HOST)
            .appendPath(conversationId)
            .appendQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM, userId)
            .build()
    }
    val requestCode = getRequestCode(OPEN_MESSAGE_REQUEST_CODE_PREFIX, userId.orEmpty(), conversationId)

    return PendingIntent.getActivity(
        context.applicationContext,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun otherUserProfilePendingIntent(context: Context, destinationUserId: String, userId: String): PendingIntent {
    val intent = Intent(context.applicationContext, WireActivity::class.java).apply {
        data = Uri.Builder()
            .scheme(DeepLinkProcessor.DEEP_LINK_SCHEME)
            .authority(DeepLinkProcessor.OTHER_USER_PROFILE_DEEPLINK_HOST)
            .appendPath(destinationUserId)
            .appendQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM, userId)
            .build()
    }
    val requestCode = getRequestCode(OPEN_OTHER_USER_PROFILE_CODE_PREFIX, userId, destinationUserId)

    return PendingIntent.getActivity(
        context.applicationContext,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun summaryMessagePendingIntent(context: Context): PendingIntent = openAppPendingIntent(context)

fun replyMessagePendingIntent(context: Context, conversationId: String, userId: String?): PendingIntent = PendingIntent.getBroadcast(
    context,
    getRequestCode(REPLY_MESSAGE_REQUEST_CODE_PREFIX, userId.orEmpty(), conversationId),
    NotificationReplyReceiver.newIntent(context, conversationId, userId),
    PendingIntent.FLAG_MUTABLE
)

fun openOngoingCallPendingIntent(
    context: Context,
    conversationId: String,
    userId: String,
    shouldAnswerCall: Boolean = false
): PendingIntent {
    val intent = getOngoingCallIntent(
        context = context,
        conversationId = conversationId,
        userId = userId,
        shouldAnswerCall = shouldAnswerCall
    )
    return PendingIntent.getActivity(
        context.applicationContext,
        getRequestCode(OPEN_ONGOING_CALL_REQUEST_CODE, userId, conversationId, shouldAnswerCall.toString()),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun endOngoingCallPendingIntent(context: Context, conversationId: String, userId: String): PendingIntent {
    val intent = EndOngoingCallReceiver.newIntent(context, conversationId, userId)

    return PendingIntent.getBroadcast(
        context.applicationContext,
        getRequestCode(END_ONGOING_CALL_REQUEST_CODE, userId, conversationId),
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

fun declineCallPendingIntent(context: Context, conversationId: String, userId: String): PendingIntent {
    val intent = IncomingCallActionReceiver.newIntent(
        context = context,
        conversationId = conversationId,
        userId = userId,
        action = IncomingCallActionReceiver.ACTION_DECLINE_CALL
    )

    return PendingIntent.getBroadcast(
        context.applicationContext,
        getRequestCode(DECLINE_CALL_REQUEST_CODE, userId, conversationId),
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

fun answerCallPendingIntent(context: Context, conversationId: String, userId: String): PendingIntent {
    val notificationManager = NotificationManagerCompat.from(context)
    val notification = notificationManager.activeNotifications
    val isAlreadyHavingACall = notification.find {
        it.notification.channelId.contains(NotificationConstants.INCOMING_CALL_CHANNEL_ID) ||
                it.notification.channelId.contains(NotificationConstants.ONGOING_CALL_CHANNEL_ID)
    } != null
    val canAnswerCallWithoutUserInteraction = !isAlreadyHavingACall &&
            (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)

    return if (canAnswerCallWithoutUserInteraction) {
        openOngoingCallPendingIntent(
            context = context,
            conversationId = conversationId,
            userId = userId,
            shouldAnswerCall = true
        )
    } else {
        fullScreenIncomingCallPendingIntent(
            context = context,
            conversationId = conversationId,
            userId = userId,
            shouldAnswerCall = true
        )
    }
}

fun outgoingCallPendingIntent(context: Context, conversationId: String, userId: String): PendingIntent {
    val intent = getOutgoingCallIntent(
        context = context,
        conversationId = conversationId,
        userId = userId
    )
    return PendingIntent.getActivity(
        context,
        getRequestCode(OUTGOING_CALL_REQUEST_CODE, conversationId, userId),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun fullScreenIncomingCallPendingIntent(
    context: Context,
    conversationId: String,
    userId: String,
    shouldAnswerCall: Boolean = false
): PendingIntent {
    val intent = getIncomingCallIntent(
        context = context,
        conversationId = conversationId,
        userId = userId,
        shouldAnswerCall = shouldAnswerCall
    )
    return PendingIntent.getActivity(
        context,
        getRequestCode(FULL_SCREEN_REQUEST_CODE, userId, conversationId, shouldAnswerCall.toString()),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

private fun openMigrationLoginIntent(context: Context, userHandle: String) =
    Intent(context.applicationContext, WireActivity::class.java).apply {
        data = Uri.Builder()
            .scheme(DeepLinkProcessor.DEEP_LINK_SCHEME)
            .authority(DeepLinkProcessor.MIGRATION_LOGIN_HOST)
            .appendPath(userHandle)
            .build()
    }

fun openMigrationLoginPendingIntent(context: Context, userHandle: String): PendingIntent =
    PendingIntent.getActivity(
        context.applicationContext,
        getRequestCode(OPEN_MIGRATION_LOGIN_REQUEST_CODE, userHandle),
        openMigrationLoginIntent(context, userHandle),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

fun openAppPendingIntent(context: Context): PendingIntent {
    val appIntent = Intent(context.applicationContext, WireActivity::class.java).apply {
        // pass empty URI so the OS will call onNewIntent instead of onCreate
        // for the WireActivity, keeping it open
        data = Uri.Builder().build()
    }
    return PendingIntent.getActivity(
        context.applicationContext,
        MESSAGE_NOTIFICATIONS_SUMMARY_REQUEST_CODE,
        appIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun playPauseAudioPendingIntent(context: Context): PendingIntent {
    val intent = PlayPauseAudioMessageReceiver.newIntent(context)

    return PendingIntent.getBroadcast(
        context.applicationContext,
        getRequestCode(PLAY_PAUSE_AUDIO_REQUEST_CODE),
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

fun stopAudioPendingIntent(context: Context): PendingIntent {
    val intent = StopAudioMessageReceiver.newIntent(context)

    return PendingIntent.getBroadcast(
        context.applicationContext,
        getRequestCode(STOP_AUDIO_REQUEST_CODE),
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

private const val MESSAGE_NOTIFICATIONS_SUMMARY_REQUEST_CODE = 0
private const val DECLINE_CALL_REQUEST_CODE = "decline_call_"
private const val FULL_SCREEN_REQUEST_CODE = "incoming_call_"
private const val OPEN_ONGOING_CALL_REQUEST_CODE = "ongoing_call_"
private const val OPEN_MIGRATION_LOGIN_REQUEST_CODE = "migration_login_"
private const val OUTGOING_CALL_REQUEST_CODE = "outgoing_call_"
private const val END_ONGOING_CALL_REQUEST_CODE = "hang_up_call_"
private const val PLAY_PAUSE_AUDIO_REQUEST_CODE = "play_or_pause_audio_"
private const val STOP_AUDIO_REQUEST_CODE = "stop_audio_"
private const val OPEN_MESSAGE_REQUEST_CODE_PREFIX = "open_message_"
private const val OPEN_OTHER_USER_PROFILE_CODE_PREFIX = "open_other_user_profile_"
private const val REPLY_MESSAGE_REQUEST_CODE_PREFIX = "reply_"

private fun getRequestCode(prefix: String, vararg parameters: String): Int =
    (prefix + parameters.joinToString("_")).hashCode()
