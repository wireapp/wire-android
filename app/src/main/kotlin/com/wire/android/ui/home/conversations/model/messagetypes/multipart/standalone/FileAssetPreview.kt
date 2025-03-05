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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.home.conversations.model.messagetypes.asset.getDownloadStatusText
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.isFailed
import com.wire.kalium.logic.util.fileExtension

@Composable
internal fun BoxScope.FileAssetPreview(item: MultipartAttachmentUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorsScheme().surface)
            .padding(dimensions().spacing8x),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {
        FileHeaderView(
            extension = item.fileName?.fileExtension() ?: item.mimeType.substringAfter("/"),
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
    }
    item.progress?.let {
        WireLinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
            progress = { item.progress },
            color = if (item.transferStatus == AssetTransferStatus.FAILED_DOWNLOAD) colorsScheme().error else colorsScheme().primary,
            trackColor = Color.Transparent,
        )
    }
}
