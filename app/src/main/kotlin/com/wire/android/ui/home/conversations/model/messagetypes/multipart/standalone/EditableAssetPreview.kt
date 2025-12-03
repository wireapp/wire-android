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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.textColor
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.transferProgressColor
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.util.fileExtension

/**
 * Show preview for files which support online editing option.
 */
@Composable
internal fun EditableAssetPreview(
    item: MultipartAttachmentUi,
    messageStyle: MessageStyle,
    accent: Accent
) {
    Column(
        modifier = Modifier
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
                color = messageStyle.textColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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

                // Remember recent drawable to use as placeholder to avoid blink on update
                var drawable by remember { mutableStateOf<Drawable?>(null) }

                val request = ImageRequest.Builder(LocalContext.current)
                    .diskCacheKey(item.contentHash)
                    .memoryCacheKey(item.contentHash)
                    .placeholderMemoryCacheKey(item.contentHash)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .placeholder(drawable)
                    .crossfade(true)
                    .data(item.previewUrl)
                    .build()

                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = request,
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.FillWidth,
                    onSuccess = { result ->
                        drawable = result.result.drawable
                    }
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
