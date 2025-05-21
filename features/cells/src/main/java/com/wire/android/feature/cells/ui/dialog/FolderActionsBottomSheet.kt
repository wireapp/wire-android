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
package com.wire.android.feature.cells.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.wire.android.feature.cells.ui.MenuOptions
import com.wire.android.feature.cells.ui.model.BottomSheetAction
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.FolderAction
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.theme.WireTheme
import kotlinx.coroutines.launch

@Composable
internal fun FolderActionsBottomSheet(
    menuOptions: MenuOptions.FolderMenuOptions,
    onAction: (BottomSheetAction.Folder) -> Unit,
    onDismiss: () -> Unit,
    sheetState: WireModalSheetState<Unit> = rememberWireModalSheetState<Unit>(WireSheetValue.Expanded(Unit))
) {
    val scope = rememberCoroutineScope()

    WireModalSheetLayout(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensions().spacing24x)
        ) {

            WireDivider(modifier = Modifier.fillMaxWidth())

            menuOptions.actions.forEach { action ->
                BottomSheetMenuItem(
                    modifier = Modifier.clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onAction(action)
                            }
                        }
                    },
                    action = action
                )
                WireDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewFolderActionsBottomSheet() {
    WireTheme {
        FolderActionsBottomSheet(
            sheetState = rememberWireModalSheetState(WireSheetValue.Expanded(value = Unit)),
            menuOptions = MenuOptions.FolderMenuOptions(
                cellNodeUi = CellNodeUi.Folder(
                    uuid = "243567990900989897",
                    name = "some folder.pdf",
                    userName = "User",
                    conversationName = "Conversation",
                    modifiedTime = null,
                    size = null
                ),
                actions = listOf(
                    BottomSheetAction.Folder(FolderAction.MOVE),
                    BottomSheetAction.Folder(FolderAction.SHARE),
                    BottomSheetAction.Folder(FolderAction.DOWNLOAD),
                    BottomSheetAction.Folder(FolderAction.DELETE),
                )
            ),
            onAction = {},
            onDismiss = {}
        )
    }
}
