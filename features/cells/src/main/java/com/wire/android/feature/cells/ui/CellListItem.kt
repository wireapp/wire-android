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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.domain.model.icon
import com.wire.android.feature.cells.domain.model.previewSupported
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.chip.WireDisplayChipWithOverFlow
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme

@Composable
internal fun CellListItem(
    cell: CellNodeUi,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .height(dimensions().spacing64x)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            if (cell is CellNodeUi.File) {
                FileIconPreview(cell)
            } else {
                FolderIconPreview()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions().spacing2x)
            ) {

                Text(
                    text = cell.name ?: "",
                    style = typography().title02,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (cell.tags.isNotEmpty()) {
                        WireDisplayChipWithOverFlow(
                            label = cell.tags.first(),
                            chipsCount = cell.tags.size - 1,
                            modifier = Modifier.padding(end = dimensions().spacing4x)
                        )
                    }

                    cell.subtitle()?.let {
                        Text(
                            text = it,
                            textAlign = TextAlign.Left,
                            overflow = TextOverflow.Ellipsis,
                            style = typography().label04,
                            color = colorsScheme().secondaryText,
                            maxLines = 1,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(dimensions().spacing56x)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                )
            }
        }
        cell.downloadProgress?.let {
            WireLinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart),
                progress = { it },
                color = colorsScheme().primary,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
internal fun FolderIconPreview() {
    Box(
        modifier = Modifier
            .size(dimensions().spacing56x),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(dimensions().spacing32x),
            painter = painterResource(R.drawable.ic_folder_item),
            contentDescription = null,
        )
    }
}

@Composable
internal fun FileIconPreview(cell: CellNodeUi.File) {
    Box(
        modifier = Modifier
            .size(dimensions().spacing56x),
        contentAlignment = Alignment.Center
    ) {
        if (cell.previewUrl != null && cell.assetType.previewSupported()) {

            val builder = ImageRequest.Builder(LocalContext.current)
                .diskCacheKey(cell.contentHash)
                .memoryCacheKey(cell.contentHash)
                .data(cell.previewUrl)
                .crossfade(true)

            AsyncImage(
                modifier = Modifier
                    .size(dimensions().spacing40x)
                    .clip(RoundedCornerShape(dimensions().spacing4x))
                    .background(
                        color = colorsScheme().outline,
                        shape = RoundedCornerShape(dimensions().spacing4x)
                    )
                    .border(
                        width = dimensions().spacing1x,
                        color = colorsScheme().outline,
                        shape = RoundedCornerShape(dimensions().spacing4x)
                    ),
                contentScale = ContentScale.Crop,
                model = builder.build(),
                contentDescription = null,
            )
            cell.publicLinkId?.let {
                PublicLinkIcon(
                    offsetX = dimensions().spacing16x,
                    offsetY = dimensions().spacing16x
                )
            }
        } else {
            Image(
                modifier = Modifier.size(dimensions().spacing32x),
                painter = painterResource(cell.assetType.icon()),
                contentDescription = null,
            )
            cell.publicLinkId?.let {
                PublicLinkIcon()
            }
        }
    }
}

@Composable
private fun PublicLinkIcon(
    offsetX: Dp = dimensions().spacing12x,
    offsetY: Dp = dimensions().spacing12x,
) {
    Icon(
        modifier = Modifier
            .size(dimensions().spacing20x)
            .offset(
                x = offsetX,
                y = offsetY
            )
            .background(
                color = colorsScheme().surfaceVariant,
                shape = CircleShape
            )
            .border(
                width = dimensions().spacing1x,
                color = colorsScheme().outline,
                shape = CircleShape
            )
            .padding(dimensions().spacing2x),
        imageVector = Icons.Default.Link,
        contentDescription = null,
    )
}

@Composable
private fun CellNodeUi.subtitle() =
    when {
        userName != null && conversationName != null -> {
            stringResource(R.string.file_subtitle, userName!!, conversationName!!)
        }

        userName != null && modifiedTime != null -> {
            stringResource(R.string.file_subtitle_modified, modifiedTime!!, userName!!)
        }

        userName != null -> userName
        conversationName != null -> conversationName
        modifiedTime != null -> modifiedTime
        else -> null
    }

@PreviewMultipleThemes
@Composable
private fun PreviewCellListItem() {
    WireTheme {
        CellListItem(
            cell = CellNodeUi.File(
                uuid = "",
                name = "file name",
                downloadProgress = 0.75f,
                assetType = AttachmentFileType.IMAGE,
                size = 123214,
                localPath = null,
                mimeType = "image/jpg",
                publicLinkId = "",
                userName = "Test User",
                conversationName = "Test Conversation",
                modifiedTime = null,
                remotePath = null,
                contentHash = null,
                contentUrl = null,
                previewUrl = null
            ),
            onMenuClick = {},
        )
    }
}
