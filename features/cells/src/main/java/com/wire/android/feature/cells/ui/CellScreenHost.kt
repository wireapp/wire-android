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
import kotlinx.coroutines.CoroutineScope

/** Sends one-off [CellViewAction]s to the screen. */
internal fun interface CellActionSink {
    fun sendAction(action: CellViewAction)
}

/** Updates the list of nodes shown on the screen. */
internal interface CellNodeListUpdater {
    /** Reloads the list from the server. */
    fun refresh()

    /** Takes [node] off the list right away, before the server confirms it's gone. */
    fun hide(node: CellNodeUi)

    /** Puts back a node [hide] took off the list, e.g. when the change it anticipated failed. */
    fun show(node: CellNodeUi)

    /** Applies a change made to a node to the list, if it shows that node, without loading it again. */
    fun applyUpdate(update: CellNodeUpdate)

    /** Suspends until the list got reloaded after a [refresh]. */
    suspend fun awaitRefreshDone()
}

/**
 * What a files screen offers to the controllers acting on its behalf: where to run their work, what the screen shows,
 * sending actions, updating the list and ending the bulk selection.
 */
internal interface CellScreenHost : CellActionSink {
    /** Lives as long as the screen: work launched in it is cancelled when the screen goes away. */
    val scope: CoroutineScope
    val context: CellScreenContext
    val nodeList: CellNodeListUpdater
    fun clearSelection()
}
