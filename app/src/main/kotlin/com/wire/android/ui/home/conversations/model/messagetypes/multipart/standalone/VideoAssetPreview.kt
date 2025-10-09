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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.wire.android.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.darkColorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.AssetSource
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.isBubble
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewAvailable
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewImageModel
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.transferProgressColor
import com.wire.android.ui.theme.Accent
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
    item: MultipartAttachmentUi,
    messageStyle: MessageStyle,
    accent: Accent = Accent.Unknown
) {

    val videoSize = ImageMessageParams(
        realImgWidth = item.metadata?.width() ?: 0,
        realImgHeight = item.metadata?.height() ?: 0,
        allowUpscale = true
    ).normalizedSize()

    val maxWidth = calculateMaxMediaAssetWidth(
        item = item,
        maxDefaultWidth = dimensions().attachmentVideoMaxWidth,
        maxDefaultWidthLandscape = dimensions().attachmentVideoMaxWidthLandscape
    )

    val fileNameColor = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().onPrimary
        MessageStyle.BUBBLE_OTHER -> colorsScheme().onSurface
        MessageStyle.NORMAL -> colorsScheme().onSurface
    }

    Column(
        modifier = Modifier
            .width(videoSize.width + dimensions().spacing16x)
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
            .applyIf(!messageStyle.isBubble()) {
                background(
                    color = colorsScheme().primaryButtonSelected,
                    shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
                )
                border(
                    width = dimensions().spacing1x,
                    color = colorsScheme().outline,
                    shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
                )
                padding(dimensions().spacing12x)
            }
            .clip(RoundedCornerShape(dimensions().messageAttachmentCornerSize)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {

        FileHeaderView(
            modifier = Modifier.padding(dimensions().spacing8x),
            extension = item.mimeType.substringAfter("/"),
            size = item.assetSize,
            messageStyle = messageStyle
        )

        item.fileName?.let {
            Text(
                modifier = Modifier.fillMaxWidth().padding(start = dimensions().spacing8x),
                text = it.substringBeforeLast("."),
                style = typography().body02,
                color = fileNameColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .width(videoSize.width)
                .height(videoSize.height)
                .background(
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
    when {
        width == null || height == null -> 16f / 9f
        width == 0 || height == 0 -> 16f / 9f
        else -> width.toFloat() / height.toFloat()
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
                    ),
                    messageStyle = MessageStyle.NORMAL
                )
            }
            Box {
                VideoAssetPreview(
                    item = attachment.copy(
                        transferStatus = AssetTransferStatus.DOWNLOAD_IN_PROGRESS,
                        progress = 0.75f
                    ),
                    messageStyle = MessageStyle.NORMAL
                )
            }
            Box {
                VideoAssetPreview(
                    item = attachment,
                    messageStyle = MessageStyle.NORMAL
                )
            }
            Box {
                VideoAssetPreview(
                    item = attachment.copy(
                        transferStatus = FAILED_DOWNLOAD,
                        progress = 0.75f
                    ),
                    messageStyle = MessageStyle.NORMAL
                )
            }
        }
    }
}
