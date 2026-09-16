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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R as cellsR
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.multipart.MultipartAttachmentOpenLoadState
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.model.messagetypes.asset.getDownloadStatusText
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.TransferStatusIcon
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.isFailed
import com.wire.kalium.logic.util.fileExtension

@Composable
internal fun BoxScope.FileAssetGridPreview(
    item: MultipartAttachmentUi,
    messageStyle: MessageStyle
) {
    val statusLabel = when (item.openLoadState) {
        is MultipartAttachmentOpenLoadState.Loading -> stringResource(cellsR.string.tap_to_cancel_loading)
        MultipartAttachmentOpenLoadState.Error -> stringResource(cellsR.string.unable_to_load_retry)
        is MultipartAttachmentOpenLoadState.Ready -> null
        null -> if (
            item.transferStatus == AssetTransferStatus.NOT_DOWNLOADED ||
            item.transferStatus == AssetTransferStatus.DOWNLOAD_IN_PROGRESS ||
            item.transferStatus == AssetTransferStatus.SAVED_INTERNALLY
        ) {
            null
        } else {
            getDownloadStatusText(item.transferStatus)
        }
    }

    FileHeaderView(
        modifier = Modifier.padding(dimensions().spacing8x),
        extension = item.fileName?.fileExtension() ?: item.mimeType.substringAfter("/"),
        size = item.assetSize,
        messageStyle = messageStyle,
        label = statusLabel,
        labelColor = if (item.transferStatus.isFailed() || item.openLoadState is MultipartAttachmentOpenLoadState.Error) {
            colorsScheme().error
        } else {
            null
        },
        showLabelInBubble = true,
    )
    TransferStatusIcon(item)
}
