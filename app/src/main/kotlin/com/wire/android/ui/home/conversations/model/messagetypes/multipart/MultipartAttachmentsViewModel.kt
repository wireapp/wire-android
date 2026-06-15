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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.domain.model.AttachmentFileType.IMAGE
import com.wire.android.feature.cells.domain.model.AttachmentFileType.PDF
import com.wire.android.feature.cells.domain.model.AttachmentFileType.VIDEO
import com.wire.android.feature.cells.ui.CellFileLocalPathCache
import com.wire.android.feature.cells.ui.OpenFileDownloadController
import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.multipart.MultipartAttachmentOpenLoadState
import com.wire.android.ui.common.multipart.toUiModel
import com.wire.android.util.FileManager
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.offline.ObserveOfflineFilesUseCase
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.featureConfig.CollaboraEdition
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.CellAssetContent
import com.wire.kalium.logic.data.message.MessageAttachment
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import javax.inject.Inject

interface MultipartAttachmentsViewModel {
    val offlineAttachmentIds: StateFlow<Set<String>>
    val openLoadStates: StateFlow<Map<String, MultipartAttachmentOpenLoadState>>

    // Flow (not SharedFlow) so each error event is delivered to exactly one collector.
    // Multiple MultipartAttachmentsView composables share the same ViewModel instance
    // (keyed by conversationId), so using SharedFlow would broadcast to all of them.
    val openAttachmentErrorEvent: Flow<Unit>
    fun onClick(attachment: MultipartAttachmentUi, openInImageViewer: (String) -> Unit)
    fun mapAttachments(
        attachments: List<MessageAttachment>,
        offlineAttachmentIds: Set<String> = emptySet(),
        openLoadStates: Map<String, MultipartAttachmentOpenLoadState> = emptyMap(),
    ): List<MultipartAttachmentGroup> {

        val result = mutableListOf<MultipartAttachmentGroup>()
        var group: MultipartAttachmentGroup? = null

        attachments.forEach {
            val uiAttachment = it.toMappedUiAttachment(offlineAttachmentIds, openLoadStates)
            if (it.isMediaAttachment()) {
                group = when (group) {
                    null -> MultipartAttachmentGroup.Media(listOf(uiAttachment))
                    is MultipartAttachmentGroup.Media -> {
                        group.copy(attachments = group.attachments + uiAttachment)
                    }
                    else -> {
                        result.add(group)
                        MultipartAttachmentGroup.Media(listOf(uiAttachment))
                    }
                }
            } else {
                group = when (group) {
                    null -> MultipartAttachmentGroup.Files(listOf(uiAttachment))
                    is MultipartAttachmentGroup.Files -> {
                        group.copy(attachments = group.attachments + uiAttachment)
                    }
                    else -> {
                        result.add(group)
                        MultipartAttachmentGroup.Files(listOf(uiAttachment))
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

    fun onAttachmentsVisible(attachments: List<MessageAttachment>)
    fun onAttachmentsHidden(attachments: List<MessageAttachment>)
}

@Suppress("EmptyFunctionBlock")
object MultipartAttachmentsViewModelPreview : MultipartAttachmentsViewModel {
    override val offlineAttachmentIds: StateFlow<Set<String>> = MutableStateFlow(emptySet<String>())
    override val openLoadStates: StateFlow<Map<String, MultipartAttachmentOpenLoadState>> = MutableStateFlow(emptyMap())
    override val openAttachmentErrorEvent: Flow<Unit> = Channel<Unit>().receiveAsFlow()
    override fun onClick(attachment: MultipartAttachmentUi, openInImageViewer: (String) -> Unit) {}
    override fun onAttachmentsVisible(attachments: List<MessageAttachment>) {}
    override fun onAttachmentsHidden(attachments: List<MessageAttachment>) {}
}

@HiltViewModel
class MultipartAttachmentsViewModelImpl @Inject constructor(
    private val refreshHelper: CellAssetRefreshHelper,
    private val openFileDownloadController: OpenFileDownloadController,
    private val sharedPathCache: CellFileLocalPathCache,
    private val getEditorUrl: GetEditorUrlUseCase,
    private val onlineEditor: OnlineEditor,
    private val fileManager: FileManager,
    private val featureFlags: KaliumConfigs,
    private val getWireCellsConfig: GetWireCellConfigurationUseCase,
    observeOfflineFiles: ObserveOfflineFilesUseCase,
) : ViewModel(), MultipartAttachmentsViewModel {

    // Channel instead of SharedFlow: each error is delivered to exactly one collector,
    // preventing duplicate toasts when multiple message cards share this ViewModel.
    private val _openAttachmentErrorEvent = Channel<Unit>(Channel.BUFFERED)
    override val openAttachmentErrorEvent: Flow<Unit> = _openAttachmentErrorEvent.receiveAsFlow()

    // Map shared OpenLoadState (from cells) to MultipartAttachmentOpenLoadState.
    // Because CellFileLocalPathCache is @Singleton, state is shared with CellViewModel:
    // downloading from the Cells browser immediately reflects in conversation messages too.
    override val openLoadStates: StateFlow<Map<String, MultipartAttachmentOpenLoadState>> =
        sharedPathCache.openLoadStates
            .map { states -> states.mapValues { (_, state) -> state.toMultipartState() } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    override val offlineAttachmentIds: StateFlow<Set<String>> = observeOfflineFiles()
        .map { offlineFiles -> offlineFiles.mapTo(mutableSetOf()) { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private var isCollaboraEnabled: Boolean = false

    internal var conversationId: String? = null

    init {
        loadWireCellConfig()
    }

    override fun onClick(attachment: MultipartAttachmentUi, openInImageViewer: (String) -> Unit) {
        // Always use the authoritative shared-cache state — the `attachment` snapshot may be stale
        // if recomposition hasn't fired yet when the user taps.
        val currentLoadState = sharedPathCache.openLoadStates.value[attachment.uuid]

        if (currentLoadState != null) {
            when (currentLoadState) {
                is OpenLoadState.Loading -> openFileDownloadController.cancel(attachment.uuid, viewModelScope)
                is OpenLoadState.Ready -> openFileAtPath(currentLoadState.localPath.toString(), attachment)
                OpenLoadState.Error -> startDownload(attachment)
            }
            return
        }

        when {
            attachment.isImage() && !attachment.fileNotFound() -> openInImageViewer(attachment.uuid)
            attachment.isEditSupported && isCollaboraEnabled && featureFlags.collaboraIntegration ->
                openOnlineEditor(attachment.uuid)

            attachment.fileNotFound() -> {
                refreshHelper.refresh(attachment.uuid)
            }

            attachment.localFileAvailable() -> openLocalFile(attachment)
            attachment.canOpenWithUrl() -> openUrl(attachment)
            else -> startDownload(attachment)
        }
    }

    override fun onAttachmentsVisible(attachments: List<MessageAttachment>) {
        refreshHelper.onAttachmentsVisible(attachments)
    }

    override fun onAttachmentsHidden(attachments: List<MessageAttachment>) {
        refreshHelper.onAttachmentsHidden(attachments)
    }

    private fun openLocalFile(attachment: MultipartAttachmentUi) {
        fileManager.openWithExternalApp(
            assetDataPath = attachment.localPath?.toPath() ?: error("No local path"),
            assetName = attachment.fileName ?: "",
            mimeType = attachment.mimeType
        ) {
            appLogger.e("Failed to open: ${attachment.localPath}", tag = "MultipartAttachmentsViewModel")
            _openAttachmentErrorEvent.trySend(Unit)
        }
    }

    private fun openFileAtPath(localPath: String, attachment: MultipartAttachmentUi) {
        fileManager.openWithExternalApp(
            assetDataPath = localPath.toPath(),
            assetName = attachment.fileName ?: "",
            mimeType = attachment.mimeType
        ) {
            appLogger.e("Failed to open file at path: $localPath", tag = "MultipartAttachmentsViewModel")
            _openAttachmentErrorEvent.trySend(Unit)
        }
    }

    private fun openUrl(attachment: MultipartAttachmentUi) {
        fileManager.openUrlWithExternalApp(
            url = attachment.contentUrl ?: error("No preview URL"),
            mimeType = attachment.mimeType
        ) {
            appLogger.e("Failed to open: ${attachment.previewUrl}", tag = "MultipartAttachmentsViewModel")
            _openAttachmentErrorEvent.trySend(Unit)
        }
    }

    private fun startDownload(attachment: MultipartAttachmentUi) {
        openFileDownloadController.start(
            scope = viewModelScope,
            cellNode = attachment.toCellNode(conversationId),
            onOpenFile = { cellNode ->
                val localPath = cellNode.localPath ?: return@start
                openFileAtPath(localPath, attachment)
            },
            onError = { error ->
                appLogger.e("Download failed for ${attachment.uuid}: $error", tag = "MultipartAttachmentsViewModel")
                _openAttachmentErrorEvent.trySend(Unit)
            },
        )
    }

    private fun openOnlineEditor(attachmentUuid: String) = viewModelScope.launch {
        getEditorUrl(attachmentUuid)
            .onSuccess { url ->
                if (url != null) {
                    onlineEditor.open(url)
                }
            }
    }

    override fun onCleared() {
        _openAttachmentErrorEvent.close()
        refreshHelper.close()
    }

    private fun loadWireCellConfig() = viewModelScope.launch {
        val config = getWireCellsConfig()
        isCollaboraEnabled = config?.collabora != CollaboraEdition.NO
    }
}

internal fun MessageAttachment.assetId() =
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

/**
 * Maps [OpenLoadState] (cells-module type) to [MultipartAttachmentOpenLoadState].
 * [OpenLoadState.Loading] uses 0f to signal indeterminate; [MultipartAttachmentOpenLoadState.Loading] uses null.
 */
private fun OpenLoadState.toMultipartState(): MultipartAttachmentOpenLoadState = when (this) {
    is OpenLoadState.Loading -> MultipartAttachmentOpenLoadState.Loading(if (progress > 0f) progress else null)
    is OpenLoadState.Ready -> MultipartAttachmentOpenLoadState.Ready(localPath.toString())
    OpenLoadState.Error -> MultipartAttachmentOpenLoadState.Error
}

/**
 * Converts [MultipartAttachmentUi] to a minimal [CellNodeUi.File] for use with [OpenFileDownloadController].
 * Only fields used by the controller (uuid, name, size, conversationId) need to be set.
 */
private fun MultipartAttachmentUi.toCellNode(conversationId: String?): CellNodeUi.File = CellNodeUi.File(
    uuid = uuid,
    name = fileName,
    mimeType = mimeType,
    assetType = assetType,
    size = assetSize,
    localPath = localPath,
    conversationId = conversationId,
    userName = null,
    userHandle = null,
    ownerUserId = null,
    conversationName = null,
    modifiedTime = null,
    remotePath = null,
)

private fun MessageAttachment.toMappedUiAttachment(
    offlineAttachmentIds: Set<String>,
    openLoadStates: Map<String, MultipartAttachmentOpenLoadState>,
): MultipartAttachmentUi {
    val openLoadState = openLoadStates[assetId()]
    val loadingProgress = (openLoadState as? MultipartAttachmentOpenLoadState.Loading)?.progress
    val readyLocalPath = (openLoadState as? MultipartAttachmentOpenLoadState.Ready)?.localPath
    val base = toUiModel(
        progress = loadingProgress,
        isAvailableOffline = assetId() in offlineAttachmentIds,
    )
    return base.copy(
        openLoadState = openLoadState,
        localPath = readyLocalPath ?: base.localPath,
    )
}
