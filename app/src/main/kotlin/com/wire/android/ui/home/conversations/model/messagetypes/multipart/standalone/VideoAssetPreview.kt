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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.wire.android.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.darkColorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.AssetSource
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewAvailable
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewImageModel
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.transferProgressColor
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.DateAndTimeParsers
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.AssetTransferStatus.FAILED_DOWNLOAD
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.durationMs
import com.wire.kalium.logic.data.message.height
import com.wire.kalium.logic.data.message.width

@Composable
internal fun VideoAssetPreview(
    item: MultipartAttachmentUi
) {

    val width = item.metadata?.width() ?: 0
    val height = item.metadata?.height() ?: 0
    val maxWidth = calculateMaxMediaAssetWidth(
        item = item,
        maxDefaultWidth = dimensions().attachmentVideoMaxWidth,
        maxDefaultWidthLandscape = dimensions().attachmentVideoMaxWidthLandscape
    )

    Column(
        modifier = Modifier
            .widthIn(max = maxWidth)
            .background(
                color = colorsScheme().surface,
                shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
            )
            .border(
                width = dimensions().spacing1x,
                color = colorsScheme().outline,
                shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
            )
            .clip(RoundedCornerShape(dimensions().messageAttachmentCornerSize))
            .padding(dimensions().spacing10x),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {

        FileHeaderView(
            modifier = Modifier.padding(dimensions().spacing8x),
            extension = item.mimeType.substringAfter("/"),
            size = item.assetSize,
        )

        Box(
            modifier = Modifier
                .aspectRatio(aspectRatio(width, height))
                .background(
                    color = colorsScheme().outline,
                    shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
                )
                .border(
                    width = 1.dp,
                    color = colorsScheme().outline,
                    shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
                )
                .clip(RoundedCornerShape(dimensions().messageAttachmentCornerSize)),
            contentAlignment = Alignment.Center
        ) {

            // Video preview image
            if (item.previewAvailable()) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = item.previewImageModel(
                        decoderFactory = { result, options, _ ->
                            VideoFrameDecoder(result.source, options)
                        }
                    ),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
            }

            // Video duration text
            item.metadata?.durationMs()?.let {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomEnd)
                        .background(color = colorsScheme().scrim)
                        .padding(dimensions().spacing4x),
                    text = DateAndTimeParsers.videoMessageTime(it),
                    style = typography().subline01,
                    color = darkColorsScheme().onSurface,
                    textAlign = TextAlign.Center
                )
            }

            item.contentUrl?.let {
                Image(
                    modifier = Modifier
                        .size(dimensions().spacing40x)
                        .align(Alignment.Center),
                    painter = painterResource(id = R.drawable.ic_play_circle_filled),
                    contentDescription = null,
                )
            }

            // Download progress
            item.progress?.let {
                WireLinearProgressIndicator(
                    modifier = Modifier
                        .widthIn(max = maxWidth)
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
    if (width != null && height != null) {
        width.toFloat() / height.toFloat()
    } else {
        16f / 9f
    }

@PreviewMultipleThemes
@Composable
private fun PreviewVideoAsset() {
    val attachment = MultipartAttachmentUi(
        assetSize = 123456,
        fileName = "Test file.mp4",
        mimeType = "video/mp4",
        transferStatus = AssetTransferStatus.SAVED_INTERNALLY,
        uuid = "assetUuid",
        source = AssetSource.CELL,
        localPath = null,
        previewUrl = null,
        assetType = AttachmentFileType.VIDEO,
        metadata = AssetContent.AssetMetadata.Video(
            width = 3,
            height = 1,
            durationMs = 30000,
        ),
        progress = null,
    )

    WireTheme {
        Column(
            modifier = Modifier.padding(dimensions().spacing8x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
        ) {
            Box {
                VideoAssetPreview(
                    item = attachment.copy(
                        transferStatus = AssetTransferStatus.NOT_DOWNLOADED
                    )
                )
            }
            Box {
                VideoAssetPreview(
                    item = attachment.copy(
                        transferStatus = AssetTransferStatus.DOWNLOAD_IN_PROGRESS,
                        progress = 0.75f
                    )
                )
            }
            Box {
                VideoAssetPreview(
                    item = attachment
                )
            }
            Box {
                VideoAssetPreview(
                    item = attachment.copy(
                        transferStatus = FAILED_DOWNLOAD,
                        progress = 0.75f
                    )
                )
            }
        }
    }
}
