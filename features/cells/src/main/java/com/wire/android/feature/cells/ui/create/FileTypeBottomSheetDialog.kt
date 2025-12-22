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
package com.wire.android.feature.cells.ui.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.create.createfile.FileType
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun FileTypeBottomSheetDialog(
    sheetState: WireModalSheetState<Unit>,
    onDismiss: () -> Unit,
    onItemSelected: (FileType) -> Unit,
) {
    WireModalSheetLayout(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        WireMenuModalSheetContent(
            menuItems = buildList {
                add {
                    BottomSheetItem(
                        title = stringResource(R.string.file_type_bottom_sheet_document),
                        icon = R.drawable.ic_file_type_doc,
                        onClicked = { onItemSelected(FileType.DOCUMENT) },
                    )
                }
                add {
                    BottomSheetItem(
                        title = stringResource(R.string.file_type_bottom_sheet_spreadsheet),
                        icon = R.drawable.ic_file_type_spreadsheet,
                        onClicked = { onItemSelected(FileType.SPREADSHEET) },
                    )
                }
                add {
                    BottomSheetItem(
                        title = stringResource(R.string.file_type_bottom_sheet_presentation),
                        icon = R.drawable.ic_file_type_presentation,
                        onClicked = { onItemSelected(FileType.PRESENTATION) },
                    )
                }
            }
        )
    }
}

@Composable
private fun BottomSheetItem(
    title: String,
    icon: Int = R.drawable.ic_folder,
    onClicked: () -> Unit,
) {
    MenuBottomSheetItem(
        title = title,
        onItemClick = onClicked,
        leading = {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier
                    .size(MaterialTheme.wireDimensions.wireIconButtonSize)
            )
        },
    )
}

@MultipleThemePreviews
@Composable
fun PreviewFileTypeBottomSheetDialog() {
    WireTheme {
        FileTypeBottomSheetDialog(
            sheetState = rememberWireModalSheetState(WireSheetValue.Expanded(value = Unit)),
            onDismiss = {},
            onItemSelected = {},
        )
    }
}
