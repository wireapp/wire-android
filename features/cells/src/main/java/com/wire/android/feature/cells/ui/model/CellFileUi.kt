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
package com.wire.android.feature.cells.ui.model

import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.domain.model.AttachmentFileType.IMAGE
import com.wire.android.feature.cells.domain.model.AttachmentFileType.PDF
import com.wire.android.feature.cells.domain.model.AttachmentFileType.VIDEO
import com.wire.android.util.cellFileDateTime
import com.wire.kalium.cells.domain.model.CellFile
import kotlinx.datetime.Instant

internal data class CellFileUi(
    val uuid: String,
    val fileName: String?,
    val mimeType: String,
    val assetType: AttachmentFileType,
    val assetSize: Long?,
    val localPath: String?,
    val remotePath: String? = null,
    val contentHash: String? = null,
    val contentUrl: String? = null,
    val previewUrl: String? = null,
    val downloadProgress: Float? = null,
    val userName: String? = null,
    val conversationName: String? = null,
    val publicLinkId: String? = null,
    val modifiedTime: String? = null,
)

internal fun CellFile.toUiModel() = CellFileUi(
    uuid = uuid,
    fileName = fileName,
    mimeType = mimeType,
    assetType = AttachmentFileType.fromMimeType(mimeType),
    assetSize = assetSize,
    localPath = localPath,
    remotePath = remotePath,
    contentHash = contentHash,
    contentUrl = contentUrl,
    previewUrl = previewUrl,
    userName = userName,
    conversationName = conversationName,
    publicLinkId = publicLinkId,
    modifiedTime = formattedModifiedTime(),
)

private fun CellFile.formattedModifiedTime() = lastModified?.let {
    Instant.fromEpochMilliseconds(it).cellFileDateTime()
}

internal fun CellFileUi.localFileAvailable() = localPath != null
internal fun CellFileUi.canOpenWithUrl() = contentUrl != null && assetType in listOf(IMAGE, VIDEO, PDF)
