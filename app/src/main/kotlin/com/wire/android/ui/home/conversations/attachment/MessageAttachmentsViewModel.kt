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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.attachmentdraft.model.toUiModel
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.MessageSharedState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.util.GetMediaMetadataUseCase
import com.wire.kalium.cells.domain.CellUploadEvent
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.model.AttachmentDraft
import com.wire.kalium.cells.domain.model.AttachmentUploadStatus
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.RetryAttachmentUploadUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.id.QualifiedID
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel(assistedFactory = MessageAttachmentsViewModel.Factory::class)
class MessageAttachmentsViewModel @AssistedInject constructor(
    @Assisted conversationNavArgs: ConversationNavArgs,
    private val assetImporter: MessageAttachmentAssetImporter,
    private val observeAttachments: ObserveAttachmentDraftsUseCase,
    private val addAttachment: AddAttachmentDraftUseCase,
    private val removeAttachment: RemoveAttachmentDraftUseCase,
    private val retryUpload: RetryAttachmentUploadUseCase,
    private val uploadManager: CellUploadManager,
    private val fileGateway: MessageAttachmentFileGateway,
    private val sharedState: MessageSharedState,
    private val getMediaMetadata: GetMediaMetadataUseCase,
) : ViewModel() {

    private val conversationId: QualifiedID = conversationNavArgs.conversationId
    private val uploadObservers = mutableMapOf<String, Job>()
    private val removedAttachments = MutableStateFlow(emptyList<String>())

    val attachments = mutableStateListOf<AttachmentDraftUi>()

    var failedAttachmentDialogState: FailedAttachmentDialogState by mutableStateOf(FailedAttachmentDialogState.Hidden)
        private set

    var incompatibleFileNameDialogState: IncompatibleFileNameDialogState by mutableStateOf(IncompatibleFileNameDialogState.Hidden)
        private set

    private val pendingIncompatibleBundles: ArrayDeque<AssetBundle> = ArrayDeque()

    init {
        viewModelScope.launch {
            combine(removedAttachments, observeAttachments(conversationId)) { removed, list ->
                list.filterNot { removed.contains(it.uuid) }.mapToUiWithProgress()
            }.collectLatest {
                attachments.clear()
                attachments.addAll(it)
            }
        }

        viewModelScope.launch {
            sharedState.asFlow().collect {
                if (it.isNotEmpty()) {
                    onFilesAddedAsBundle(it)
                }
            }
        }
    }

    fun onAudioRecorded(uri: String, wavesMask: List<Int>?) = viewModelScope.launch {
        assetImporter.importAsset(uri)?.assetBundle?.let { bundle ->
            addAttachment(
                conversationId = conversationId,
                fileName = bundle.fileName,
                assetPath = bundle.dataPath,
                assetSize = bundle.dataSize,
                mimeType = bundle.mimeType,
                assetMetadata = fileGateway.audioMetadata(bundle.dataPath, bundle.mimeType, wavesMask)
            ).onFailure {
                appLogger.e("Failed to add recorded audio attachment: $it", tag = "MessageAttachmentsViewModel")
            }
        }
    }

    fun onFilesSelected(uriList: List<String>) = viewModelScope.launch {
        uriList.forEach { uri ->
            assetImporter.importAsset(uri)?.let { asset ->
                enqueueOrAddAttachment(asset.assetBundle)
            }
        }
    }

    fun onFilesAddedAsBundle(bundles: List<AssetBundle>) = viewModelScope.launch {
        bundles.forEach { bundle ->
            enqueueOrAddAttachment(bundle)
        }
    }

    private fun enqueueOrAddAttachment(bundle: AssetBundle) {
        if (bundle.fileName.hasIncompatibleFileNameCharacters()) {
            pendingIncompatibleBundles.addLast(bundle)
            if (incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Hidden) {
                showNextIncompatibleDialog()
            }
        } else {
            addAttachment(bundle)
        }
    }

    private fun showNextIncompatibleDialog() {
        val next = pendingIncompatibleBundles.firstOrNull() ?: return
        incompatibleFileNameDialogState = IncompatibleFileNameDialogState.Visible(
            sanitizedFileName = next.fileName.sanitizeIncompatibleFileNameCharacters(),
        )
    }

    fun onReplaceFileNameAutomatically() {
        val state = incompatibleFileNameDialogState as? IncompatibleFileNameDialogState.Visible ?: return
        incompatibleFileNameDialogState = IncompatibleFileNameDialogState.Hidden
        pendingIncompatibleBundles.removeFirstOrNull()?.let { bundle ->
            addAttachment(bundle.copy(fileName = state.sanitizedFileName))
        }
        showNextIncompatibleDialog()
    }

    fun onDismissIncompatibleFileNameDialog() {
        incompatibleFileNameDialogState = IncompatibleFileNameDialogState.Hidden
        pendingIncompatibleBundles.removeFirstOrNull()
        showNextIncompatibleDialog()
    }

    private fun addAttachment(bundle: AssetBundle) = viewModelScope.launch {
        addAttachment(
            conversationId = conversationId,
            fileName = bundle.fileName,
            assetPath = bundle.dataPath,
            assetSize = bundle.dataSize,
            mimeType = bundle.mimeType,
            assetMetadata = getMediaMetadata(bundle.dataPath, bundle.mimeType),
        )
            .onFailure {
                appLogger.e("Failed to add attachment: $it", tag = "MessageAttachmentsViewModel")
            }
    }

    fun onAttachmentMenuClicked(attachment: AttachmentDraftUi) {
        if (attachment.uploadError) {
            failedAttachmentDialogState = FailedAttachmentDialogState.Visible(
                attachment = attachment,
                showRetryOption = fileGateway.exists(attachment.localFilePath),
            )
        } else {
            deleteAttachment(attachment)
        }
    }

    private fun deleteAttachment(attachment: AttachmentDraftUi) = viewModelScope.launch {
        removedAttachments.update { it + attachment.uuid }
        removeAttachment(attachment.uuid)
            .onSuccess {
                removedAttachments.update { it - attachment.uuid }
            }
            .onFailure { error ->
                removedAttachments.update { it - attachment.uuid }
                appLogger.e("Failed to remove attachment: $error", tag = "MessageAttachmentsViewModel")
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

    fun onAttachmentClicked(attachment: AttachmentDraftUi) {
        if (attachment.uploadError) {
            failedAttachmentDialogState = FailedAttachmentDialogState.Visible(
                attachment = attachment,
                showRetryOption = fileGateway.exists(attachment.localFilePath),
            )
        } else {
            showAttachment(attachment)
        }
    }

    private fun showAttachment(attachment: AttachmentDraftUi) {
        fileGateway.open(attachment.localFilePath, attachment.fileName) {
            appLogger.e("Failed to open: ${attachment.localFilePath}", tag = "MessageAttachmentsViewModel")
        }
    }

    fun onFailedAttachmentDialogDismissed() {
        failedAttachmentDialogState = FailedAttachmentDialogState.Hidden
    }

    fun remove() {
        (failedAttachmentDialogState as? FailedAttachmentDialogState.Visible)?.let {
            failedAttachmentDialogState = FailedAttachmentDialogState.Hidden
            deleteAttachment(it.attachment)
        }
    }

    fun retryUpload() {
        (failedAttachmentDialogState as? FailedAttachmentDialogState.Visible)?.let { state ->
            failedAttachmentDialogState = FailedAttachmentDialogState.Hidden
            viewModelScope.launch {
                retryUpload(state.attachment.uuid)
                    .onSuccess {
                        observeUpload(state.attachment.uuid)
                    }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: ConversationNavArgs): MessageAttachmentsViewModel
    }
}

sealed interface FailedAttachmentDialogState {
    data object Hidden : FailedAttachmentDialogState
    data class Visible(
        val attachment: AttachmentDraftUi,
        val showRetryOption: Boolean,
    ) : FailedAttachmentDialogState
}

sealed interface IncompatibleFileNameDialogState {
    data object Hidden : IncompatibleFileNameDialogState
    data class Visible(val sanitizedFileName: String) : IncompatibleFileNameDialogState
}

private fun String.hasIncompatibleFileNameCharacters(): Boolean =
    this == "." || startsWith(".") || contains("/") || contains("\\") || contains("\"")

private fun String.sanitizeIncompatibleFileNameCharacters(): String =
    trimStart('.')
        .replace("/", "_")
        .replace("\\", "_")
        .replace("\"", "_")
        .ifEmpty { "file" }
