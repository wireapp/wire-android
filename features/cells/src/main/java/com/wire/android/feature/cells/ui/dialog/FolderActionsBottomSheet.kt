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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.ui.MenuOptions
import com.wire.android.feature.cells.ui.model.BottomSheetAction
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.FolderAction
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme

@Composable
internal fun FolderActionsBottomSheet(
    menuOptions: MenuOptions.FolderMenuOptions,
    onAction: (BottomSheetAction.Folder) -> Unit,
    onDismiss: () -> Unit,
    sheetState: WireModalSheetState<Unit> = rememberWireModalSheetState<Unit>(WireSheetValue.Expanded(Unit))
) {

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
                MenuItem(
                    modifier = Modifier.clickable {
                        onAction(action)
                    },
                    action = action
                )
                WireDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MenuItem(
    action: BottomSheetAction.Folder,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = dimensions().spacing16x)
            .height(dimensions().spacing48x)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {
        Image(
            painter = painterResource(action.data.icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                color = if (action.data.isHighlighted) colorsScheme().error else colorsScheme().onSurface
            )
        )
        Text(
            text = stringResource(action.data.title),
            style = typography().body01,
            color = if (action.data.isHighlighted) colorsScheme().error else typography().body01.color
        )
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
                    contents = listOf(),
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
