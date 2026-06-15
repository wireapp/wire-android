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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.multipart.MultipartAttachmentOpenLoadState
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.android.feature.cells.R as cellsR

@Composable
internal fun BoxScope.TransferStatusIcon(
    item: MultipartAttachmentUi,
    size: Dp = dimensions().spacing32x,
    onLoaded: @Composable () -> Unit = {}
) {

    when (val openLoadState = item.openLoadState) {
        is MultipartAttachmentOpenLoadState.Loading -> {
            if (openLoadState.progress != null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(size)
                        .align(Alignment.Center),
                    progress = { openLoadState.progress },
                    color = colorsScheme().primary,
                    trackColor = colorsScheme().primaryVariant,
                    strokeWidth = dimensions().spacing2x,
                    strokeCap = StrokeCap.Round,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(size)
                        .align(Alignment.Center),
                    color = colorsScheme().primary,
                    trackColor = colorsScheme().primaryVariant,
                    strokeWidth = dimensions().spacing2x,
                    strokeCap = StrokeCap.Round,
                )
            }
            return
        }

        is MultipartAttachmentOpenLoadState.Ready -> {
            Icon(
                modifier = Modifier
                    .size(size)
                    .background(color = colorsScheme().surface, shape = CircleShape)
                    .padding(dimensions().spacing6x)
                    .align(Alignment.Center),
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = stringResource(cellsR.string.content_description_offline_available),
                tint = colorsScheme().primary,
            )
            return
        }

        MultipartAttachmentOpenLoadState.Error,
        null -> Unit
    }

    when (item.transferStatus) {

        AssetTransferStatus.FAILED_DOWNLOAD,
        AssetTransferStatus.NOT_DOWNLOADED -> {
            Icon(
                modifier = Modifier
                    .size(size)
                    .background(color = colorsScheme().surface, shape = CircleShape)
                    .padding(dimensions().spacing6x)
                    .align(Alignment.Center),
                painter = painterResource(R.drawable.ic_download),
                contentDescription = null,
                tint = colorsScheme().secondaryText
            )
        }

        AssetTransferStatus.SAVED_INTERNALLY -> onLoaded()

        else -> {}
    }
}
