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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.AssetSource
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.isBubble
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.transferProgressColor
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.message.AssetContent

@Composable
internal fun AssetGridPreview(
    item: MultipartAttachmentUi,
    messageStyle: MessageStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .applyIf(messageStyle.isBubble()) {
                background(
                    color = colorsScheme().surfaceVariant,
                    shape = RoundedCornerShape(dimensions().messageAttachmentGridCornerSize)
                )
            }
            .clip(RoundedCornerShape(dimensions().messageAttachmentGridCornerSize))
    ) {

        if (item.transferStatus != AssetTransferStatus.NOT_FOUND) {
            when (item.assetType) {
                AttachmentFileType.IMAGE -> {
                    ImageAssetGridPreview(item)
                }

                AttachmentFileType.VIDEO -> {
                    VideoAssetGridPreview(item, messageStyle)
                }

                else -> {
                    FileAssetGridPreview(item, messageStyle)
                }
            }

            item.progress?.let {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(dimensions().spacing32x)
                        .align(Alignment.Center),
                    progress = { it },
                    color = transferProgressColor(item.transferStatus),
                    trackColor = Color.Transparent,
                )
            }
        } else {
            AssetNotAvailableGridPreview()
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewAssetGrid() {

    val attachment = MultipartAttachmentUi(
        assetSize = 123456,
        fileName = "Test file.pdf",
        mimeType = "",
        transferStatus = AssetTransferStatus.SAVED_INTERNALLY,
        uuid = "assetUuid",
        source = AssetSource.CELL,
        localPath = "localPath",
        previewUrl = "previewUrl",
        assetType = AttachmentFileType.PDF,
        metadata = AssetContent.AssetMetadata.Image(
            width = 100,
            height = 100,
        ),
        progress = null,
    )

    WireTheme {
        Column(
            modifier = Modifier.padding(dimensions().spacing8x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
        ) {
            AssetGridPreview(
                item = attachment.copy(
                    assetType = AttachmentFileType.CODE,
                    fileName = "Test file.kt",
                    transferStatus = AssetTransferStatus.NOT_DOWNLOADED,
                ),
                messageStyle = MessageStyle.NORMAL,
                onClick = {},
            )
        }
    }
}
