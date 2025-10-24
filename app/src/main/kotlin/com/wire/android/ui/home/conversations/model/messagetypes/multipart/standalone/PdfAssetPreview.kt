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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.AssetSource
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewAvailable
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewImageModel
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.transferProgressColor
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PdfPreviewDecoder
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.AssetTransferStatus.FAILED_DOWNLOAD
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.height
import com.wire.kalium.logic.data.message.width
import com.wire.kalium.logic.util.fileExtension

@Composable
internal fun PdfAssetPreview(
    item: MultipartAttachmentUi,
    messageStyle: MessageStyle,
    accent: Accent
) {

    val width = item.metadata?.width() ?: 0
    val height = item.metadata?.height() ?: 0

    val maxWidth = calculateMaxMediaAssetWidth(
        item = item,
        maxDefaultWidth = dimensions().attachmentPdfMaxWidth,
        maxDefaultWidthLandscape = dimensions().attachmentPdfMaxWidthLandscape
    )

    Column(
        modifier = Modifier
            .widthIn(max = maxWidth)
            .applyIf(messageStyle == MessageStyle.BUBBLE_SELF) {
                background(
                    colorsScheme().bubbleContainerAccentBackgroundColor.getOrDefault(
                        accent,
                        colorsScheme().defaultBubbleContainerBackgroundColor
                    )
                )
            }
            .applyIf(messageStyle == MessageStyle.BUBBLE_OTHER) {
                background(
                    colorsScheme().surface
                )
            }
            .applyIf(messageStyle == MessageStyle.NORMAL) {
                background(
                    color = colorsScheme().surface,
                    shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
                )
                border(
                    width = dimensions().spacing1x,
                    color = colorsScheme().outline,
                    shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
                )
            }
            .clip(RoundedCornerShape(dimensions().messageAttachmentCornerSize)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {

        FileHeaderView(
            modifier = Modifier.padding(
                start = dimensions().spacing8x,
                top = dimensions().spacing8x,
                end = dimensions().spacing8x
            ),
            extension = item.fileName?.fileExtension() ?: item.mimeType.substringAfter("/"),
            size = item.assetSize,
            messageStyle = messageStyle
        )

        item.fileName?.let {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = dimensions().spacing8x, end = dimensions().spacing8x),
                text = it,
                style = MaterialTheme.wireTypography.body02,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .aspectRatio(aspectRatio(width, height))
                .background(
                    color = colorsScheme().outline,
                    shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
                )
                .clip(RoundedCornerShape(dimensions().messageAttachmentCornerSize)),
            contentAlignment = Alignment.Center
        ) {

            // Pdf preview image
            if (item.previewAvailable()) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = item.previewImageModel(
                        decoderFactory = { result, options, _ ->
                            PdfPreviewDecoder(result.source, options)
                        }
                    ),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.FillWidth
                )
            }

            // Download progress
            item.progress?.let {
                WireLinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart),
                    progress = { item.progress },
                    color = transferProgressColor(item.transferStatus),
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
private fun aspectRatio(width: Int?, height: Int?) =
    when {
        width == null || height == null -> 10f / 14f
        width == 0 || height == 0 -> 10f / 14f
        // Very long image
        width.toFloat() / height.toFloat() < 0.7f -> 10f / 14f
        else -> width.toFloat() / height.toFloat()
    }

@PreviewMultipleThemes
@Composable
private fun PreviewPdfAsset() {
    val attachment = MultipartAttachmentUi(
        assetSize = 123456,
        fileName = "Test file.pdf",
        mimeType = "image/pdf",
        transferStatus = AssetTransferStatus.SAVED_INTERNALLY,
        uuid = "assetUuid",
        source = AssetSource.CELL,
        localPath = null,
        previewUrl = null,
        assetType = AttachmentFileType.PDF,
        metadata = AssetContent.AssetMetadata.Image(
            width = 3,
            height = 1,
        ),
        progress = null,
    )

    WireTheme {
        Column(
            modifier = Modifier.padding(dimensions().spacing8x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
        ) {
            Box {
                PdfAssetPreview(
                    item = attachment.copy(
                        transferStatus = AssetTransferStatus.NOT_DOWNLOADED
                    ),
                    messageStyle = MessageStyle.NORMAL,
                    accent = Accent.Amber
                )
            }
            Box {
                PdfAssetPreview(
                    item = attachment.copy(
                        transferStatus = AssetTransferStatus.DOWNLOAD_IN_PROGRESS,
                        progress = 0.75f
                    ),
                    messageStyle = MessageStyle.NORMAL,
                    accent = Accent.Blue
                )
            }
            Box {
                PdfAssetPreview(
                    item = attachment,
                    messageStyle = MessageStyle.NORMAL,
                    accent = Accent.Red
                )
            }
            Box {
                PdfAssetPreview(
                    item = attachment.copy(
                        transferStatus = FAILED_DOWNLOAD,
                        progress = 0.75f
                    ),
                    messageStyle = MessageStyle.NORMAL,
                    accent = Accent.Petrol
                )
            }
        }
    }
}
