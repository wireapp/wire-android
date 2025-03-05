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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.attachmentdraft.model.AttachmentFileType
import com.wire.android.ui.common.attachmentdraft.model.previewSupported
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewAvailable
import com.wire.kalium.logic.data.asset.AssetTransferStatus

@Composable
internal fun AssetGridPreview(
    item: MultipartAttachmentUi,
    onClick: () -> Unit,
    onLoadPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {

    if (item.assetType.previewSupported() && item.previewAvailable().not()) {
        onLoadPreview()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .background(color = colorsScheme().backdrop.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = colorsScheme().outline, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {

        when (item.assetType) {
            AttachmentFileType.IMAGE -> {
                ImageAssetGridPreview(item)
            }
            AttachmentFileType.VIDEO -> {
                VideoAssetGridPreview(item)
            }
            AttachmentFileType.PDF -> {
                PdfAssetGridPreview(item)
            }
            else -> {
                FileAssetGridPreview(item)
            }
        }

        item.progress?.let {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp).align(Alignment.Center),
                progress = { it },
                color = if (item.transferStatus == AssetTransferStatus.FAILED_DOWNLOAD) colorsScheme().error else colorsScheme().primary,
                trackColor = Color.Transparent,
            )
//            WireLinearProgressIndicator(
//                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
//                progress = { item.progress },
//                color = if (item.transferStatus == AssetTransferStatus.FAILED_DOWNLOAD) colorsScheme().error else colorsScheme().primary,
//                trackColor = Color.Transparent,
//            )
        }
    }
}
