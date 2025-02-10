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
package com.wire.android.ui.home.cell

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.kalium.cells.domain.CellUploadEvent
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.model.CellNode
import com.wire.kalium.cells.domain.usecase.CancelDraftUseCase
import com.wire.kalium.cells.domain.usecase.DeleteCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetCellFilesUseCase
import com.wire.kalium.cells.domain.usecase.PublishDraftUseCase
import com.wire.kalium.logic.functional.onFailure
import com.wire.kalium.logic.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CellViewModel @Inject constructor(
    private val listFilesUseCase: GetCellFilesUseCase,
    private val deleteUseCase: DeleteCellFileUseCase,
    private val publishDraftUseCase: PublishDraftUseCase,
    private val cancelDraftUseCase: CancelDraftUseCase,
    private val handleUriAssetUseCase: HandleUriAssetUseCase,
    private val uploadManager: CellUploadManager,
) : ViewModel() {

    private companion object {
        const val CELL = "wire-cells-android"
        const val LIST_DELAY = 700L
    }

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    private val _state = MutableStateFlow(CellViewState())
    val state = _state.asStateFlow()

    private val uploadObservers = mutableMapOf<String, Job>()

    fun listFiles() {
        viewModelScope.launch {
            listFilesUseCase(CELL)
                .onSuccess { files ->
                    _state.update { currentState ->
                        currentState.copy(
                            files = files.mapToUiNodes()
                        )
                    }
                }
                .onFailure { error ->
                    Log.e("CellViewModel", "Failed to list files: $error")
                    _uiMessage.emit("Failed to list files")
                }
        }
    }

    fun upload(uris: List<Uri>) = viewModelScope.launch {
        uris.forEach { uri ->
            when (val result = handleUriAssetUseCase.invoke(uri)) {
                is HandleUriAssetUseCase.Result.Success ->
                    with(result.assetBundle) {
                        uploadManager.upload(dataPath, dataSize, "$CELL/$fileName")
                            .onSuccess { node ->
                                addNewNode(node)
                                observeUpload(node.uuid)
                            }
                            .onFailure { error ->
                                Log.e("CellViewModel", "Upload failed: $error")
                                _uiMessage.emit("Upload failed!")
                            }
                    }

                is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> {
                    _uiMessage.emit("Asset too large")
                }

                is HandleUriAssetUseCase.Result.Failure.Unknown -> {
                    _uiMessage.emit("Unknown error")
                }
            }
        }
    }

    fun deleteFile(uiNode: CellNodeUi) {

        _state.update { currentState ->
            currentState.copy(
                files = currentState.files.filter { it.node.uuid != uiNode.node.uuid }
            )
        }

        viewModelScope.launch {

            val uploadData = uploadManager.getUploadInfo(uiNode.node.uuid)

            if (uploadData != null) {
                uploadObservers[uiNode.node.uuid]?.cancel()
                uploadManager.cancelUpload(uiNode.node.uuid)
                listFiles()
            } else {
                if (uiNode.node.isDraft) {
                    cancelDraftUseCase(uiNode.node)
                } else {
                    deleteUseCase(uiNode.node)
                }
                    .onSuccess {
                        delay(LIST_DELAY)
                        listFiles()
                    }
                    .onFailure { error ->
                        Log.e("CellViewModel", "Failed to delete file: $error")
                        _uiMessage.emit("Failed to delete file")
                        listFiles()
                    }
            }
        }
    }

    fun onFileClick(uiNode: CellNodeUi) {
        val uploadInfo = uploadManager.getUploadInfo(uiNode.node.uuid)
        if (uiNode.node.isDraft && uploadInfo == null) {
            viewModelScope.launch {
                publishDraftUseCase(uiNode.node)
                    .onSuccess {
                        listFiles()
                        _uiMessage.emit("Draft published")
                    }
                    .onFailure { error ->
                        Log.e("CellViewModel", "Failed to publish draft: $error")
                        _uiMessage.emit("Failed to publish draft")
                    }
            }
        }
    }

    private fun addNewNode(node: CellNode) {
        _state.update { currentState ->
            currentState.copy(
                files = currentState.files + CellNodeUi(
                    node = node,
                    fileName = node.path.substringAfterLast("/"),
                    fileSize = node.size ?: 0,
                    uploadProgress = 0f
                )
            )
        }
    }

    private fun observeUpload(uuid: String) {

        uploadObservers[uuid]?.cancel()

        val observer = uploadManager.observeUpload(uuid)?.onEach { event ->
            when (event) {
                is CellUploadEvent.UploadProgress -> updateFile(uuid) { copy(uploadProgress = event.progress) }

                CellUploadEvent.UploadCompleted -> {
                    listFiles()
                }

                CellUploadEvent.UploadError -> updateFile(uuid) { copy(uploadError = true) }
            }
        }?.launchIn(viewModelScope)

        observer?.let {
            uploadObservers[uuid] = it
        }
    }

    private fun updateFile(uuid: String, block: CellNodeUi.() -> CellNodeUi) {
        _state.update {
            it.copy(
                files = it.files.map { file ->
                    if (file.node.uuid == uuid) {
                        block(file)
                    } else {
                        file
                    }
                }
            )
        }
    }

    private fun List<CellNode>.mapToUiNodes(): List<CellNodeUi> =
            map { node ->

                val uploadInfo = uploadManager.getUploadInfo(node.uuid)

                if (uploadInfo != null) {

                    observeUpload(node.uuid)

                    CellNodeUi(
                        node = node,
                        fileName = node.path.substringAfterLast("/"),
                        fileSize = node.size ?: 0,
                        uploadProgress = uploadInfo.progress,
                        uploadError = uploadInfo.uploadFiled
                    )
                } else {
                    CellNodeUi(
                        node = node,
                        fileName = node.path.substringAfterLast("/"),
                        fileSize = node.size ?: 0
                    )
                }
            }

    override fun onCleared() {
        cancelObservers()
        super.onCleared()
    }

    fun cancelObservers() {
        uploadObservers.values.forEach { it.cancel() }
    }
}

@Immutable
data class CellViewState(
    val files: List<CellNodeUi> = emptyList(),
)

data class CellNodeUi(
    val node: CellNode,
    val fileName: String,
    val fileSize: Long = 0,
    val uploadProgress: Float? = null,
    val uploadError: Boolean = false,
)
