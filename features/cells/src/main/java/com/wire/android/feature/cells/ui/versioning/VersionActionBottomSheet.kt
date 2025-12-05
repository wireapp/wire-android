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
package com.wire.android.feature.cells.ui.versioning

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun VersionActionBottomSheet(
    sheetState: WireModalSheetState<CellVersion>,
    onDismiss: () -> Unit,
    onRestoreVersionClicked: (String) -> Unit,
    onDownloadVersionClicked: (String) -> Unit,
) {
    WireModalSheetLayout(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) { state ->
        WireMenuModalSheetContent(
            menuItems = buildList {
                add {
                    RestoreVersionModalItem(
                        title = stringResource(R.string.restore_version_bottom_sheet_item_label),
                        onClicked = { onRestoreVersionClicked(state.versionId) },
                    )
                }
                add {
                    DownloadVersionModalItem(
                        title = stringResource(R.string.download_version_bottom_sheet_item_label),
                        onClicked = { onDownloadVersionClicked(state.versionId) },
                    )
                }
            }
        )
    }
}

@Composable
private fun RestoreVersionModalItem(
    title: String,
    onClicked: () -> Unit,
) {
    MenuBottomSheetItem(
        title = title,
        onItemClick = onClicked,
        leading = {
            Icon(
                painter = painterResource(id = R.drawable.ic_undo),
                contentDescription = null,
                tint = colorsScheme().onBackground,
                modifier = Modifier.Companion
                    .size(MaterialTheme.wireDimensions.wireIconButtonSize)
            )
        },
    )
}

@Composable
private fun DownloadVersionModalItem(
    title: String,
    onClicked: () -> Unit,
) {
    MenuBottomSheetItem(
        title = title,
        onItemClick = onClicked,
        leading = {
            Icon(
                painter = painterResource(id = R.drawable.ic_save),
                contentDescription = null,
                tint = colorsScheme().onBackground,
                modifier = Modifier.Companion
                    .size(MaterialTheme.wireDimensions.wireIconButtonSize)
            )
        },
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewCellsOptionsBottomSheet() {
    WireTheme {
        VersionActionBottomSheet(
            sheetState = rememberWireModalSheetState(WireSheetValue.Expanded(value = CellVersion())),
            onDismiss = {},
            onRestoreVersionClicked = {},
            onDownloadVersionClicked = {}
        )
    }
}
