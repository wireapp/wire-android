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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.home.conversations.model.messagetypes.asset.getDownloadStatusText
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.TransferStatusIcon
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewAvailable
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewImageModel
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PdfPreviewDecoder
import com.wire.kalium.logic.data.asset.AssetTransferStatus.FAILED_DOWNLOAD
import com.wire.kalium.logic.data.asset.isFailed
import com.wire.kalium.logic.data.message.height
import com.wire.kalium.logic.data.message.width

@Composable
internal fun PdfAssetPreview(item: MultipartAttachmentUi) {

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
            extension = item.mimeType.substringAfter("/"),
            size = item.assetSize,
            label = getDownloadStatusText(item.transferStatus),
            labelColor = if (item.transferStatus.isFailed()) colorsScheme().error else null
        )

        item.fileName?.let {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = it,
                style = MaterialTheme.wireTypography.body02,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

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
                            PdfPreviewDecoder(result.source, options)
                        }
                    ),
                    contentDescription = null,
                )
            }

            TransferStatusIcon(item, 38.dp)

            item.progress?.let {
                WireLinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
                    progress = { item.progress },
                    color = if (item.transferStatus == FAILED_DOWNLOAD) colorsScheme().error else colorsScheme().primary,
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
        10f / 14f
    }
