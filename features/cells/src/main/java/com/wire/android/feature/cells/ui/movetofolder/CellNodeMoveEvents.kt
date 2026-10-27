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
package com.wire.android.feature.cells.ui.movetofolder

import com.wire.android.di.metro.MetroSessionScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells the file lists which folders changed after a move, so they refresh even while they sit on the back stack
 * (e.g. the parent folder when nodes are moved up into it). The move flow can span several destination screens,
 * which is why this isn't passed back as a navigation result.
 */
@SingleIn(MetroSessionScope::class)
class CellNodeMoveEvents @Inject constructor() {

    private val _changedFolderPaths = MutableSharedFlow<Set<String>>(extraBufferCapacity = 1)
    val changedFolderPaths: SharedFlow<Set<String>> = _changedFolderPaths.asSharedFlow()

    /** @param folderPaths the folders the nodes left and the folder they were moved into. */
    fun onNodesMoved(folderPaths: Set<String>) {
        _changedFolderPaths.tryEmit(folderPaths)
    }
}
