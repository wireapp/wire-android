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
package com.wire.android.feature.cells.ui

import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.kalium.cells.domain.usecase.offline.DeleteOfflineFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.GetOfflineFileUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Removes the offline copy of files: their offline record, and their file on the device. */
class OfflineCopyRemover @Inject constructor(
    private val getOfflineFile: GetOfflineFileUseCase,
    private val deleteOfflineFile: DeleteOfflineFileUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
) {

    internal suspend fun remove(file: CellNodeUi.File) {
        val localPath = getOfflineFile(file.uuid)?.localPath
            ?: file.localPath
            ?: sharedPathCache.getCompletedPath(file.uuid)

        // Remove the DB record so the UI stops showing the offline indicator.
        deleteOfflineFile(file.uuid)

        // Delete the physical file from device storage
        localPath?.takeIf { it.isNotBlank() }?.let { path ->
            withContext(Dispatchers.IO) { File(path).delete() }
        }

        sharedPathCache.clearCompletedPath(file.uuid)
        sharedPathCache.clearOpenLoadState(file.uuid)
    }
}
