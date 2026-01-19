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

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import coil3.asDrawable
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.textColor
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.transferProgressColor
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.util.fileExtension

/**
 * Show preview for files which support online editing option.
 */
@Composable
internal fun EditableAssetPreview(
    item: MultipartAttachmentUi,
    messageStyle: MessageStyle,
) {
    val resources = LocalResources.current
    Column(
        modifier = Modifier
            .heightIn(min = dimensions().spacing80x)
            .applyIf(messageStyle == MessageStyle.BUBBLE_SELF) {
                background(colorsScheme().selfBubble.secondary)
            }
            .applyIf(messageStyle == MessageStyle.BUBBLE_OTHER) {
                background(colorsScheme().otherBubble.secondary)
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
            Box(
                modifier = Modifier.then(
                    if (item.previewUrl == null) Modifier.weight(1f) else Modifier
                )
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = dimensions().spacing8x, end = dimensions().spacing8x)
                        .align(Alignment.BottomStart),
                    text = it,
                    style = MaterialTheme.wireTypography.body02,
                    maxLines = 2,
                    color = messageStyle.textColor(),
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .background(
                    color = colorsScheme().outline,
                    shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
                )
                .clip(RoundedCornerShape(dimensions().messageAttachmentCornerSize)),
            contentAlignment = Alignment.Center
        ) {

            item.previewUrl?.let {

                var drawable by remember { mutableStateOf<Drawable?>(null) }

                SubcomposeAsyncImage(
                    model = item.previewUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(.8f)
                        .sizeIn(maxHeight = dimensions().messageDocumentPreviewMaxHeight),
                    contentScale = ContentScale.FillWidth,
                ) {
                    when (painter.state.value) {
                        is AsyncImagePainter.State.Loading -> {
                            drawable?.let {
                                val painter = drawable?.toBitmap()?.asImageBitmap()?.let { BitmapPainter(it) }
                                painter?.let { paint ->
                                    Image(
                                        painter = paint,
                                        contentDescription = null,
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        is AsyncImagePainter.State.Success -> {
                            // Show the loaded image
                            SubcomposeAsyncImageContent()

                            // Update drawable state to use as placeholder next time
                            drawable = (painter.state.value as AsyncImagePainter.State.Success).result.image.asDrawable(resources)
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray)
                            )
                        }
                    }
                }
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
