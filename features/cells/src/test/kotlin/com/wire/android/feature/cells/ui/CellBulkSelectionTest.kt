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

import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.model.CellNodeUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CellBulkSelectionTest {

    @Test
    fun `given a long press that selected its node, when dragging, then select the nodes the drag covers`() = runTest {
        val selection = bulkSelection()

        selection.onLongClick(nodes[1])
        selection.onDragSelection(listOf(nodes[1], nodes[2], nodes[3]))

        assertEquals(setOf("1", "2", "3"), selection.selectedNodes.value.keys)
    }

    @Test
    fun `given a drag, when dragging back towards the long pressed node, then shrink the selection`() = runTest {
        val selection = bulkSelection()

        selection.onLongClick(nodes[1])
        selection.onDragSelection(listOf(nodes[1], nodes[2], nodes[3]))
        selection.onDragSelection(listOf(nodes[1], nodes[2]))

        assertEquals(setOf("1", "2"), selection.selectedNodes.value.keys)
    }

    @Test
    fun `given a selection, when dragging over it, then keep the nodes selected before the drag`() = runTest {
        val selection = bulkSelection()
        selection.toggle(nodes[0])
        selection.toggle(nodes[2])

        selection.onLongClick(nodes[3])
        selection.onDragSelection(listOf(nodes[3], nodes[2], nodes[1]))
        selection.onDragSelection(listOf(nodes[3]))

        assertEquals(setOf("0", "2", "3"), selection.selectedNodes.value.keys)
    }

    @Test
    fun `given a long press that deselected its node, when dragging, then deselect the nodes the drag covers`() = runTest {
        val selection = bulkSelection()
        nodes.forEach(selection::toggle)

        selection.onLongClick(nodes[1])
        selection.onDragSelection(listOf(nodes[1], nodes[2], nodes[3]))

        assertEquals(setOf("0", "4"), selection.selectedNodes.value.keys)
    }

    @Test
    fun `given an ended drag, when a new drag starts, then apply it on top of the selection the first one left`() = runTest {
        val selection = bulkSelection()

        selection.onLongClick(nodes[0])
        selection.onDragSelection(listOf(nodes[0], nodes[1]))
        selection.onDragSelectionEnd()
        selection.onLongClick(nodes[3])
        selection.onDragSelection(listOf(nodes[3], nodes[4]))

        assertEquals(setOf("0", "1", "3", "4"), selection.selectedNodes.value.keys)
    }

    @Test
    fun `given selection is not enabled, when dragging, then select nothing`() = runTest {
        val selection = bulkSelection(isOnline = false)

        selection.onDragSelection(listOf(nodes[0], nodes[1]))

        assertEquals(emptySet<String>(), selection.selectedNodes.value.keys)
    }

    private fun TestScope.bulkSelection(isOnline: Boolean = true) = CellBulkSelection(
        isSupported = true,
        isOnline = MutableStateFlow(isOnline),
        isSelfUserViewerOnly = MutableStateFlow(false),
        scope = backgroundScope,
    ).also { runCurrent() }

    private companion object {
        val nodes = (0..4).map { index ->
            CellNodeUi.File(
                uuid = "$index",
                conversationId = null,
                name = "file$index.pdf",
                mimeType = "application/pdf",
                assetType = AttachmentFileType.OTHER,
                localPath = null,
                size = 1024L,
                userName = null,
                userHandle = null,
                ownerUserId = null,
                conversationName = null,
                modifiedTime = null,
            )
        }
    }
}
