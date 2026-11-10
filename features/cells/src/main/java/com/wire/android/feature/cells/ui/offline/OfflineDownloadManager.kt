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
package com.wire.android.feature.cells.ui.offline

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.cells.ui.CellFileLocalPathCache
import com.wire.android.feature.cells.ui.OfflineFileDownloadController
import com.wire.android.feature.cells.util.FileHelper
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutCallback
import com.wire.kalium.network.NetworkStateObserver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Runs the "make available offline" downloads of every logged-in account, for as long as the app process lives.
 *
 * App scoped on purpose: screens and the session graph come and go with the activity, while downloads must keep going
 * in the background. The process is kept alive meanwhile by a foreground service in the app module, which shows
 * [summaries] in an ongoing notification and stops once nothing is downloading anymore.
 *
 * Nothing is persisted: downloads still running or waiting are lost if the process dies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
class OfflineDownloadManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val fileHelper: FileHelper,
    private val networkStateObserver: NetworkStateObserver,
) {
    // Main, as the screens used to run these downloads: the controller and the queue aren't meant for concurrent calls.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val accounts = MutableStateFlow<Map<UserId, AccountOfflineDownloads>>(emptyMap())

    private val logoutCallback = object : LogoutCallback {
        override suspend fun invoke(userId: UserId, reason: LogoutReason) {
            scope.launch {
                accounts.value[userId]?.cancelAll()
                accounts.update { it - userId }
            }
        }
    }

    init {
        coreLogic.getGlobalScope().logoutCallbackManager.register(logoutCallback)
    }

    /** Where every account with downloads stands, see [OfflineDownloadSummary]. */
    val summaries: Flow<Map<UserId, OfflineDownloadSummary>> = accounts.flatMapLatest { accounts ->
        if (accounts.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(accounts.map { (userId, downloads) -> downloads.summary.map { userId to it } }) { it.toMap() }
        }
    }

    /** Cancels every download of every account, running or waiting. */
    fun cancelAll() {
        scope.launch { accounts.value.values.forEach { it.cancelAll() } }
    }

    @Synchronized
    internal fun forAccount(userId: UserId): AccountOfflineDownloads =
        accounts.value[userId] ?: createAccountDownloads(userId).also { created -> accounts.update { it + (userId to created) } }

    private fun createAccountDownloads(userId: UserId): AccountOfflineDownloads {
        val cells = coreLogic.getSessionScope(userId).cells
        // Its own cache, outliving the session graph's: it only holds the download progress and queued files.
        val pathCache = CellFileLocalPathCache()
        return AccountOfflineDownloads(
            scope = scope,
            controller = OfflineFileDownloadController(
                download = cells.downloadCellFile,
                fileHelper = fileHelper,
                saveOfflineFile = cells.saveOfflineFile,
                sharedPathCache = pathCache,
            ),
            pathCache = pathCache,
            networkStateObserver = networkStateObserver,
        )
    }
}
