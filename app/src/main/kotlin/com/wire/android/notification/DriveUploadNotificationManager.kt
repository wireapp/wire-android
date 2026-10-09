/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import androidx.core.app.NotificationManagerCompat
import com.wire.android.R
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.cells.domain.CellUploadItem
import com.wire.kalium.cells.domain.CellUploadState
import com.wire.kalium.cells.domain.isActive
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import com.wire.android.feature.notification.R as NR

/**
 * Shows and updates the Shared Drive upload notification, driven directly by
 * [CellUploadCoordinator][com.wire.kalium.cells.domain.CellUploadCoordinator]'s own `uploads` state — the
 * same state the upload bottom sheet renders. There is no separate notification state to keep in sync.
 *
 * Uploads are grouped by [CellUploadItem.conversationId] into one notification per conversation, so
 * uploading to two Shared Drives at once never mixes their progress, file counts or actions into a single
 * bar: each conversation gets exactly the notification its own upload batch would get on its own.
 *
 * This does not need to be, and is not, the [AssetUploadObserverWorker][com.wire.android.workmanager.worker.AssetUploadObserverWorker]'s
 * foreground notification: that worker's only job is holding the process alive while any upload (message
 * attachment or Drive) is in progress, and it already does that via a separate silent notification. This
 * manager posts its own richer, user-facing notification, updated on every state change rather than once.
 */
@SingleIn(AppScope::class)
class DriveUploadNotificationManager @Inject constructor(
    private val context: Context,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val notificationChannelsManager: NotificationChannelsManager,
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    // Written once per update from each account's own collector (see updateNotifications): concurrent puts
    // land on different user keys, so a ConcurrentHashMap is enough without extra synchronization. Read by
    // the stale-user cleanup below to cancel every per-conversation notification a removed account had up.
    private val conversationIdsByUser = ConcurrentHashMap<UserId, Set<String>>()

    suspend fun observeAndNotify() {
        var previousUserIds = emptySet<UserId>()
        coreLogic.getGlobalScope().observeValidAccounts()
            .collectLatest { accounts ->
                val currentUserIds = accounts.map { (selfUser, _) -> selfUser.id }.toSet()
                (previousUserIds - currentUserIds).forEach(::cancelAllNotificationsForUser)
                previousUserIds = currentUserIds

                coroutineScope {
                    accounts.forEach { (selfUser, _) ->
                        launch {
                            coreLogic.getSessionScope(selfUser.id).cells.uploadCoordinator.uploads
                                .collect { uploads -> updateNotifications(selfUser.id, uploads) }
                        }
                    }
                }
            }
    }

    private fun cancelAllNotificationsForUser(userId: UserId) {
        conversationIdsByUser.remove(userId)?.forEach { conversationId ->
            notificationManager.cancel(NotificationConstants.getDriveUploadNotificationId(userId, conversationId))
        }
    }

    private suspend fun updateNotifications(userId: UserId, uploads: List<CellUploadItem>) {
        val byConversation = uploads.groupBy { it.conversationId }

        val previousConversationIds = conversationIdsByUser[userId].orEmpty()
        (previousConversationIds - byConversation.keys).forEach { staleConversationId ->
            notificationManager.cancel(NotificationConstants.getDriveUploadNotificationId(userId, staleConversationId))
        }
        conversationIdsByUser[userId] = byConversation.keys

        byConversation.forEach { (conversationId, conversationUploads) ->
            updateNotification(userId, conversationId, conversationUploads)
        }
    }

    private suspend fun updateNotification(userId: UserId, conversationId: String, uploads: List<CellUploadItem>) {
        notificationChannelsManager.createRegularChannel(
            channelId = NotificationConstants.DRIVE_UPLOAD_CHANNEL_ID,
            channelName = NotificationConstants.DRIVE_UPLOAD_CHANNEL_NAME,
            importance = NotificationManagerCompat.IMPORTANCE_LOW
        )

        val notification = buildNotification(userId, conversationId, uploads)
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NotificationConstants.getDriveUploadNotificationId(userId, conversationId), notification)
        }
    }

    private suspend fun buildNotification(userId: UserId, conversationId: String, uploads: List<CellUploadItem>): Notification {
        val total = uploads.size
        val completed = uploads.count { it.state is CellUploadState.Completed }
        val failed = uploads.count { it.state is CellUploadState.Failed }
        val active = uploads.count { it.state.isActive }
        val conversationName = coreLogic.getSessionScope(userId).cells.getConversationName(conversationId)

        val builder = NotificationCompat.Builder(context, NotificationConstants.DRIVE_UPLOAD_CHANNEL_ID)
            .setSmallIcon(NR.drawable.notification_icon_small)
            .setContentIntent(driveFilesPendingIntent(context, conversationId, userId.toString()))
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        when {
            active > 0 -> {
                builder
                    .setContentTitle(
                        withConversationName(
                            context.resources.getQuantityString(R.plurals.notification_drive_upload_uploading_title, total, total),
                            conversationName,
                        )
                    )
                    .setContentText(context.getString(R.string.notification_drive_upload_progress_text, completed, total))
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .applyProgress(uploads)
                    .addAction(getCancelAllDriveUploadsAction(context, userId.toString(), conversationId))
            }

            failed == 0 -> {
                builder
                    .setContentTitle(
                        withConversationName(context.getString(R.string.notification_drive_upload_complete_title), conversationName)
                    )
                    .setContentText(
                        context.resources.getQuantityString(R.plurals.notification_drive_upload_complete_text, completed, completed)
                    )
                    .setOngoing(false)
                    .setAutoCancel(true)
            }

            else -> {
                builder
                    .setContentTitle(
                        withConversationName(
                            context.getString(R.string.notification_drive_upload_partial_title, completed, total),
                            conversationName,
                        )
                    )
                    .setContentText(
                        context.resources.getQuantityString(R.plurals.notification_drive_upload_partial_text, failed, failed)
                    )
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .addAction(getRetryFailedDriveUploadsAction(context, userId.toString(), conversationId))
            }
        }

        return builder.build()
    }

    /** Falls back to [title] alone when the conversation name isn't known locally yet. */
    private fun withConversationName(title: String, conversationName: String?): String =
        conversationName?.let { context.getString(R.string.notification_drive_upload_title_with_conversation, title, it) } ?: title

    /** Weighted by bytes rather than file count, so one large file mid-transfer still moves the bar. */
    private fun NotificationCompat.Builder.applyProgress(uploads: List<CellUploadItem>): NotificationCompat.Builder {
        val totalBytes = uploads.sumOf { it.sizeBytes.coerceAtLeast(0L) }
        if (totalBytes <= 0L) {
            return setProgress(0, 0, true)
        }
        val uploadedBytes = uploads.sumOf { item ->
            when (val state = item.state) {
                CellUploadState.Completed -> item.sizeBytes.coerceAtLeast(0L)
                is CellUploadState.Uploading -> (item.sizeBytes.coerceAtLeast(0L) * state.progress).toLong()
                else -> 0L
            }
        }
        return setProgress(PROGRESS_MAX, ((uploadedBytes * PROGRESS_MAX) / totalBytes).toInt(), false)
    }

    private companion object {
        const val PROGRESS_MAX = 100
    }
}