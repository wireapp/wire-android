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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.message.height
import com.wire.kalium.logic.data.message.width

@Composable
fun AssetPreview(
    item: MultipartAttachmentUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .background(
                color = colorsScheme().surfaceVariant,
                shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
            )
            .border(
                width = 1.dp,
                color = colorsScheme().outline,
                shape = RoundedCornerShape(dimensions().messageAttachmentCornerSize)
            )
            .clip(RoundedCornerShape(dimensions().messageAttachmentCornerSize))
    ) {
        if (item.transferStatus != AssetTransferStatus.NOT_FOUND) {
            when (item.assetType) {
                AttachmentFileType.IMAGE -> ImageAssetPreview(item)
                AttachmentFileType.VIDEO -> VideoAssetPreview(item)
                else -> FileAssetPreview(item)
            }
        } else {
            AssetNotAvailablePreview()
        }
    }
}

@Composable
internal fun calculateMaxMediaAssetWidth(
    item: MultipartAttachmentUi,
    maxDefaultWidth: Dp,
    maxDefaultWidthLandscape: Dp,
): Dp {

    val width = item.metadata?.width() ?: 0
    val height = item.metadata?.height() ?: 0

    return when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            if (width < height) {
                maxDefaultWidth
            } else {
                Dp.Unspecified
            }
        }

        Configuration.ORIENTATION_LANDSCAPE -> {
            if (width < height) {
                maxDefaultWidth
            } else {
                maxDefaultWidthLandscape
            }
        }

        else -> Dp.Unspecified
    }
}
