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
package com.wire.android.ui.home.conversations.model.messagetypes.video

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.isBubble
import com.wire.android.ui.home.conversations.messages.item.onSurface
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.DateAndTimeParsers
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AssetTransferStatus

@Composable
fun VideoMessage(
    assetSize: Long?,
    assetName: String,
    assetExtension: String,
    assetDataPath: String?,
    width: Int?,
    height: Int?,
    duration: Long?,
    transferStatus: AssetTransferStatus,
    onVideoClick: Clickable,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val maxWidth = if (width != null && height != null) {
        if (width < height) {
            240.dp
        } else {
            Dp.Unspecified
        }
    } else {
        Dp.Unspecified
    }

    Column(
        modifier = modifier
            .applyIf(!messageStyle.isBubble()) {
                widthIn(max = maxWidth)
                    .background(color = colorsScheme().surface, shape = RoundedCornerShape(dimensions().buttonCornerSize))
                    .border(
                        width = dimensions().spacing1x,
                        color = colorsScheme().outline,
                        shape = RoundedCornerShape(dimensions().buttonCornerSize)
                    )
                    .clip(RoundedCornerShape(dimensions().buttonCornerSize))
            }
            .clickable { }
            .padding(dimensions().spacing6x),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {

        FileHeaderView(
            extension = assetExtension,
            size = assetSize,
            messageStyle = messageStyle,
            modifier = Modifier
                .fillMaxWidth()
        )

        val textColor = when (messageStyle) {
            MessageStyle.BUBBLE_SELF -> colorsScheme().selfBubble.onPrimary
            MessageStyle.BUBBLE_OTHER -> colorsScheme().otherBubble.onPrimary
            MessageStyle.NORMAL -> colorsScheme().onSurfaceVariant
        }

        Text(
            modifier = Modifier.align(Alignment.Start),
            text = assetName,
            style = typography().body02,
            color = textColor,
        )

        Box(
            modifier = Modifier
                .applyIf(!messageStyle.isBubble()) {
                    fillMaxWidth(widthFraction(width, height))
                        .aspectRatio(aspectRatio(width, height))
                        .background(
                            color = colorsScheme().scrim,
                            shape = RoundedCornerShape(dimensions().buttonCornerSize)
                        )
                        .border(
                            width = 1.dp,
                            color = colorsScheme().outline,
                            shape = RoundedCornerShape(dimensions().buttonCornerSize)
                        )
                }
                .clip(RoundedCornerShape(dimensions().buttonCornerSize))
                .clickable { onVideoClick.onClick() },
            contentAlignment = Alignment.Center
        ) {

            assetDataPath?.let {
                val model = ImageRequest.Builder(context)
                    .data(it)
                    .decoderFactory { result, options, _ ->
                        VideoFrameDecoder(result.source, options)
                    }
                    .build()

                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = model,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
                duration?.let {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomEnd)
                            .background(color = colorsScheme().scrim)
                            .padding(dimensions().spacing4x),
                        text = DateAndTimeParsers.videoMessageTime(duration),
                        style = typography().subline01,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            when (transferStatus) {
                AssetTransferStatus.NOT_DOWNLOADED ->
                    Text(
                        text = stringResource(R.string.asset_message_tap_to_download_text),
                        color = messageStyle.onSurface(),
                        style = typography().subline01,
                    )

                AssetTransferStatus.DOWNLOAD_IN_PROGRESS ->
                    WireCircularProgressIndicator(
                        modifier = Modifier.size(dimensions().spacing32x),
                        progressColor = messageStyle.onSurface()
                    )

                AssetTransferStatus.FAILED_DOWNLOAD ->
                    Text(
                        text = stringResource(R.string.asset_message_failed_download_text),
                        color = messageStyle.onSurface(),
                        style = typography().subline01,
                    )

                AssetTransferStatus.UPLOADED,
                AssetTransferStatus.SAVED_INTERNALLY ->
                    Image(
                        modifier = Modifier.size(dimensions().spacing48x),
                        painter = painterResource(id = R.drawable.ic_play_circle_filled),
                        contentDescription = null,
                    )

                else -> {}
            }
        }
    }
}

@Suppress("MagicNumber")
private fun widthFraction(width: Int?, height: Int?) =
    if (width != null && height != null) {
        if (width < height) {
            0.5f
        } else {
            1f
        }
    } else {
        1f
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
private fun PreviewVideoMessage() {
    WireTheme {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoMessage(
                assetSize = 123456,
                assetName = "video.mp4",
                assetExtension = "mp4",
                assetDataPath = "",
                width = 1920,
                height = 1080,
                duration = 1231231,
                transferStatus = AssetTransferStatus.NOT_DOWNLOADED,
                messageStyle = MessageStyle.NORMAL,
                onVideoClick = Clickable {},
            )
            VideoMessage(
                assetSize = 123456,
                assetName = "video.mp4",
                assetExtension = "mp4",
                assetDataPath = "",
                transferStatus = AssetTransferStatus.DOWNLOAD_IN_PROGRESS,
                width = null,
                height = null,
                duration = 1231231,
                messageStyle = MessageStyle.NORMAL,
                onVideoClick = Clickable {},
            )
            VideoMessage(
                assetSize = 123456,
                assetName = "video.mp4",
                assetExtension = "mp4",
                assetDataPath = "",
                transferStatus = AssetTransferStatus.SAVED_INTERNALLY,
                width = 1920,
                height = 1080,
                duration = 234000,
                messageStyle = MessageStyle.NORMAL,
                onVideoClick = Clickable { },
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewVideoMessageError() {
    WireTheme {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoMessage(
                assetSize = 123456,
                assetName = "video.mp4",
                assetExtension = "mp4",
                assetDataPath = null,
                transferStatus = AssetTransferStatus.FAILED_DOWNLOAD,
                width = null,
                height = null,
                duration = 123123,
                messageStyle = MessageStyle.NORMAL,
                onVideoClick = Clickable {},
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewVideoMessageVertical() {
    WireTheme {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoMessage(
                assetSize = 123456,
                assetName = "video.mp4",
                assetExtension = "mp4",
                assetDataPath = null,
                transferStatus = AssetTransferStatus.SAVED_INTERNALLY,
                width = 1080,
                height = 1920,
                duration = 12412412,
                messageStyle = MessageStyle.NORMAL,
                onVideoClick = Clickable {},
            )
        }
    }
}
