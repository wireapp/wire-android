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
import com.wire.kalium.cells.domain.model.Node
import kotlinx.datetime.Instant

sealed class CellNodeUi {
    abstract val name: String?
    abstract val uuid: String
    abstract val userName: String?
    abstract val conversationName: String?
    abstract val modifiedTime: String?

    data class Folder(
        override val name: String?,
        override val uuid: String,
        override val userName: String?,
        override val conversationName: String?,
        override val modifiedTime: String?,
    ) : CellNodeUi()

    data class File(
        override val name: String?,
        override val uuid: String,
        override val userName: String?,
        override val conversationName: String?,
        override val modifiedTime: String?,
        val mimeType: String,
        val assetType: AttachmentFileType,
        val assetSize: Long?,
        val localPath: String?,
        val remotePath: String? = null,
        val contentHash: String? = null,
        val contentUrl: String? = null,
        val previewUrl: String? = null,
        val downloadProgress: Float? = null,
        val publicLinkId: String? = null,
    ) : CellNodeUi()
}

internal fun Node.File.toUiModel() = CellNodeUi.File(
    uuid = uuid,
    name = name,
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

internal fun Node.Folder.toUiModel() = CellNodeUi.Folder(
    uuid = uuid,
    name = name,
    userName = userName,
    conversationName = conversationName,
    modifiedTime = formattedModifiedTime(),
)

private fun Node.File.formattedModifiedTime() = modifiedTime?.let {
    Instant.fromEpochMilliseconds(it).cellFileDateTime()
}
private fun Node.Folder.formattedModifiedTime() = modifiedTime?.let {
    Instant.fromEpochMilliseconds(it).cellFileDateTime()
}

internal fun CellNodeUi.File.localFileAvailable() = localPath != null
internal fun CellNodeUi.File.canOpenWithUrl() = contentUrl != null && assetType in listOf(IMAGE, VIDEO, PDF)
