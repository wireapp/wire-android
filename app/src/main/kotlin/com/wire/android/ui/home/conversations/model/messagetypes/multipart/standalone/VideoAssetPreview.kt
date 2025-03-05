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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.wire.android.R
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.home.conversations.model.messagetypes.asset.getDownloadStatusText
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.TransferStatusIcon
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewAvailable
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewImageModel
import com.wire.android.util.DateAndTimeParsers
import com.wire.kalium.logic.data.asset.isFailed
import com.wire.kalium.logic.data.message.durationMs
import com.wire.kalium.logic.data.message.height
import com.wire.kalium.logic.data.message.width

@Composable
internal fun VideoAssetPreview(
    item: MultipartAttachmentUi
) {

    val width = item.metadata?.width()
    val height = item.metadata?.height()

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
        modifier = Modifier
            .widthIn(max = maxWidth)
            .background(color = colorsScheme().surface, shape = RoundedCornerShape(dimensions().buttonCornerSize))
            .border(
                width = dimensions().spacing1x,
                color = colorsScheme().outline,
                shape = RoundedCornerShape(dimensions().buttonCornerSize)
            )
            .clip(RoundedCornerShape(dimensions().buttonCornerSize))
            .padding(dimensions().spacing10x),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {

        FileHeaderView(
            modifier = Modifier.padding(dimensions().spacing8x),
            extension = item.mimeType.substringAfter("/"),
            size = item.assetSize,
            label = getDownloadStatusText(item.transferStatus),
            labelColor = if (item.transferStatus.isFailed()) colorsScheme().error else null
        )

        Box(
            modifier = Modifier
                .aspectRatio(aspectRatio(width, height))
                .background(
                    color = colorsScheme().outline,
                    shape = RoundedCornerShape(dimensions().buttonCornerSize)
                )
                .border(
                    width = 1.dp,
                    color = colorsScheme().outline,
                    shape = RoundedCornerShape(dimensions().buttonCornerSize)
                )
                .clip(RoundedCornerShape(dimensions().buttonCornerSize)),
            contentAlignment = Alignment.Center
        ) {

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

            item.metadata?.durationMs()?.let {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomEnd)
                        .background(color = Color.Black.copy(alpha = 0.5f))
                        .padding(4.dp),
                    text = DateAndTimeParsers.videoMessageTime(it),
                    fontSize = 12.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            TransferStatusIcon(item, 38.dp) {
                Image(
                    modifier = Modifier
                        .size(42.dp)
                        .align(Alignment.Center),
                    painter = painterResource(id = R.drawable.ic_play_circle_filled),
                    contentDescription = null,
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
