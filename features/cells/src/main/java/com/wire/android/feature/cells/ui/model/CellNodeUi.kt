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
    abstract val userHandle: String?
    abstract val ownerUserId: String?
    abstract val conversationName: String?
    abstract val modifiedTime: String?
    abstract val publicLinkId: String?
    abstract val remotePath: String?
    abstract val size: Long?
    abstract val tags: List<String>
    internal abstract val openLoadState: OpenLoadState?
    abstract val downloadProgress: Float?

    /** True when this file has been saved for offline use (persisted in the offline files DB). */
    abstract val isAvailableOffline: Boolean

    val isOpenLoading: Boolean get() = openLoadState is OpenLoadState.Loading
    val openLoadProgress: Float? get() = (openLoadState as? OpenLoadState.Loading)?.progress

    data class Folder internal constructor(
        override val name: String?,
        override val uuid: String,
        override val userName: String?,
        override val userHandle: String?,
        override val ownerUserId: String?,
        override val conversationName: String?,
        override val modifiedTime: String?,
        override val publicLinkId: String? = null,
        override val remotePath: String? = null,
        override val size: Long?,
        override val tags: List<String> = emptyList(),
        internal override val openLoadState: OpenLoadState? = null,
        override val downloadProgress: Float? = null,
        override val isAvailableOffline: Boolean = false,
    ) : CellNodeUi()

    data class File internal constructor(
        override val name: String?,
        override val uuid: String,
        override val userName: String?,
        override val userHandle: String?,
        override val ownerUserId: String?,
        override val conversationName: String?,
        override val modifiedTime: String?,
        override val publicLinkId: String? = null,
        override val remotePath: String? = null,
        override val size: Long?,
        val mimeType: String,
        val assetType: AttachmentFileType,
        val localPath: String?,
        val contentHash: String? = null,
        val contentUrl: String? = null,
        val previewUrl: String? = null,
        override val tags: List<String> = emptyList(),
        val isEditSupported: Boolean = false,
        internal override val openLoadState: OpenLoadState? = null,
        override val downloadProgress: Float? = null,
        override val isAvailableOffline: Boolean = false,
    ) : CellNodeUi()
}

internal fun Node.File.toUiModel(
    openLoadState: OpenLoadState? = null,
    downloadProgress: Float? = null,
    isAvailableOffline: Boolean = false,
) = CellNodeUi.File(
    uuid = uuid,
    name = name,
    mimeType = mimeType,
    assetType = AttachmentFileType.fromMimeType(mimeType),
    size = size,
    localPath = (openLoadState as? OpenLoadState.Ready)?.localPath?.toString() ?: localPath,
    remotePath = remotePath,
    contentHash = contentHash,
    contentUrl = contentUrl,
    previewUrl = previewUrl,
    userName = userName,
    userHandle = userHandle,
    ownerUserId = ownerUserId,
    conversationName = conversationName,
    publicLinkId = publicLinkId,
    modifiedTime = formattedModifiedTime(),
    tags = tags,
    isEditSupported = isEditSupported,
    openLoadState = openLoadState,
    downloadProgress = downloadProgress,
    isAvailableOffline = isAvailableOffline,
)

internal fun Node.Folder.toUiModel() = CellNodeUi.Folder(
    uuid = uuid,
    name = name,
    userName = userName,
    userHandle = userHandle,
    ownerUserId = ownerUserId,
    conversationName = conversationName,
    modifiedTime = formattedModifiedTime(),
    remotePath = remotePath,
    size = size,
    tags = tags,
    publicLinkId = publicLinkId,
)

private fun Node.File.formattedModifiedTime() = modifiedTime?.let {
    Instant.fromEpochMilliseconds(it).cellFileDateTime()
}

private fun Node.Folder.formattedModifiedTime() = modifiedTime?.let {
    Instant.fromEpochMilliseconds(it).cellFileDateTime()
}

internal fun CellNodeUi.File.withSessionState(
    openLoadState: OpenLoadState?,
    downloadProgress: Float?,
    isAvailableOffline: Boolean,
): CellNodeUi.File = copy(
    openLoadState = openLoadState,
    localPath = (openLoadState as? OpenLoadState.Ready)?.localPath?.toString() ?: localPath,
    downloadProgress = downloadProgress,
    isAvailableOffline = isAvailableOffline,
)


internal fun CellNodeUi.File.localFileAvailable() = localPath != null
internal fun CellNodeUi.File.canOpenWithUrl() = contentUrl != null && assetType in listOf(IMAGE, VIDEO, PDF)
internal fun CellNodeUi.isEditSupported() = (this as? CellNodeUi.File)?.isEditSupported == true
