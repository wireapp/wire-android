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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wire.android.notification.broadcastreceivers.CallNotificationDismissReceiver
import com.wire.android.notification.broadcastreceivers.EndOngoingCallReceiver
import com.wire.android.notification.broadcastreceivers.NotificationReplyReceiver
import com.wire.android.ui.WireActivity
import com.wire.android.ui.calling.CallActivity
import com.wire.android.ui.calling.CallScreenType
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
    val requestCode = getRequestCode(conversationId, OPEN_MESSAGE_REQUEST_CODE_PREFIX)

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
    val requestCode = getRequestCode(destinationUserId, OPEN_OTHER_USER_PROFILE_CODE_PREFIX)

    return PendingIntent.getActivity(
        context.applicationContext,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

// TODO
fun callMessagePendingIntent(context: Context, conversationId: String, userId: String?): PendingIntent =
    messagePendingIntent(context, conversationId, userId)

fun summaryMessagePendingIntent(context: Context): PendingIntent = openAppPendingIntent(context)

fun replyMessagePendingIntent(context: Context, conversationId: String, userId: String?): PendingIntent = PendingIntent.getBroadcast(
    context,
    getRequestCode(conversationId, REPLY_MESSAGE_REQUEST_CODE_PREFIX),
    NotificationReplyReceiver.newIntent(context, conversationId, userId),
    PendingIntent.FLAG_MUTABLE
)

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
        getRequestCode(conversationId, END_ONGOING_CALL_REQUEST_CODE),
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

fun declineCallPendingIntent(context: Context, conversationId: String, userId: String): PendingIntent {
    val intent = CallNotificationDismissReceiver.newIntent(context, conversationId, userId)

    return PendingIntent.getBroadcast(
        context.applicationContext,
        getRequestCode(conversationId, DECLINE_CALL_REQUEST_CODE),
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

fun fullScreenIncomingCallPendingIntent(context: Context, conversationId: String, userId: String): PendingIntent {
    val intent = openIncomingCallIntent(context, conversationId, userId)

    return PendingIntent.getActivity(
        context,
        FULL_SCREEN_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

private fun openIncomingCallIntent(context: Context, conversationId: String, userId: String) =
    Intent(context.applicationContext, CallActivity::class.java).apply {
        putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId)
        putExtra(CallActivity.EXTRA_USER_ID, userId)
        putExtra(CallActivity.EXTRA_SCREEN_TYPE, CallScreenType.Incoming.name)
    }

private fun openOngoingCallIntent(context: Context, conversationId: String) =
    Intent(context.applicationContext, CallActivity::class.java).apply {
        putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId)
        putExtra(CallActivity.EXTRA_SCREEN_TYPE, CallScreenType.Ongoing.name)
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
        OPEN_MIGRATION_LOGIN_REQUEST_CODE,
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

private const val MESSAGE_NOTIFICATIONS_SUMMARY_REQUEST_CODE = 0
private const val DECLINE_CALL_REQUEST_CODE = "decline_call_"
private const val FULL_SCREEN_REQUEST_CODE = 3
private const val OPEN_ONGOING_CALL_REQUEST_CODE = 4
private const val OPEN_MIGRATION_LOGIN_REQUEST_CODE = 5
private const val END_ONGOING_CALL_REQUEST_CODE = "hang_up_call_"
private const val OPEN_MESSAGE_REQUEST_CODE_PREFIX = "open_message_"
private const val OPEN_OTHER_USER_PROFILE_CODE_PREFIX = "open_other_user_profile_"
private const val REPLY_MESSAGE_REQUEST_CODE_PREFIX = "reply_"

private fun getRequestCode(conversationId: String, prefix: String): Int = (prefix + conversationId).hashCode()
