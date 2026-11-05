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

package com.wire.android.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.feature.cells.ui.offline.OfflineDownloadManager
import com.wire.android.feature.cells.ui.offline.OfflineDownloadSummary
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants.OFFLINE_DOWNLOAD_CHANNEL_ID
import com.wire.android.notification.NotificationConstants.OFFLINE_DOWNLOAD_CHANNEL_NAME
import com.wire.android.notification.NotificationIds
import com.wire.android.notification.openAppPendingIntent
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.wire.android.feature.notification.R as NR

/**
 * Keeps the app process alive while "make available offline" downloads run, see [OfflineDownloadManager], and shows
 * their progress in its foreground notification, with an action to cancel them all.
 *
 * Started by [OfflineDownloadServiceLauncher] when downloads begin; once none is left, the notification is replaced by
 * a summary of how they went and the service stops.
 */
class OfflineDownloadService : Service() {

    @Inject
    lateinit var offlineDownloadManager: OfflineDownloadManager

    @Inject
    lateinit var notificationChannelsManager: NotificationChannelsManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private var observeJob: Job? = null
    private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        wireApplicationGraph.inject(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId

        if (intent?.action == ACTION_CANCEL_ALL) {
            offlineDownloadManager.cancelAll()
            if (observeJob?.isActive != true) stopSelf(startId)
            return START_NOT_STICKY
        }

        if (observeJob?.isActive != true) {
            if (!startAsForeground(buildProgressNotification(OfflineDownloadSummary()))) return START_NOT_STICKY
            observeJob = scope.launch { observeDownloads() }
        }
        return START_NOT_STICKY
    }

    private suspend fun observeDownloads() {
        var lastNotified: OfflineDownloadSummary? = null
        var lastNotifiedAt = 0L
        val finished = offlineDownloadManager.summaries
            .map { summaries -> summaries.values.fold(OfflineDownloadSummary(), OfflineDownloadSummary::plus) }
            .onEach { summary ->
                if (!summary.isActive) return@onEach
                // Progress moves with every chunk downloaded: only redraw on a new count, or now and then for the bar.
                val now = SystemClock.elapsedRealtime()
                val countsChanged = lastNotified?.copy(downloadedBytes = 0) != summary.copy(downloadedBytes = 0)
                if (countsChanged || now - lastNotifiedAt >= PROGRESS_UPDATE_INTERVAL_MS) {
                    notify(buildProgressNotification(summary))
                    lastNotified = summary
                    lastNotifiedAt = now
                }
            }
            .first { !it.isActive }
        finish(finished)
    }

    private fun finish(summary: OfflineDownloadSummary) {
        // Everything cancelled: nothing worth telling.
        val hasOutcome = summary.savedCount + summary.failedCount > 0
        ServiceCompat.stopForeground(
            this,
            if (hasOutcome) ServiceCompat.STOP_FOREGROUND_DETACH else ServiceCompat.STOP_FOREGROUND_REMOVE
        )
        if (hasOutcome) notify(buildFinishedNotification(summary))
        stopSelf(lastStartId)
    }

    private fun startAsForeground(notification: Notification): Boolean {
        notificationChannelsManager.createRegularChannel(
            channelId = OFFLINE_DOWNLOAD_CHANNEL_ID,
            channelName = OFFLINE_DOWNLOAD_CHANNEL_NAME,
            importance = NotificationManagerCompat.IMPORTANCE_LOW,
        )
        return try {
            ServiceCompat.startForeground(
                this,
                NotificationIds.OFFLINE_DOWNLOAD_NOTIFICATION_ID.ordinal,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
            true
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException: the downloads still run, only without the notification.
            appLogger.e("$TAG: failure while starting foreground: $e")
            stopSelf()
            false
        }
    }

    private fun notify(notification: Notification) {
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NotificationIds.OFFLINE_DOWNLOAD_NOTIFICATION_ID.ordinal, notification)
        }
    }

    private fun baseNotification(): NotificationCompat.Builder =
        NotificationCompat.Builder(this, OFFLINE_DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(NR.drawable.notification_icon_small)
            .setContentIntent(openAppPendingIntent(this))
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

    private fun buildProgressNotification(summary: OfflineDownloadSummary): Notification {
        val builder = baseNotification()
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, getString(R.string.notification_offline_download_action_cancel), cancelAllPendingIntent())

        if (summary.totalCount == 0) {
            return builder
                .setContentTitle(getString(R.string.notification_offline_download_preparing_title))
                .setProgress(0, 0, true)
                .build()
        }

        builder
            .setContentTitle(
                resources.getQuantityString(R.plurals.notification_offline_download_title, summary.totalCount, summary.totalCount)
            )
            .setContentText(getString(R.string.notification_offline_download_progress_text, summary.savedCount, summary.totalCount))
        // Weighted by bytes rather than file count, so one large file mid-transfer still moves the bar.
        if (summary.totalBytes > 0) {
            builder.setProgress(PROGRESS_MAX, ((summary.downloadedBytes * PROGRESS_MAX) / summary.totalBytes).toInt(), false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun buildFinishedNotification(summary: OfflineDownloadSummary): Notification {
        val (title, text) = when {
            summary.ranOutOfStorage -> getString(R.string.notification_offline_download_no_storage_title) to
                    getString(R.string.notification_offline_download_partial_title, summary.savedCount, summary.totalCount)

            summary.failedCount == 0 -> getString(R.string.notification_offline_download_complete_title) to
                    resources.getQuantityString(
                        R.plurals.notification_offline_download_complete_text,
                        summary.savedCount,
                        summary.savedCount
                    )

            else -> getString(R.string.notification_offline_download_partial_title, summary.savedCount, summary.totalCount) to
                    resources.getQuantityString(
                        R.plurals.notification_offline_download_failed_text,
                        summary.failedCount,
                        summary.failedCount
                    )
        }
        return baseNotification()
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
    }

    private fun cancelAllPendingIntent(): PendingIntent = PendingIntent.getService(
        this,
        CANCEL_ALL_REQUEST_CODE,
        Intent(this, OfflineDownloadService::class.java).setAction(ACTION_CANCEL_ALL),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    override fun onTimeout(startId: Int, fgsType: Int) {
        // The system's daily data sync allowance ran out: stop the downloads rather than get the app killed.
        appLogger.w("$TAG: foreground service timeout reached, cancelling offline downloads")
        offlineDownloadManager.cancelAll()
        stopSelf(startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        observeJob = null
        scope.cancel("OfflineDownloadService was destroyed")
    }

    companion object {
        private const val TAG = "OfflineDownloadService"
        private const val ACTION_CANCEL_ALL = "com.wire.android.services.action.CANCEL_ALL_OFFLINE_DOWNLOADS"
        private const val CANCEL_ALL_REQUEST_CODE = 0
        private const val PROGRESS_MAX = 100
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L

        fun newIntent(context: Context): Intent = Intent(context, OfflineDownloadService::class.java)
    }
}
