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

import android.content.Context
import androidx.core.content.ContextCompat
import com.wire.android.appLogger
import com.wire.android.feature.cells.ui.offline.OfflineDownloadManager
import com.wire.android.feature.cells.ui.offline.OfflineDownloadSummary
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Starts [OfflineDownloadService] whenever offline downloads begin, so they keep running with the app in the background.
 * The service stops itself once they are all done.
 */
@SingleIn(AppScope::class)
class OfflineDownloadServiceLauncher @Inject constructor(
    private val context: Context,
    private val offlineDownloadManager: OfflineDownloadManager,
) {
    suspend fun observe() {
        offlineDownloadManager.summaries
            .map { summaries -> summaries.values.any(OfflineDownloadSummary::isActive) }
            .distinctUntilChanged()
            .filter { isActive -> isActive }
            .collect { startService() }
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(context, OfflineDownloadService.newIntent(context))
        } catch (e: IllegalStateException) {
            // Downloads only start from the app in the foreground, this is not expected: they still run, without the notification.
            appLogger.e("OfflineDownloadServiceLauncher: unable to start the service: $e")
        }
    }
}
