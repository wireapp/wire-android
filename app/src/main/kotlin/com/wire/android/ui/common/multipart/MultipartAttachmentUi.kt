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
package com.wire.android.ui.common.multipart

import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.AssetContent.AssetMetadata
import com.wire.kalium.logic.data.message.CellAssetContent
import com.wire.kalium.logic.data.message.MessageAttachment

data class MultipartAttachmentUi(
    val uuid: String,
    val source: AssetSource,
    val fileName: String?,
    val localPath: String?,
    val contentHash: String? = null,
    val contentUrl: String? = null,
    val contentUrlExpiresAt: Long? = null,
    val previewUrl: String? = null,
    val mimeType: String,
    val assetType: AttachmentFileType,
    val assetSize: Long?,
    val metadata: AssetMetadata? = null,
    val transferStatus: AssetTransferStatus,
    val progress: Float? = null,
)

enum class AssetSource {
    CELL, ASSET_STORAGE
}

fun MessageAttachment.toUiModel(progress: Float? = null) = when (this) {
    is AssetContent -> this.toUiModel(progress)
    is CellAssetContent -> this.toUiModel(progress)
}

fun CellAssetContent.toUiModel(progress: Float?) = MultipartAttachmentUi(
    uuid = this.id,
    source = AssetSource.CELL,
    fileName = this.assetPath?.substringAfterLast("/"),
    localPath = this.localPath,
    contentUrl = this.contentUrl,
    contentUrlExpiresAt = this.contentUrlExpiresAt,
    previewUrl = this.previewUrl,
    mimeType = this.mimeType,
    assetType = AttachmentFileType.fromMimeType(mimeType),
    assetSize = this.assetSize,
    metadata = this.metadata,
    transferStatus = this.transferStatus,
    progress = progress,
    contentHash = contentHash,
)

fun AssetContent.toUiModel(progress: Float?) = MultipartAttachmentUi(
    uuid = this.remoteData.assetId,
    source = AssetSource.ASSET_STORAGE,
    fileName = this.name,
    localPath = this.localData?.assetDataPath,
    previewUrl = null,
    mimeType = this.mimeType,
    assetType = AttachmentFileType.fromMimeType(mimeType),
    assetSize = this.sizeInBytes,
    metadata = this.metadata,
    transferStatus = AssetTransferStatus.NOT_DOWNLOADED,
    progress = progress,
    contentHash = null,
    contentUrl = null,
)
