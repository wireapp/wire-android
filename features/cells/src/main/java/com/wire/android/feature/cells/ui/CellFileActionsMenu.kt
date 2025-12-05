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
import javax.inject.Inject

class CellFileActionsMenu @Inject constructor(
    private val featureFlags: KaliumConfigs
) {
    internal fun buildMenu(
        cellNode: CellNodeUi,
        isRecycleBin: Boolean,
        isConversationFiles: Boolean,
        isAllFiles: Boolean,
        isSearching: Boolean
    ): List<NodeBottomSheetAction> =
        when {
            isRecycleBin -> {
                buildList {
                    add(NodeBottomSheetAction.RESTORE)
                    add(NodeBottomSheetAction.DELETE_PERMANENTLY)
                }
            }

            isConversationFiles -> {
                buildList {
                    if (cellNode is CellNodeUi.File && cellNode.localFileAvailable()) {
                        add(NodeBottomSheetAction.SHARE)
                    }
                    add(NodeBottomSheetAction.PUBLIC_LINK)
                    add(NodeBottomSheetAction.DOWNLOAD)

                    if (featureFlags.collaboraIntegration && cellNode.isEditSupported()) {
                        add(NodeBottomSheetAction.EDIT)
                    }

                    if (featureFlags.collaboraIntegration && cellNode.isEditSupported()) {
                        add(NodeBottomSheetAction.VERSION_HISTORY)
                    }

                    add(NodeBottomSheetAction.ADD_REMOVE_TAGS)
                    add(NodeBottomSheetAction.MOVE)
                    add(NodeBottomSheetAction.RENAME)
                    add(NodeBottomSheetAction.DELETE)
                }
            }

            isAllFiles || isSearching -> {
                buildList {
                    if (cellNode is CellNodeUi.File && cellNode.localFileAvailable()) {
                        add(NodeBottomSheetAction.SHARE)
                    }
                    add(NodeBottomSheetAction.PUBLIC_LINK)
                    add(NodeBottomSheetAction.DOWNLOAD)
                }
            }

            else -> {
                emptyList()
            }
        }

    internal sealed interface MenuActionResult
    internal data class Action(val action: CellViewAction) : MenuActionResult
    internal data class Share(val node: CellNodeUi.File) : MenuActionResult
    internal data class Download(val node: CellNodeUi) : MenuActionResult
    internal data class Edit(val node: CellNodeUi) : MenuActionResult

    @Suppress("CyclomaticComplexMethod")
    internal fun onMenuItemAction(
        conversationId: String?,
        parentFolderUuid: String?,
        node: CellNodeUi,
        action: NodeBottomSheetAction,
        onResult: (MenuActionResult) -> Unit,
    ) {
        val result = when (action) {
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
            NodeBottomSheetAction.DOWNLOAD -> Download(node)
            NodeBottomSheetAction.EDIT -> Edit(node)
            NodeBottomSheetAction.VERSION_HISTORY -> Action(ShowVersionHistoryScreen(node.uuid))
        }

        onResult(result)
    }
}
