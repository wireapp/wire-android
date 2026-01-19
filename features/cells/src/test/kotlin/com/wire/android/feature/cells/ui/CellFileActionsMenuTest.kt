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
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CellFileActionsMenuTest {

    @Test
    fun `GIVEN Search context AND File node with local file available WHEN onItemMenuClick called THEN emits SHARE PUBLIC_LINK DOWNLOAD actions`() =
        runTest {

            // WHEN
            val items = buildMenu(
                isAllFiles = true,
                isSearching = true,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.DOWNLOAD
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
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.DOWNLOAD,
                    NodeBottomSheetAction.ADD_REMOVE_TAGS,
                    NodeBottomSheetAction.MOVE,
                    NodeBottomSheetAction.RENAME,
                    NodeBottomSheetAction.DELETE,
                ),
                items
            )
        }

    @Test
    fun `GIVEN AllFiles context AND File node with local file available WHEN onItemMenuClick called THEN emits SHARE PUBLIC_LINK DOWNLOAD actions`() =
        runTest {

            // WHEN
            val items = buildMenu(
                isAllFiles = true,
            )

            // THEN
            assertEquals(
                listOf(
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.DOWNLOAD
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
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.DOWNLOAD,
                    NodeBottomSheetAction.EDIT,
                    NodeBottomSheetAction.VERSION_HISTORY,
                    NodeBottomSheetAction.ADD_REMOVE_TAGS,
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
                    NodeBottomSheetAction.SHARE,
                    NodeBottomSheetAction.PUBLIC_LINK,
                    NodeBottomSheetAction.DOWNLOAD,
                    NodeBottomSheetAction.ADD_REMOVE_TAGS,
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
    fun `GIVEN file menu WHEN download option selected called THEN correct action emitted`() =
        runTest {
            // GIVEN
            val menu = actionsMenu()

            // WHEN
            menu.onMenuItemAction(
                conversationId = null,
                parentFolderUuid = null,
                node = fileNode,
                action = NodeBottomSheetAction.DOWNLOAD,
                onResult = { result ->
                    // THEN
                    assertEquals(
                        CellFileActionsMenu.Download(fileNode),
                        result
                    )
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

    private fun actionsMenu(
        withCollaboraIntegration: Boolean = false,
    ) = CellFileActionsMenu(
        featureFlags = KaliumConfigs(
            collaboraIntegration = withCollaboraIntegration
        )
    )

    @Suppress("LongParameterList")
    private fun buildMenu(
        fileNode: CellNodeUi = Companion.fileNode,
        withCollaboraIntegration: Boolean = false,
        isRecycleBin: Boolean = false,
        isConversationFiles: Boolean = false,
        isAllFiles: Boolean = false,
        isSearching: Boolean = false,
        isCollaboraEnabled: Boolean = false,
    ): List<NodeBottomSheetAction> =
        CellFileActionsMenu(
            featureFlags = KaliumConfigs(
                collaboraIntegration = withCollaboraIntegration
            )
        ).buildMenu(fileNode, isRecycleBin, isConversationFiles, isAllFiles, isSearching, isCollaboraEnabled)

    private companion object {
        val fileNode = CellNodeUi.File(
            name = "file.txt",
            conversationName = "Conversation",
            downloadProgress = null,
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
            publicLinkId = null
        )
        val folderNode = CellNodeUi.Folder(
            name = "folder",
            uuid = "uuid",
            userName = "user",
            conversationName = "conversation",
            modifiedTime = "time",
            size = 1
        )
    }
}
