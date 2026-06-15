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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.AssetSource
import com.wire.android.ui.common.multipart.MultipartAttachmentOpenLoadState
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.textColor
import com.wire.android.ui.home.conversations.model.messagetypes.asset.getDownloadStatusText
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.isFailed
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.util.fileExtension
import com.wire.android.feature.cells.R as cellsR
import com.wire.android.ui.common.R as commonR

@Composable
internal fun BoxScope.FileAssetPreview(
    item: MultipartAttachmentUi,
    messageStyle: MessageStyle,
) {
    val statusLabel = when (item.openLoadState) {
        is MultipartAttachmentOpenLoadState.Loading -> stringResource(cellsR.string.tap_to_cancel_loading)
        MultipartAttachmentOpenLoadState.Error -> stringResource(cellsR.string.unable_to_load_retry)
        is MultipartAttachmentOpenLoadState.Ready -> null
        null -> if (
            item.transferStatus == AssetTransferStatus.NOT_DOWNLOADED ||
            item.transferStatus == AssetTransferStatus.DOWNLOAD_IN_PROGRESS ||
            item.transferStatus == AssetTransferStatus.SAVED_INTERNALLY
        ) {
            null
        } else {
            getDownloadStatusText(item.transferStatus)
        }

        else -> getDownloadStatusText(item.transferStatus)
    }

    val isOpenLoadError = item.openLoadState is MultipartAttachmentOpenLoadState.Error
    val leadingContent: (@Composable () -> Unit)? = when (item.openLoadState) {
        is MultipartAttachmentOpenLoadState.Loading -> {
            val loadingProgress = item.openLoadState.progress
            {
                if (loadingProgress != null) {
                    CircularProgressIndicator(
                        progress = { loadingProgress },
                        color = colorsScheme().primary,
                        trackColor = colorsScheme().primaryVariant,
                        strokeWidth = dimensions().spacing2x,
                        strokeCap = StrokeCap.Round,
                    )
                } else {
                    CircularProgressIndicator(
                        color = colorsScheme().primary,
                        trackColor = colorsScheme().primaryVariant,
                        strokeWidth = dimensions().spacing2x,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }

        is MultipartAttachmentOpenLoadState.Ready -> {
            {
                Icon(
                    painter = painterResource(commonR.drawable.ic_check_circle),
                    contentDescription = stringResource(cellsR.string.content_description_offline_available),
                    tint = colorsScheme().primary,
                )
            }
        }

        else -> null
    }

    Column(
        modifier = Modifier
            .applyIf(messageStyle == MessageStyle.BUBBLE_SELF) {
                background(colorsScheme().selfBubble.secondary)
            }
            .applyIf(messageStyle == MessageStyle.BUBBLE_OTHER) {
                background(colorsScheme().otherBubble.secondary)
            }
            .fillMaxWidth()
            .height(dimensions().spacing80x)
            .padding(dimensions().spacing8x),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {
        FileHeaderView(
            extension = item.fileName?.fileExtension() ?: item.mimeType.substringAfter("/"),
            type = item.assetType,
            size = item.assetSize,
            label = statusLabel,
            labelColor = if (item.transferStatus.isFailed() || isOpenLoadError) colorsScheme().error else null,
            messageStyle = messageStyle,
            showLabelInBubble = true,
            leadingContent = leadingContent,
        )
        item.fileName?.let {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart),
                    text = it,
                    style = MaterialTheme.wireTypography.body02,
                    maxLines = 2,
                    color = messageStyle.textColor(),
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewFileAsset() {

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
            Box {
                FileAssetPreview(
                    item = attachment.copy(
                        assetType = AttachmentFileType.CODE,
                        fileName = "Test file.kt",
                        transferStatus = AssetTransferStatus.NOT_DOWNLOADED
                    ),
                    messageStyle = MessageStyle.NORMAL
                )
            }
            Box {
                FileAssetPreview(
                    item = attachment.copy(
                        assetType = AttachmentFileType.ARCHIVE,
                        fileName = "Test file.zip",
                        transferStatus = AssetTransferStatus.DOWNLOAD_IN_PROGRESS,
                        progress = 0.75f
                    ),
                    messageStyle = MessageStyle.NORMAL
                )
            }
            Box {
                FileAssetPreview(
                    item = attachment,
                    messageStyle = MessageStyle.NORMAL
                )
            }
            Box {
                FileAssetPreview(
                    item = attachment.copy(
                        assetType = AttachmentFileType.OTHER,
                        fileName = "Test file.prof",
                        transferStatus = AssetTransferStatus.FAILED_DOWNLOAD,
                        progress = 0.75f
                    ),
                    messageStyle = MessageStyle.NORMAL
                )
            }
        }
    }
}
