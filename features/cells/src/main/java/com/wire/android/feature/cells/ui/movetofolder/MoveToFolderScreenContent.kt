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
package com.wire.android.feature.cells.ui.movetofolder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.feature.cells.ui.FileIconPreview
import com.wire.android.feature.cells.ui.FolderIconPreview
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.ui.common.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
internal fun MoveToFolderScreenContent(
    folders: List<CellNodeUi>,
    onFolderClick: (CellNodeUi.Folder) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing2x)
    ) {
        folders.forEach { node ->
            item(
                key = node.uuid
            ) {
                RowItem(node) {
                    onFolderClick(it)
                }
            }
        }
    }
}

@Composable
private fun RowItem(
    cell: CellNodeUi,
    onFolderClick: (CellNodeUi.Folder) -> Unit = { }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing56x)
            .background(colorsScheme().surface)
            .padding(
                bottom = dimensions().spacing2x,
                end = dimensions().spacing12x,
            )
            .alpha(if (cell is CellNodeUi.Folder) 1f else 0.5f)
            .then(
                if (cell is CellNodeUi.Folder) {
                    Modifier.clickable { onFolderClick(cell) }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (cell) {
            is CellNodeUi.File -> FileIconPreview(cell)
            is CellNodeUi.Folder -> FolderIconPreview(cell)
        }
        Text(
            cell.name ?: "",
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        if (cell is CellNodeUi.Folder) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward_ios),
                tint = colorsScheme().onSurface,
                contentDescription = null,
            )
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewMoveToFolderItem() {
    WireTheme {
        RowItem(
            CellNodeUi.Folder(
                uuid = "243567990900989897",
                name = "some folder.pdf",
                userName = "User",
                conversationName = "Conversation",
                modifiedTime = null,
                size = 1234,
                publicLinkId = "public"
            )
        )
    }
}
