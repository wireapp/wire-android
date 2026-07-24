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

import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.NodeBottomSheetAction
import com.wire.android.feature.cells.ui.model.NodeMenuItem
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CellFileActionsMenuTest {

    @Test
    fun `GIVEN Search context AND File node with local file available WHEN onItemMenuClick called THEN emits SHARE PUBLIC_LINK actions`() =
        runTest {

            // WHEN
            val items = buildMenu(
                isAllFiles = true,
                isSearching = true,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.PUBLIC_LINK,
                ),
                items
            )
        }

    @Test
    fun `GIVEN RecycleBin context WHEN onItemMenuClick called THEN emits RESTORE DELETE_PERMANENTLY actions`() = runTest {

        // WHEN
        val items = buildMenu(
            isRecycleBin = true,
            isConversationFiles = true,
        )

        // THEN
        assertEquals(
            listOf(
                NodeBottomSheetAction.RESTORE,
                NodeBottomSheetAction.DELETE_PERMANENTLY,
            ),
            items
        )
    }

    @Test
    fun `GIVEN Conversation context AND File node with local file available WHEN onItemMenuClick called THEN emits all conversation actions`() =
        runTest {

            // GIVEN
            val fileWithNullLocalPath = fileNode.copy(localPath = null)

            // WHEN
            val items = buildMenu(
                fileNode = fileWithNullLocalPath,
                isConversationFiles = true,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.ADD_REMOVE_TAGS,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.MOVE,
                    NodeBottomSheetAction.RENAME,
                    NodeBottomSheetAction.DELETE,
                ),
                items
            )
        }

    @Test
    fun `GIVEN AllFiles context AND File node with local file available WHEN onItemMenuClick called THEN emits SHARE PUBLIC_LINK actions`() =
        runTest {

            // WHEN
            val items = buildMenu(
                isAllFiles = true,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.PUBLIC_LINK,
                ),
                items
            )
        }

    @Test
    fun `GIVEN ConversationFiles context AND File supports edit AND collabora enabled WHEN onItemMenuClick called THEN emits correct actions`() =
        runTest {

            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(isEditSupported = true),
                withCollaboraIntegration = true,
                isConversationFiles = true,
                isCollaboraEnabled = true,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.EDIT,
                    NodeBottomSheetAction.VERSION_HISTORY,
                    NodeBottomSheetAction.ADD_REMOVE_TAGS,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.MOVE,
                    NodeBottomSheetAction.RENAME,
                    NodeBottomSheetAction.DELETE,
                ),
                items
            )
        }

    @Test
    fun `GIVEN ConversationFiles context AND File NOT supports edit AND collabora enabled WHEN onItemMenuClick called THEN emits correct actions`() =
        runTest {

            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(isEditSupported = false),
                withCollaboraIntegration = true,
                isConversationFiles = true,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.MAKE_AVAILABLE_OFFLINE,
                    NodeBottomSheetAction.ADD_REMOVE_TAGS,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.MOVE,
                    NodeBottomSheetAction.RENAME,
                    NodeBottomSheetAction.DELETE,
                ),
                items
            )
        }

    @Test
    fun `GIVEN file menu WHEN share option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.SHARE,
                onResult = { result ->

                    // THEN
                    assertEquals(CellFileActionsMenu.Share(fileNode), result)
                }
            )
        }

    @Test
    fun `GIVEN folder menu WHEN share option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = folderNode,
                action = NodeBottomSheetAction.SHARE,
                onResult = { result ->

                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Action(ShowPublicLinkScreen(folderNode)),
                        result
                    )
                }
            )
        }

    @Test
    fun `GIVEN file menu WHEN move option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = "conversation/path",
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.MOVE,
                onResult = { result ->

                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Action(
                            ShowMoveToFolderScreen(
                                currentPath = "conversation",
                                nodeToMovePath = "conversation/path/file.txt",
                                uuid = fileNode.uuid
                            )
                        ),
                        result
                    )
                }
            )
        }

    @Test
    fun `GIVEN recycle bin menu WHEN restore item in folder option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = "parentFolder",
                node = fileNode,
                action = NodeBottomSheetAction.RESTORE,
                onResult = { result ->

                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Action(ShowRestoreParentFolderDialog(fileNode)),
                        result
                    )
                }
            )
        }

    @Test
    fun `GIVEN recycle bin menu WHEN restore item option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.RESTORE,
                onResult = { result ->

                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Action(ShowRestoreConfirmation(fileNode)),
                        result
                    )
                }
            )
        }

    @Test
    fun `GIVEN recycle bin menu WHEN delete item option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.DELETE_PERMANENTLY,
                onResult = { result ->
                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Action(ShowDeleteConfirmation(fileNode, true)),
                        result
                    )
                }
            )
        }

    @Test
    fun `GIVEN file menu WHEN tags item option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.ADD_REMOVE_TAGS,
                onResult = { result ->
                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Action(ShowAddRemoveTagsScreen(fileNode)),
                        result
                    )
                }
            )
        }

    @Test
    fun `GIVEN file menu WHEN public link option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.PUBLIC_LINK,
                onResult = { result ->
                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Action(ShowPublicLinkScreen(fileNode)),
                        result
                    )
                }
            )
        }

    @Test
    fun `GIVEN file menu WHEN rename option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.RENAME,
                onResult = { result ->
                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Action(ShowRenameScreen(fileNode)),
                        result
                    )
                }
            )
        }

    @Test
    fun `GIVEN file menu WHEN delete option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.DELETE,
                onResult = { result ->
                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Action(ShowDeleteConfirmation(fileNode, false)),
                        result
                    )
                }
            )
        }

    @Test
    fun `GIVEN file is downloading offline WHEN building allFiles menu THEN emits only CANCEL_DOWNLOAD`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(downloadProgress = 0.5f),
                isAllFiles = true,
            )

            // THEN
            assertEquals(
                listOf(NodeBottomSheetAction.CANCEL_DOWNLOAD),
                items
            )
        }

    @Test
    fun `GIVEN file is downloading offline WHEN building conversationFiles menu THEN emits only CANCEL_DOWNLOAD`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(downloadProgress = 0.5f),
                isConversationFiles = true,
            )

            // THEN
            assertEquals(
                listOf(NodeBottomSheetAction.CANCEL_DOWNLOAD),
                items
            )
        }

    @Test
    fun `GIVEN file menu WHEN cancel download option selected THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.CANCEL_DOWNLOAD,
                onResult = { result ->
                    // THEN
                    assertEquals(CellFileActionsMenu.CancelDownload(fileNode), result)
                }
            )
        }

    @Test
    fun `GIVEN file is available offline WHEN building allFiles menu THEN emits REMOVE_OFFLINE_ACCESS instead of MAKE_AVAILABLE_OFFLINE`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(isAvailableOffline = true),
                isAllFiles = true,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.REMOVE_OFFLINE_ACCESS,
                ),
                items
            )
        }

    @Test
    fun `GIVEN file is available offline WHEN building conversationFiles menu THEN emits REMOVE_OFFLINE_ACCESS instead of MAKE_AVAILABLE_OFFLINE`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(isAvailableOffline = true),
                isConversationFiles = true,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.REMOVE_OFFLINE_ACCESS,
                    NodeBottomSheetAction.ADD_REMOVE_TAGS,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.MOVE,
                    NodeBottomSheetAction.RENAME,
                    NodeBottomSheetAction.DELETE,
                ),
                items
            )
        }

    @Test
    fun `GIVEN file menu WHEN remove offline access option selected THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.REMOVE_OFFLINE_ACCESS,
                onResult = { result ->
                    // THEN
                    assertEquals(CellFileActionsMenu.RemoveOfflineAccess(fileNode), result)
                }
            )
        }

    @Test
    fun `GIVEN file menu WHEN edit option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.EDIT,
                onResult = { result ->
                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Edit(fileNode),
                        result
                    )
                }
            )
        }

    @Test
    fun `GIVEN offlineFilesEnabled AND file not available offline AND collabora enabled AND edit NOT supported WHEN building menu THEN MAKE_AVAILABLE_OFFLINE is added`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(isEditSupported = false),
                withCollaboraIntegration = true,
                isAllFiles = true,
            )

            // THEN
            assert(NodeBottomSheetAction.MAKE_AVAILABLE_OFFLINE in items)
            assert(NodeBottomSheetAction.REMOVE_OFFLINE_ACCESS !in items)
        }

    @Test
    fun `GIVEN offlineFilesEnabled AND file not available offline AND collabora enabled AND edit IS supported WHEN building menu THEN no offline action is added`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(isEditSupported = true),
                withCollaboraIntegration = true,
                isAllFiles = true,
            )

            // THEN
            assert(NodeBottomSheetAction.MAKE_AVAILABLE_OFFLINE !in items)
            assert(NodeBottomSheetAction.REMOVE_OFFLINE_ACCESS !in items)
        }

    @Test
    fun `GIVEN offline files disabled WHEN building allFiles menu THEN no offline action is added`() =
        runTest {
            // WHEN
            val items = buildMenu(
                isAllFiles = true,
                offlineFilesEnabled = false,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.PUBLIC_LINK,
                ),
                items
            )
        }

    @Test
    fun `GIVEN offline files disabled AND file available offline WHEN building conversation menu THEN no offline action is added`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(isAvailableOffline = true),
                isConversationFiles = true,
                offlineFilesEnabled = false,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.ADD_REMOVE_TAGS,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.MOVE,
                    NodeBottomSheetAction.RENAME,
                    NodeBottomSheetAction.DELETE,
                ),
                items
            )
        }

    @Test
    fun `GIVEN offline files enabled AND offline AND file available offline WHEN building menu THEN emits OPEN and REMOVE_OFFLINE_ACCESS`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(isAvailableOffline = true),
                isConversationFiles = true,
                isOnline = false,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.REMOVE_OFFLINE_ACCESS,
                ),
                items
            )
        }

    @Test
    fun `GIVEN offline files disabled AND offline WHEN building menu THEN falls through to online menu`() =
        runTest {
            // WHEN
            val items = buildMenu(
                isConversationFiles = true,
                isOnline = false,
                offlineFilesEnabled = false,
            )

            // THEN - offline branch skipped, normal conversation menu without offline action
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.ADD_REMOVE_TAGS,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.MOVE,
                    NodeBottomSheetAction.RENAME,
                    NodeBottomSheetAction.DELETE,
                ),
                items
            )
        }

    @Test
    fun `GIVEN AllFiles context AND viewer-only file WHEN building menu THEN restricted actions are present but disabled`() =
        runTest {
            // WHEN
            val items = buildMenuItems(
                fileNode = fileNode.copy(isEditSupported = false, isViewerOnly = true),
                withCollaboraIntegration = true,
                isAllFiles = true,
            )

            // THEN - viewer-only file keeps Open enabled, and shows the restricted actions grayed out
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.MAKE_AVAILABLE_OFFLINE,
                ),
                items.map { it.action }
            )
            assertTrue(items.first { it.action == NodeBottomSheetAction.OPEN }.enabled)
            assertFalse(items.first { it.action == NodeBottomSheetAction.SHARE }.enabled)
            assertFalse(items.first { it.action == NodeBottomSheetAction.PUBLIC_LINK }.enabled)
            assertFalse(items.first { it.action == NodeBottomSheetAction.MAKE_AVAILABLE_OFFLINE }.enabled)
        }

    @Test
    fun `GIVEN AllFiles context AND non viewer-only file WHEN building menu THEN restricted actions are enabled`() =
        runTest {
            // WHEN
            val items = buildMenuItems(
                fileNode = fileNode.copy(isEditSupported = false, isViewerOnly = false),
                withCollaboraIntegration = true,
                isAllFiles = true,
            )

            // THEN
            assertTrue(items.first { it.action == NodeBottomSheetAction.SHARE }.enabled)
            assertTrue(items.first { it.action == NodeBottomSheetAction.PUBLIC_LINK }.enabled)
            assertTrue(items.first { it.action == NodeBottomSheetAction.MAKE_AVAILABLE_OFFLINE }.enabled)
        }

    @Test
    fun `GIVEN ConversationFiles context AND viewer-only file WHEN building menu THEN only Open remains`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = fileNode.copy(isEditSupported = false, isViewerOnly = true),
                withCollaboraIntegration = true,
                isConversationFiles = true,
            )

            // THEN - all sharing / management actions are removed, only Open remains
            assertEquals(
                listOf(NodeBottomSheetAction.OPEN),
                items
            )
        }

    @Test
    fun `GIVEN ConversationFiles context AND viewer-only folder WHEN building menu THEN only Open remains`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = folderNode.copy(isViewerOnly = true),
                isConversationFiles = true,
            )

            // THEN - management actions are removed for viewer-only folders too
            assertEquals(
                listOf(NodeBottomSheetAction.OPEN),
                items
            )
        }

    @Test
    fun `GIVEN ConversationFiles context AND non viewer-only folder WHEN building menu THEN management actions remain`() =
        runTest {
            // WHEN
            val items = buildMenu(
                fileNode = folderNode.copy(isViewerOnly = false),
                isConversationFiles = true,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.OPEN,
                    NodeBottomSheetAction.ADD_REMOVE_TAGS,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.MOVE,
                    NodeBottomSheetAction.RENAME,
                    NodeBottomSheetAction.DELETE,
                ),
                items
            )
        }

    private fun actionsMenu(
        withCollaboraIntegration: Boolean = false,
        offlineFilesEnabled: Boolean = true,
    ) = CellFileActionsMenu(
        featureFlags = KaliumConfigs(
            collaboraIntegration = withCollaboraIntegration,
        ),
        offlineFilesEnabled = offlineFilesEnabled,
    )

    private fun buildMenu(
        fileNode: CellNodeUi = Companion.fileNode,
        withCollaboraIntegration: Boolean = false,
        offlineFilesEnabled: Boolean = true,
        isRecycleBin: Boolean = false,
        isConversationFiles: Boolean = false,
        isAllFiles: Boolean = false,
        isSearching: Boolean = false,
        isCollaboraEnabled: Boolean = false,
        isOnline: Boolean = true,
    ): List<NodeBottomSheetAction> =
        buildMenuItems(
            fileNode = fileNode,
            withCollaboraIntegration = withCollaboraIntegration,
            offlineFilesEnabled = offlineFilesEnabled,
            isRecycleBin = isRecycleBin,
            isConversationFiles = isConversationFiles,
            isAllFiles = isAllFiles,
            isSearching = isSearching,
            isCollaboraEnabled = isCollaboraEnabled,
            isOnline = isOnline,
        ).map { it.action }

    @Suppress("LongParameterList")
    private fun buildMenuItems(
        fileNode: CellNodeUi = Companion.fileNode,
        withCollaboraIntegration: Boolean = false,
        offlineFilesEnabled: Boolean = true,
        isRecycleBin: Boolean = false,
        isConversationFiles: Boolean = false,
        isAllFiles: Boolean = false,
        isSearching: Boolean = false,
        isCollaboraEnabled: Boolean = false,
        isOnline: Boolean = true,
    ): List<NodeMenuItem> =
        CellFileActionsMenu(
            featureFlags = KaliumConfigs(
                collaboraIntegration = withCollaboraIntegration,
            ),
            offlineFilesEnabled = offlineFilesEnabled,
        ).buildMenu(fileNode, isRecycleBin, isConversationFiles, isAllFiles, isSearching, isCollaboraEnabled, isOnline)

    private companion object {
        val fileNode = CellNodeUi.File(
            name = "file.txt",
            conversationId = "conversationId",
            conversationName = "Conversation",
            uuid = "fileUuid",
            mimeType = "video/mp4",
            assetType = AttachmentFileType.VIDEO,
            size = 23432532532,
            localPath = "localPath",
            userName = null,
            modifiedTime = null,
            remotePath = null,
            contentHash = null,
            contentUrl = null,
            previewUrl = null,
            publicLinkId = null,
            userHandle = null,
            ownerUserId = null,
        )
        val folderNode = CellNodeUi.Folder(
            name = "folder",
            uuid = "uuid",
            userName = "user",
            conversationName = "conversation",
            modifiedTime = 1696154400000L,
            size = 1,
            userHandle = null,
            ownerUserId = null,
        )
    }
}
