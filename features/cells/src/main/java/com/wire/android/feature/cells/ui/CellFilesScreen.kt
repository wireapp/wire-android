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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.model.CellFileUi
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme

@Composable
internal fun CellFilesScreen(
    files: List<CellFileUi>,
    onFileClick: (CellFileUi) -> Unit,
    onFileMenuClick: (CellFileUi) -> Unit,
//    onRefresh: () -> Unit
) {

//    TODO: Enable PullToRefresh support on HomeScreen level?
//    PullToRefreshBox(
//        isRefreshing = state.refreshing,
//        onRefresh = { onRefresh() }
//    ) {
    LazyColumn(
        modifier = Modifier
            .background(color = colorsScheme().surface)
            .fillMaxWidth(),
    ) {
        items(
            items = files,
            key = { it.uuid },
        ) { file ->
            CellListItem(
                modifier = Modifier
                    .animateItem()
                    .clickable { onFileClick(file) },
                file = file,
                onMenuClick = { onFileMenuClick(file) }
            )
            WireDivider(modifier = Modifier.fillMaxWidth())
        }
    }
//    }
}

@MultipleThemePreviews
@Composable
fun PreviewCellFilesScreen() {
    WireTheme {
        CellFilesScreen(
            files = listOf(
                CellFileUi(
                    uuid = "uuid",
                    fileName = "Image name",
                    mimeType = "image/png",
                    assetType = AttachmentFileType.IMAGE,
                    assetSize = 1234L,
                    localPath = "path/to/local/file",
                ),
                CellFileUi(
                    uuid = "uuid2",
                    fileName = "PDF name",
                    mimeType = "application/pdf",
                    assetType = AttachmentFileType.PDF,
                    assetSize = 99234L,
                    localPath = "path/to/local/file",
                )
            ),
            onFileClick = {},
            onFileMenuClick = {}
        )
    }
}
