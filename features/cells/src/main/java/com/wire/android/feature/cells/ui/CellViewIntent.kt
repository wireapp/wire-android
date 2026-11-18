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

import android.net.Uri
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.NodeBottomSheetAction
import com.wire.android.feature.cells.ui.model.NodeMenuItem

sealed interface CellViewIntent {
    data class OnItemClick(val file: CellNodeUi) : CellViewIntent
    data class OnItemMenuClick(val cellNode: CellNodeUi) : CellViewIntent
    data class OnItemLongClick(val cellNode: CellNodeUi) : CellViewIntent

    /** @param nodes the nodes from the long pressed one to the one under the finger, the long pressed one first. */
    data class OnDragSelection(val nodes: List<CellNodeUi>) : CellViewIntent
    data object OnDragSelectionEnd : CellViewIntent
    data object OnClearSelection : CellViewIntent
    data object OnMoveSelectedNodesClick : CellViewIntent
    data object OnDeleteSelectedNodesClick : CellViewIntent
    data object OnBulkDeleteConfirmed : CellViewIntent
    data object OnBulkDeleteDialogDismissed : CellViewIntent
    data object OnRestoreSelectedNodesClick : CellViewIntent
    data object OnBulkRestoreConfirmed : CellViewIntent
    data object OnBulkRestoreDialogDismissed : CellViewIntent
    data object OnEmptyRecycleBinClick : CellViewIntent
    data object OnEmptyRecycleBinConfirmed : CellViewIntent
    data object OnEmptyRecycleBinDialogDismissed : CellViewIntent
    data object OnMakeSelectedNodesAvailableOfflineClick : CellViewIntent
    data object OnBulkOfflineConfirmed : CellViewIntent
    data object OnBulkOfflineDialogDismissed : CellViewIntent
    data object OnRemoveSelectedNodesOfflineAccessClick : CellViewIntent
    data class OnMenuItemActionSelected(val node: CellNodeUi, val action: NodeBottomSheetAction) : CellViewIntent
    data class OnNodeDeleteConfirmed(val node: CellNodeUi) : CellViewIntent
    data class OnNodeRestoreConfirmed(val node: CellNodeUi) : CellViewIntent
    data class OnParentFolderRestoreConfirmed(val node: CellNodeUi) : CellViewIntent
    data class OnCancelDownload(val uuid: String) : CellViewIntent
    data class OnFilesPickedForUpload(val uris: List<Uri>) : CellViewIntent
}

data class MenuOptions(
    val node: CellNodeUi,
    val actions: List<NodeMenuItem>
)
