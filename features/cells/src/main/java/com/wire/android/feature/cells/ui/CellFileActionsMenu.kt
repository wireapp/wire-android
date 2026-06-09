/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
import com.wire.android.feature.cells.ui.model.NodeBottomSheetAction
import com.wire.android.feature.cells.ui.model.isEditSupported
import com.wire.android.feature.cells.ui.model.localFileAvailable
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dev.zacsweers.metro.Inject

@Suppress("CyclomaticComplexMethod", "LongParameterList")
class CellFileActionsMenu @Inject constructor(
    private val featureFlags: KaliumConfigs
) {
    internal fun buildMenu(
        cellNode: CellNodeUi,
        isRecycleBin: Boolean,
        isConversationFiles: Boolean,
        isAllFiles: Boolean,
        isSearching: Boolean,
        isCollaboraEnabled: Boolean,
        isOnline: Boolean = true,
    ): List<NodeBottomSheetAction> {
        if (!isOnline) {
            return buildList {
                val canOpenOffline = cellNode is CellNodeUi.Folder ||
                        (cellNode is CellNodeUi.File && cellNode.localFileAvailable())
                if (canOpenOffline) {
                    add(NodeBottomSheetAction.OPEN)
                }
                if (cellNode is CellNodeUi.File && cellNode.isAvailableOffline) {
                    add(NodeBottomSheetAction.REMOVE_OFFLINE_ACCESS)
                }
            }
        }
        return when {
            isRecycleBin -> recycleBinActions()

            isAllFiles || isSearching -> {
                commonActions(cellNode)
            }

            isConversationFiles -> {
                val common = commonActions(cellNode)
                val isTerminal = cellNode is CellNodeUi.File &&
                        (cellNode.isOpenLoading || cellNode.downloadProgress != null)
                if (isTerminal) {
                    common
                } else {
                    common + conversationActions(
                        cellNode = cellNode,
                        isCollaboraEnabled = isCollaboraEnabled,
                    )
                }
            }

            else -> emptyList()
        }
    }

    private fun recycleBinActions(): List<NodeBottomSheetAction> = listOf(
        NodeBottomSheetAction.RESTORE,
        NodeBottomSheetAction.DELETE_PERMANENTLY,
    )

    private fun commonActions(
        cellNode: CellNodeUi,
    ): List<NodeBottomSheetAction> = buildList {

        if (cellNode is CellNodeUi.File) {

            when {
                cellNode.isOpenLoading -> {
                    add(NodeBottomSheetAction.CANCEL_LOADING)
                    return@buildList
                }

                cellNode.downloadProgress != null -> {
                    add(NodeBottomSheetAction.CANCEL_DOWNLOAD)
                    return@buildList
                }

                else -> {

                    add(NodeBottomSheetAction.OPEN)

                    if (cellNode.localFileAvailable()) {
                        add(NodeBottomSheetAction.SHARE)
                    }

                    add(
                        if (cellNode.isAvailableOffline) {
                            NodeBottomSheetAction.REMOVE_OFFLINE_ACCESS
                        } else {
                            NodeBottomSheetAction.MAKE_AVAILABLE_OFFLINE
                        },
                    )
                }
            }
        } else {
            add(NodeBottomSheetAction.OPEN)
        }
    }

    private fun conversationActions(
        cellNode: CellNodeUi,
        isCollaboraEnabled: Boolean,
    ): List<NodeBottomSheetAction> = buildList {

        val canEdit = cellNode is CellNodeUi.File &&
                isCollaboraEnabled &&
                featureFlags.collaboraIntegration &&
                cellNode.isEditSupported()

        if (canEdit) {
            add(NodeBottomSheetAction.EDIT)
        }

        if (
            cellNode is CellNodeUi.File &&
            featureFlags.collaboraIntegration &&
            cellNode.isEditSupported()
        ) {
            add(NodeBottomSheetAction.VERSION_HISTORY)
        }

        addAll(
            listOf(
                NodeBottomSheetAction.ADD_REMOVE_TAGS,
                NodeBottomSheetAction.PUBLIC_LINK,
                NodeBottomSheetAction.MOVE,
                NodeBottomSheetAction.RENAME,
                NodeBottomSheetAction.DELETE,
            ),
        )
    }

    internal sealed interface MenuActionResult
    internal data class Action(val action: CellViewAction) : MenuActionResult
    internal data class Open(val node: CellNodeUi) : MenuActionResult
    internal data class Share(val node: CellNodeUi.File) : MenuActionResult
    internal data class Edit(val node: CellNodeUi) : MenuActionResult
    internal data class CancelLoading(val node: CellNodeUi) : MenuActionResult
    internal data class CancelDownload(val node: CellNodeUi) : MenuActionResult
    internal data class MakeAvailableOffline(val node: CellNodeUi.File) : MenuActionResult
    internal data class RemoveOfflineAccess(val node: CellNodeUi.File) : MenuActionResult

    internal fun onMenuItemAction(
        conversationId: String?,
        parentFolderUuid: String?,
        node: CellNodeUi,
        action: NodeBottomSheetAction,
        onResult: (MenuActionResult) -> Unit,
    ) {
        val result = when (action) {
            NodeBottomSheetAction.OPEN -> Open(node)
            NodeBottomSheetAction.SHARE -> {
                if (node is CellNodeUi.File) {
                    Share(node)
                } else {
                    Action(ShowPublicLinkScreen(node))
                }
            }

            NodeBottomSheetAction.MOVE -> conversationId?.let {
                Action(
                    ShowMoveToFolderScreen(
                        currentPath = it.substringBefore("/"),
                        nodeToMovePath = "$it/${node.name}",
                        uuid = node.uuid
                    )
                )
            } ?: return

            NodeBottomSheetAction.RESTORE -> {
                if (parentFolderUuid != null) {
                    Action(ShowRestoreParentFolderDialog(node))
                } else {
                    Action(ShowRestoreConfirmation(node = node))
                }
            }

            NodeBottomSheetAction.DELETE_PERMANENTLY -> Action(ShowDeleteConfirmation(node = node, isPermanentDelete = true))
            NodeBottomSheetAction.ADD_REMOVE_TAGS -> Action(ShowAddRemoveTagsScreen(node))
            NodeBottomSheetAction.PUBLIC_LINK -> Action(ShowPublicLinkScreen(node))
            NodeBottomSheetAction.RENAME -> Action(ShowRenameScreen(node))
            NodeBottomSheetAction.DELETE -> Action(ShowDeleteConfirmation(node = node, isPermanentDelete = false))
            NodeBottomSheetAction.EDIT -> Edit(node)
            NodeBottomSheetAction.VERSION_HISTORY -> Action(ShowVersionHistoryScreen(node.uuid, node.name ?: ""))
            NodeBottomSheetAction.CANCEL_LOADING -> CancelLoading(node)
            NodeBottomSheetAction.CANCEL_DOWNLOAD -> CancelDownload(node)
            NodeBottomSheetAction.MAKE_AVAILABLE_OFFLINE -> {
                if (node is CellNodeUi.File) {
                    MakeAvailableOffline(node)
                } else {
                    Action(ShowPublicLinkScreen(node))
                }
            }

            NodeBottomSheetAction.REMOVE_OFFLINE_ACCESS -> {
                if (node is CellNodeUi.File) {
                    RemoveOfflineAccess(node)
                } else {
                    Action(ShowPublicLinkScreen(node))
                }
            }
        }

        onResult(result)
    }
}
