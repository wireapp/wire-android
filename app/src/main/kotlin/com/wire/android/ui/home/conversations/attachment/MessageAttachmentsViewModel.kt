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
package com.wire.android.ui.home.conversations.attachment

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.attachmentdraft.model.toUiModel
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.navArgs
import com.wire.android.ui.sharing.ImportedMediaAsset
import com.wire.android.util.FileManager
import com.wire.android.util.MediaMetadata
import com.wire.kalium.cells.domain.CellUploadEvent
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.model.AttachmentDraft
import com.wire.kalium.cells.domain.model.AttachmentUploadStatus
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.util.fileExtension
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import javax.inject.Inject

@HiltViewModel
class MessageAttachmentsViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val handleUriAsset: HandleUriAssetUseCase,
    private val observeAttachments: ObserveAttachmentDraftsUseCase,
    private val addAttachment: AddAttachmentDraftUseCase,
    private val removeAttachment: RemoveAttachmentDraftUseCase,
    private val uploadManager: CellUploadManager,
    private val fileManager: FileManager,
) : SavedStateViewModel(savedStateHandle) {

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = conversationNavArgs.conversationId
    private val uploadObservers = mutableMapOf<String, Job>()
    private val removedAttachments = MutableStateFlow(emptyList<String>())

    val attachments = mutableStateListOf<AttachmentDraftUi>()

    init {
        viewModelScope.launch {
            combine(removedAttachments, observeAttachments(conversationId)) { removed, list ->
                list.filterNot { removed.contains(it.uuid) }.mapToUiWithProgress()
            }.collectLatest {
                attachments.clear()
                attachments.addAll(it)
            }
        }
    }

    fun onFilesSelected(uriList: List<Uri>) = viewModelScope.launch {
        uriList.forEach { uri ->
            handleImportedAsset(uri)?.let { asset ->
                addAttachment(asset.assetBundle)
            }
        }
    }

    private suspend fun handleImportedAsset(uri: Uri): ImportedMediaAsset? =
        when (val result = handleUriAsset.invoke(uri, saveToDeviceIfInvalid = false)) {
            is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> ImportedMediaAsset(result.assetBundle, result.maxLimitInMB)
            is HandleUriAssetUseCase.Result.Success -> ImportedMediaAsset(result.assetBundle, null)
            is HandleUriAssetUseCase.Result.Failure.Unknown -> null
        }

    private fun addAttachment(bundle: AssetBundle) = viewModelScope.launch {
        addAttachment(
            conversationId = conversationId,
            fileName = bundle.fileName,
            assetPath = bundle.dataPath,
            assetSize = bundle.dataSize,
            mimeType = bundle.mimeType,
            assetMetadata = MediaMetadata.getMediaMetadata(bundle.dataPath, bundle.mimeType),
        )
            .onFailure {
                Log.e("MessageAttachmentsViewModel", "Failed to add attachment: $it")
            }
    }

    fun deleteAttachment(uiNode: AttachmentDraftUi) = viewModelScope.launch {
        removedAttachments.update { it + uiNode.uuid }
        removeAttachment(uiNode.uuid)
            .onSuccess {
                removedAttachments.update { it - uiNode.uuid }
            }
            .onFailure { error ->
                removedAttachments.update { it - uiNode.uuid }
                Log.e("MessageAttachmentsViewModel", "Failed to remove attachment: $error")
            }
    }

    private fun getUploadProgress(attachment: AttachmentDraft): Float? =
        when (attachment.uploadStatus) {
            AttachmentUploadStatus.UPLOADED -> null
            AttachmentUploadStatus.FAILED -> 1f
            AttachmentUploadStatus.UPLOADING -> {
                uploadManager.getUploadInfo(attachment.uuid)?.run {
                    observeUpload(attachment.uuid)
                    progress
                }
            }
        }

    private fun observeUpload(uuid: String) {
        uploadObservers[uuid]?.cancel()
        uploadObservers[uuid] = viewModelScope.launch observer@{
            uploadManager.observeUpload(uuid)?.collectLatest { event ->
                when (event) {
                    is CellUploadEvent.UploadProgress -> updateFile(uuid) { copy(uploadProgress = event.progress) }
                    is CellUploadEvent.UploadCancelled -> this@observer.cancel()
                    else -> {}
                }
            }
        }
    }

    private fun updateFile(uuid: String, block: AttachmentDraftUi.() -> AttachmentDraftUi) {
        attachments.indexOfFirst { it.uuid == uuid }.takeIf { it != -1 }?.let { index ->
            attachments[index] = attachments[index].block()
        }
    }

    private fun List<AttachmentDraft>.mapToUiWithProgress(): List<AttachmentDraftUi> =
        map { attachment ->
            attachment.toUiModel().copy(
                uploadProgress = getUploadProgress(attachment),
            )
        }

    fun showAttachment(attachment: AttachmentDraftUi) {
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(attachment.fileName.fileExtension() ?: "")
        fileManager.openWithExternalApp(attachment.localFilePath.toPath(), attachment.fileName, mimeType) {
            Log.e("MessageAttachmentsViewModel", "Failed to open: ${attachment.localFilePath}")
            // TODO: show message to user?
        }
    }
}
