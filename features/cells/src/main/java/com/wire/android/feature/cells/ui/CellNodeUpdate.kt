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

/**
 * A change made to a node from this device, once the server accepted it. The lists showing the node apply it to the
 * node they loaded, rather than loading everything again from the server just for that.
 */
sealed interface CellNodeUpdate {
    val uuid: String

    /** [node], the node [uuid], with the change applied. */
    fun applyTo(node: CellNodeUi): CellNodeUi

    /** @param remotePath the new path of the node, which ends with its name. */
    data class Renamed(override val uuid: String, val name: String, val remotePath: String) : CellNodeUpdate {
        override fun applyTo(node: CellNodeUi): CellNodeUi = when (node) {
            is CellNodeUi.File -> node.copy(name = name, remotePath = remotePath)
            is CellNodeUi.Folder -> node.copy(name = name, remotePath = remotePath)
        }
    }

    data class TagsChanged(override val uuid: String, val tags: List<String>) : CellNodeUpdate {
        override fun applyTo(node: CellNodeUi): CellNodeUi = when (node) {
            is CellNodeUi.File -> node.copy(tags = tags)
            is CellNodeUi.Folder -> node.copy(tags = tags)
        }
    }

    /** @param publicLinkId the id of the public link the node is now shared with, or null when it no longer is. */
    data class PublicLinkChanged(override val uuid: String, val publicLinkId: String?) : CellNodeUpdate {
        override fun applyTo(node: CellNodeUi): CellNodeUi = when (node) {
            is CellNodeUi.File -> node.copy(publicLinkId = publicLinkId)
            is CellNodeUi.Folder -> node.copy(publicLinkId = publicLinkId)
        }
    }
}

/** [node] with the updates made to it applied, in the order they were made. */
internal fun List<CellNodeUpdate>.applyTo(node: CellNodeUi): CellNodeUi =
    fold(node) { updated, update -> if (update.uuid == node.uuid) update.applyTo(updated) else updated }
