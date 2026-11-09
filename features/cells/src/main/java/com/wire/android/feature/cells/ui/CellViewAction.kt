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
import com.wire.android.feature.cells.ui.movetofolder.NodeToMove

sealed interface CellViewAction
internal data class ShowDeleteConfirmation(val node: CellNodeUi, val isPermanentDelete: Boolean) : CellViewAction
internal data object HideDeleteConfirmation : CellViewAction
internal data class ShowRestoreConfirmation(val node: CellNodeUi) : CellViewAction
internal data object HideRestoreConfirmation : CellViewAction
internal data class ShowError(val error: CellError) : CellViewAction
internal data class ShowPublicLinkScreen(val cellNode: CellNodeUi) : CellViewAction
internal data class ShowRenameScreen(val cellNode: CellNodeUi) : CellViewAction
internal data class ShowAddRemoveTagsScreen(val cellNode: CellNodeUi) : CellViewAction
internal data class ShowMoveToFolderScreen(val currentPath: String, val nodeToMovePath: String, val uuid: String) : CellViewAction
internal data class ShowBulkMoveToFolderScreen(val currentPath: String, val nodes: List<NodeToMove>) : CellViewAction
internal data class ShowVersionHistoryScreen(val uuid: String, val fileName: String) : CellViewAction
internal data class ShowUnableToRestoreDialog(val isFolder: Boolean) : CellViewAction
internal data class ShowRestoreParentFolderDialog(val cellNode: CellNodeUi) : CellViewAction
internal data object HideRestoreParentFolderDialog : CellViewAction
internal data class ShowFileDeletedMessage(val isFile: Boolean, val permanently: Boolean) : CellViewAction
internal data class ShowNodesDeletedMessage(val deletedCount: Int, val permanently: Boolean) : CellViewAction
internal data class ShowNodesRestoredMessage(val restoredCount: Int) : CellViewAction
internal data object ShowRecycleBinEmptiedMessage : CellViewAction
internal data object RefreshData : CellViewAction
internal data class OpenFolder(val path: String, val title: String, val parentFolderUuid: String?) : CellViewAction
internal data class ShowEditErrorDialog(val nodeUuid: String) : CellViewAction
internal data object ShowOfflineFileSaved : CellViewAction
internal data class OpenImageViewer(val file: CellNodeUi.File) : CellViewAction
internal data class OpenVideoViewer(val file: CellNodeUi.File) : CellViewAction
internal data class OpenAudioPlayer(val file: CellNodeUi.File) : CellViewAction
internal data class OpenPdfViewer(val file: CellNodeUi.File) : CellViewAction
internal data class FilesPickedForUpload(val uris: List<Uri>) : CellViewAction
