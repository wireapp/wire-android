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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.FileIconPreview
import com.wire.android.feature.cells.ui.MenuOptions
import com.wire.android.feature.cells.ui.model.BottomSheetAction
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.FileAction
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
internal fun FileActionsBottomSheet(
    menuOptions: MenuOptions.FileMenuOptions,
    onAction: (BottomSheetAction) -> Unit,
    onDismiss: () -> Unit,
    sheetState: WireModalSheetState<Unit> = rememberWireModalSheetState<Unit>(WireSheetValue.Expanded(Unit))
) {

    WireModalSheetLayout(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = sheetState
    ) {
        SheetContent(
            menuOptions = menuOptions,
            onAction = { action ->
                onAction(action)
            }
        )
    }
}

@Composable
private fun SheetContent(
    menuOptions: MenuOptions.FileMenuOptions,
    onAction: (BottomSheetAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensions().spacing24x)
    ) {

        Row(
            modifier = Modifier
                .height(dimensions().spacing64x)
                .padding(horizontal = dimensions().spacing8x)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            FileIconPreview(menuOptions.cellNodeUi)

            Text(
                text = menuOptions.cellNodeUi.name ?: "",
                style = typography().title02,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        WireDivider(modifier = Modifier.fillMaxWidth())

        menuOptions.actions.forEach { action ->
            MenuItem(
                modifier = Modifier.clickable { onAction(action) },
                action = action
            )
            WireDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MenuItem(
    action: BottomSheetAction.File,
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
private fun PreviewFileActionsBottomSheet() {
    WireTheme {
        FileActionsBottomSheet(
            sheetState = rememberWireModalSheetState(WireSheetValue.Expanded(value = Unit)),
            menuOptions = MenuOptions.FileMenuOptions(
                cellNodeUi = CellNodeUi.File(
                    uuid = "",
                    name = "test file.pdf",
                    mimeType = "application/pdf",
                    assetType = AttachmentFileType.PDF,
                    assetSize = 2342342,
                    localPath = "",
                    userName = null,
                    conversationName = null,
                    modifiedTime = null
                ),
                actions = listOf(
                    BottomSheetAction.File(FileAction.SHARE),
                    BottomSheetAction.File(FileAction.PUBLIC_LINK),
                    BottomSheetAction.File(FileAction.MOVE),
                    BottomSheetAction.File(FileAction.DELETE),
                )
            ),
            onAction = {},
            onDismiss = {}
        )
    }
}
