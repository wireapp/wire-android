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

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.feature.cells.domain.model.AttachmentFileType.IMAGE
import com.wire.android.feature.cells.domain.model.AttachmentFileType.PDF
import com.wire.android.feature.cells.domain.model.AttachmentFileType.VIDEO
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.ui.common.multipart.AssetSource
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.multipart.toUiModel
import com.wire.android.util.FileManager
import com.wire.kalium.cells.domain.usecase.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.RefreshCellAssetStateUseCase
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.CellAssetContent
import com.wire.kalium.logic.data.message.MessageAttachment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import javax.inject.Inject

@HiltViewModel
class MultipartAttachmentsViewModel @Inject constructor(
    private val refreshAsset: RefreshCellAssetStateUseCase,
    private val download: DownloadCellFileUseCase,
    private val fileManager: FileManager,
    private val kaliumFileSystem: KaliumFileSystem,
) : ViewModel() {

    private val refreshed = mutableListOf<String>()

    private val uploadProgress = mutableStateMapOf<String, Float>()

    fun mapAttachments(
        attachments: List<MessageAttachment>
    ): List<MultipartAttachmentGroup> {

        val result = mutableListOf<MultipartAttachmentGroup>()
        var group: MultipartAttachmentGroup? = null

        attachments.forEach {
            if (it.isMediaAttachment()) {
                group = when (group) {
                    null -> MultipartAttachmentGroup.Media(listOf(it.toUiModel()))
                    is MultipartAttachmentGroup.Media -> group.copy(group.attachments + it.toUiModel())
                    else -> {
                        result.add(group)
                        MultipartAttachmentGroup.Media(listOf(it.toUiModel()))
                    }
                }
            } else {
                group = when (group) {
                    null -> MultipartAttachmentGroup.Files(listOf(it.toUiModel()))
                    is MultipartAttachmentGroup.Files -> group.copy(group.attachments + it.toUiModel())
                    else -> {
                        result.add(group)
                        MultipartAttachmentGroup.Files(listOf(it.toUiModel()))
                    }
                }
            }
        }

        group?.let {
            result.add(it)
        }

        return result.toImmutableList()
    }

    sealed interface MultipartAttachmentGroup {
        data class Media(val attachments: List<MultipartAttachmentUi>) : MultipartAttachmentGroup
        data class Files(val attachments: List<MultipartAttachmentUi>) : MultipartAttachmentGroup
    }

    fun onClick(attachment: MultipartAttachmentUi, openInImageViewer: (String) -> Unit) {
        when {
            attachment.isImage() && !attachment.fileNotFound() -> openInImageViewer(attachment.uuid)
            attachment.fileNotFound() -> { refreshAssetState(attachment) }
            attachment.localFileAvailable() -> openLocalFile(attachment)
            attachment.canOpenWithUrl() -> openUrl(attachment)
            else -> downloadAsset(attachment)
        }
    }

    fun refreshAssetState(attachment: MultipartAttachmentUi) {
        if (refreshed.contains(attachment.uuid).not()) {
            refreshed.add(attachment.uuid)
            if (attachment.source == AssetSource.CELL) {
                viewModelScope.launch { refreshAsset(attachment.uuid) }
            }
        }
    }

    private fun openLocalFile(attachment: MultipartAttachmentUi) {
        fileManager.openWithExternalApp(
            assetDataPath = attachment.localPath?.toPath() ?: error("No local path"),
            assetName = attachment.fileName ?: "",
            mimeType = attachment.mimeType
        ) {
            appLogger.e("Failed to open: ${attachment.localPath}", tag = "MultipartAttachmentsViewModel")
        }
    }

    private fun openUrl(attachment: MultipartAttachmentUi) {
        fileManager.openUrlWithExternalApp(
            url = attachment.contentUrl ?: error("No preview URL"),
            mimeType = attachment.mimeType
        ) {
            appLogger.e("Failed to open: ${attachment.previewUrl}", tag = "MultipartAttachmentsViewModel")
        }
    }

    private fun downloadAsset(attachment: MultipartAttachmentUi) = viewModelScope.launch {

        // TODO: Move kaliumFileSystem to common kalium module so that it can be used in use case
        val path = kaliumFileSystem.providePersistentAssetPath(attachment.fileName ?: error("No asset path"))

        if (kaliumFileSystem.exists(path)) {
            kaliumFileSystem.delete(path)
        }

        download(
            assetId = attachment.uuid,
            outFilePath = path,
            assetSize = attachment.assetSize ?: 0,
        ) { progress ->
            attachment.assetSize?.let {
                val value = progress.toFloat() / it
                if (value < 1) {
                    uploadProgress[attachment.uuid] = value
                } else {
                    uploadProgress.remove(attachment.uuid)
                }
            }
        }
    }

    private fun MessageAttachment.toUiModel() =
        toUiModel(uploadProgress[assetId()])
}

private fun MessageAttachment.assetId() =
    when (this) {
        is AssetContent -> remoteData.assetId
        is CellAssetContent -> id
    }

private fun MessageAttachment.mimeType() =
    when (this) {
        is AssetContent -> mimeType
        is CellAssetContent -> mimeType
    }

private fun MultipartAttachmentUi.isImage() = AttachmentFileType.fromMimeType(mimeType) == IMAGE

private fun MessageAttachment.isMediaAttachment() =
    when (AttachmentFileType.fromMimeType(mimeType())) {
        IMAGE, VIDEO -> true
        else -> false
    }

private fun MultipartAttachmentUi.fileNotFound() = transferStatus == AssetTransferStatus.NOT_FOUND
private fun MultipartAttachmentUi.localFileAvailable() = localPath != null
private fun MultipartAttachmentUi.canOpenWithUrl() = contentUrl != null && assetType in listOf(IMAGE, VIDEO, PDF)
