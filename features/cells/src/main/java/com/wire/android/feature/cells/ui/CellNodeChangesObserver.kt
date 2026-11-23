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

import com.wire.kalium.cells.domain.CellUploadCoordinator
import com.wire.kalium.cells.domain.CellUploadState
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

/**
 * Keeps the list of a files screen in sync with the changes made elsewhere: the paging source has no way to know about
 * them, so without an explicit refresh, or an update of the nodes concerned, they'd only show once the user leaves and
 * re-enters.
 */
class CellNodeChangesObserver @Inject constructor(
    private val uploadCoordinator: CellUploadCoordinator,
    private val nodeChangeEvents: CellNodeChangeEvents,
) {

    internal fun observe(host: CellScreenHost) {
        refreshOnUploadsCompletedInThisFolder(host)
        refreshOnNodesMovedFromOrIntoThisFolder(host)
        refreshOnNodesRestoredInThisConversation(host)
        updateNodesUpdatedElsewhere(host)
    }

    // The changes are known: no need to load the list again for them.
    private fun updateNodesUpdatedElsewhere(host: CellScreenHost) {
        host.scope.launch {
            nodeChangeEvents.nodeUpdates.collect { update -> host.nodeList.applyUpdate(update) }
        }
    }

    private fun refreshOnUploadsCompletedInThisFolder(host: CellScreenHost) {
        val destinationFolderPath = host.context.currentPath ?: return
        val refreshedIds = mutableSetOf<String>()
        host.scope.launch {
            uploadCoordinator.uploads.collect { uploads ->
                val completedHere = uploads.filter {
                    it.state is CellUploadState.Completed && it.request?.destinationFolderPath == destinationFolderPath
                }
                val hasNewlyCompleted = completedHere.count { refreshedIds.add(it.id) } > 0
                if (hasNewlyCompleted) host.nodeList.refresh()
            }
        }
    }

    private fun refreshOnNodesMovedFromOrIntoThisFolder(host: CellScreenHost) {
        val folderPath = host.context.currentPath?.trim('/') ?: return
        host.scope.launch {
            nodeChangeEvents.changedFolderPaths.collect { paths ->
                if (paths.any { it.trim('/') == folderPath }) host.nodeList.refresh()
            }
        }
    }

    private fun refreshOnNodesRestoredInThisConversation(host: CellScreenHost) {
        if (host.context.isRecycleBin) return
        val rootConversationId = host.context.rootConversationId
        host.scope.launch {
            nodeChangeEvents.restoredInConversations.collect { conversationId ->
                if (rootConversationId == null || rootConversationId == conversationId) host.nodeList.refresh()
            }
        }
    }
}
