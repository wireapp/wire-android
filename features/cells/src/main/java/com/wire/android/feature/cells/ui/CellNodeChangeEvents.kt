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

import com.wire.android.di.metro.MetroSessionScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells the file lists that nodes changed elsewhere, so they refresh even while they sit on the back stack: e.g. the
 * parent folder when nodes are moved up into it, or the conversation's files once nodes are restored from the recycle
 * bin. These flows span several screens, which is why this isn't passed back as a navigation result.
 */
@SingleIn(MetroSessionScope::class)
class CellNodeChangeEvents @Inject constructor() {

    private val _changedFolderPaths = MutableSharedFlow<Set<String>>(extraBufferCapacity = 1)
    val changedFolderPaths: SharedFlow<Set<String>> = _changedFolderPaths.asSharedFlow()

    private val _restoredInConversations = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Conversation ids nodes were restored in. Which of their folders isn't known, see [onNodesRestored]. */
    val restoredInConversations: SharedFlow<String> = _restoredInConversations.asSharedFlow()

    /** @param folderPaths the folders the nodes left and the folder they were moved into. */
    fun onNodesMoved(folderPaths: Set<String>) {
        _changedFolderPaths.tryEmit(folderPaths)
    }

    /**
     * Nodes go back to wherever they were deleted from, which the recycle bin doesn't tell: any folder of the
     * conversation may have changed.
     */
    fun onNodesRestored(conversationId: String) {
        _restoredInConversations.tryEmit(conversationId)
    }
}
