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
internal fun CellsOptionsBottomSheet(
    sheetState: WireModalSheetState<Unit>,
    onDismiss: () -> Unit,
    showRecycleBin: () -> Unit,
) {
    WireModalSheetLayout(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = sheetState
    ) {
        WireMenuModalSheetContent(
            menuItems = buildList {
                add {
                    ShowRecycleBinItem(
                        title = stringResource(R.string.open_recycle_bin),
                        onClicked = showRecycleBin,
                        enabled = true
                    )
                }
            }
        )
    }
}

@Composable
private fun ShowRecycleBinItem(
    title: String,
    onClicked: () -> Unit,
    enabled: Boolean,
) {
    MenuBottomSheetItem(
        title = title,
        onItemClick = onClicked,
        leading = {
            Icon(
                painter = painterResource(id = com.wire.android.ui.common.R.drawable.ic_delete),
                contentDescription = null,
                tint = colorsScheme().onBackground,
                modifier = Modifier
                    .size(MaterialTheme.wireDimensions.wireIconButtonSize)
            )
        },
        enabled = enabled
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewCellsOptionsBottomSheet() {
    WireTheme {
        CellsOptionsBottomSheet(
            sheetState = rememberWireModalSheetState(WireSheetValue.Expanded(value = Unit)),
            onDismiss = {},
            showRecycleBin = {}
        )
    }
}
