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

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.attachmentdraft.model.toUiModel
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.navArgs
import com.wire.kalium.cells.domain.CellUploadEvent
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.model.AttachmentDraft
import com.wire.kalium.cells.domain.model.AttachmentUploadStatus
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.logic.data.id.QualifiedID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageAttachmentsViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val observeAttachments: ObserveAttachmentDraftsUseCase,
    private val addAttachment: AddAttachmentDraftUseCase,
    private val removeAttachment: RemoveAttachmentDraftUseCase,
    private val uploadManager: CellUploadManager,
) : SavedStateViewModel(savedStateHandle) {

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = conversationNavArgs.conversationId
    private val uploadObservers = mutableMapOf<String, Job>()

    val attachments = mutableStateListOf<AttachmentDraftUi>()

    init {
        viewModelScope.launch {
            observeAttachments(conversationId).collectLatest { list ->
                attachments.clear()
                attachments.addAll(
                    list.map {
                        it.toUiModel().copy(
                            uploadProgress = getUploadProgress(it),
                        )
                    }
                )
            }
        }
    }

    fun onFilesSelected(pendingBundles: List<AssetBundle>) = viewModelScope.launch {
        pendingBundles.forEach { bundle ->
            addAttachment(
                conversationId = conversationId,
                fileName = bundle.fileName,
                assetPath = bundle.dataPath,
                assetSize = bundle.dataSize,
            )
                .onFailure {
                    Log.e("MessageAttachmentsViewModel", "Failed to add attachment: $it")
                }
        }
    }

    fun deleteAttachment(uiNode: AttachmentDraftUi) = viewModelScope.launch {
        removeAttachment(uiNode.uuid)
            .onFailure {
                Log.e("MessageAttachmentsViewModel", "Failed to remove attachment: $it")
            }
    }

    private fun getUploadProgress(attachment: AttachmentDraft): Float? =
        when (attachment.uploadStatus) {
            AttachmentUploadStatus.UPLOADED -> null
            AttachmentUploadStatus.FAILED -> uploadManager.getUploadInfo(attachment.uuid)?.progress
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
}
