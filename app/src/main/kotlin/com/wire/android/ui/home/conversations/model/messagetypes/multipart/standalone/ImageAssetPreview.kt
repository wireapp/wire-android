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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.isBubble
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewAvailable
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewImageModel
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.message.height
import com.wire.kalium.logic.data.message.width

@Composable
internal fun ImageAssetPreview(
    item: MultipartAttachmentUi,
    messageStyle: MessageStyle
) {

    val imageSize = ImageMessageParams(
        realImgWidth = item.metadata?.width() ?: 0,
        realImgHeight = item.metadata?.height() ?: 0,
        allowUpscale = true
    ).normalizedSize()

    Box(
        modifier = Modifier
            .width(imageSize.width)
            .height(imageSize.height)
    ) {
        if (item.previewAvailable()) {
            // Image preview
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = item.previewImageModel(),
                contentDescription = null,
                contentScale = if (messageStyle.isBubble()) ContentScale.Crop else ContentScale.Fit
            )
        } else {
            // File card if no preview available
            FileHeaderView(
                modifier = Modifier.padding(dimensions().spacing12x),
                extension = item.mimeType.substringAfter("/"),
                size = item.assetSize,
                messageStyle = messageStyle
            )
            if (item.transferStatus == AssetTransferStatus.NOT_FOUND) {
                Icon(
                    modifier = Modifier
                        .size(dimensions().spacing32x)
                        .align(Alignment.Center),
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = colorsScheme().scrim
                )
            }
        }
    }
}
