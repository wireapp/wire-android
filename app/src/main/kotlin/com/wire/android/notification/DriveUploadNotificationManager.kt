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
import android.app.PendingIntent
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
import com.wire.android.feature.notification.R as NR

/**
 * Shows and updates the Shared Drive upload notification, driven directly by
 * [CellUploadCoordinator][com.wire.kalium.cells.domain.CellUploadCoordinator]'s own `uploads` state — the
 * same state the upload bottom sheet renders. There is no separate notification state to keep in sync.
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

    suspend fun observeAndNotify() {
        var previousUserIds = emptySet<UserId>()
        coreLogic.getGlobalScope().observeValidAccounts()
            .collectLatest { accounts ->
                val currentUserIds = accounts.map { (selfUser, _) -> selfUser.id }.toSet()
                (previousUserIds - currentUserIds).forEach { staleUserId ->
                    notificationManager.cancel(NotificationConstants.getDriveUploadNotificationId(staleUserId))
                }
                previousUserIds = currentUserIds

                coroutineScope {
                    accounts.forEach { (selfUser, _) ->
                        launch {
                            coreLogic.getSessionScope(selfUser.id).cells.uploadCoordinator.uploads
                                .collect { uploads -> updateNotification(selfUser.id, uploads) }
                        }
                    }
                }
            }
    }

    private fun updateNotification(userId: UserId, uploads: List<CellUploadItem>) {
        if (uploads.isEmpty()) {
            notificationManager.cancel(NotificationConstants.getDriveUploadNotificationId(userId))
            return
        }

        notificationChannelsManager.createRegularChannel(
            channelId = NotificationConstants.DRIVE_UPLOAD_CHANNEL_ID,
            channelName = NotificationConstants.DRIVE_UPLOAD_CHANNEL_NAME,
            importance = NotificationManagerCompat.IMPORTANCE_LOW
        )

        val notification = buildNotification(userId, uploads)
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NotificationConstants.getDriveUploadNotificationId(userId), notification)
        }
    }

    private fun buildNotification(userId: UserId, uploads: List<CellUploadItem>): Notification {
        val total = uploads.size
        val completed = uploads.count { it.state is CellUploadState.Completed }
        val failed = uploads.count { it.state is CellUploadState.Failed }
        val active = uploads.count { it.state.isActive }

        val builder = NotificationCompat.Builder(context, NotificationConstants.DRIVE_UPLOAD_CHANNEL_ID)
            .setSmallIcon(NR.drawable.notification_icon_small)
            .setContentIntent(contentIntent(userId, uploads))
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        when {
            active > 0 -> {
                builder
                    .setContentTitle(
                        context.resources.getQuantityString(R.plurals.notification_drive_upload_uploading_title, total, total)
                    )
                    .setContentText(context.getString(R.string.notification_drive_upload_progress_text, completed, total))
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .applyProgress(uploads)
                    .addAction(getCancelAllDriveUploadsAction(context, userId.toString()))
            }

            failed == 0 -> {
                builder
                    .setContentTitle(context.getString(R.string.notification_drive_upload_complete_title))
                    .setContentText(
                        context.resources.getQuantityString(R.plurals.notification_drive_upload_complete_text, completed, completed)
                    )
                    .setOngoing(false)
                    .setAutoCancel(true)
            }

            else -> {
                builder
                    .setContentTitle(context.getString(R.string.notification_drive_upload_partial_title, completed, total))
                    .setContentText(
                        context.resources.getQuantityString(R.plurals.notification_drive_upload_partial_text, failed, failed)
                    )
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .addAction(getRetryFailedDriveUploadsAction(context, userId.toString()))
            }
        }

        return builder.build()
    }

    /**
     * Opens the batch's Shared Drive conversation directly, so long as every upload targets the same one:
     * uploads are only ever enqueued from a single [ConversationFilesScreen][com.wire.android.feature.cells.ui.ConversationFilesScreen],
     * so this should always hold, but falls back to just opening the app rather than guessing otherwise.
     */
    private fun contentIntent(userId: UserId, uploads: List<CellUploadItem>): PendingIntent =
        uploads.mapNotNull { it.request?.destinationFolderPath?.substringBefore("/") }
            .distinct()
            .singleOrNull()
            ?.let { conversationId -> driveFilesPendingIntent(context, conversationId, userId.toString()) }
            ?: openAppPendingIntent(context)

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
