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

import com.wire.android.di.CurrentAccount
import com.wire.android.di.metro.MetroSessionScope
import com.wire.android.feature.cells.ui.OfflineDownloadStates
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** The offline downloads of the current account, as the screens see them. They run in [OfflineDownloadManager]. */
@SingleIn(MetroSessionScope::class)
class SessionOfflineDownloads internal constructor(
    private val account: AccountOfflineDownloads,
) {
    @Inject
    constructor(
        manager: OfflineDownloadManager,
        @CurrentAccount currentAccount: UserId,
    ) : this(manager.forAccount(currentAccount))

    internal val states: StateFlow<OfflineDownloadStates> get() = account.states
    internal val events: SharedFlow<OfflineDownloadEvent> get() = account.events

    internal fun availableStorageBytes(): Long = account.availableStorageBytes()

    /** @see AccountOfflineDownloads.download */
    internal fun download(files: List<CellNodeUi.File>): Long? = account.download(files)

    internal fun cancel(uuid: String) = account.cancel(uuid)
}
