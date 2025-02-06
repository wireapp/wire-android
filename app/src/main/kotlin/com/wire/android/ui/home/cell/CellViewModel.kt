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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.kalium.logic.data.cells.CellNode
import com.wire.kalium.logic.feature.cells.usecase.CancelDraftUseCase
import com.wire.kalium.logic.feature.cells.usecase.DeleteCellFileUseCase
import com.wire.kalium.logic.feature.cells.usecase.GetCellFilesUseCase
import com.wire.kalium.logic.feature.cells.usecase.PublishDraftUseCase
import com.wire.kalium.logic.feature.cells.usecase.UploadToCellUseCase
import com.wire.kalium.logic.functional.onFailure
import com.wire.kalium.logic.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CellViewModel @Inject constructor(
    private val listFilesUseCase: GetCellFilesUseCase,
    private val uploadUseCase: UploadToCellUseCase,
    private val deleteUseCase: DeleteCellFileUseCase,
    private val publishDraftUseCase: PublishDraftUseCase,
    private val cancelDraftUseCase: CancelDraftUseCase,
    private val handleUriAssetUseCase: HandleUriAssetUseCase,
) : ViewModel() {

    private companion object {
        const val CELL = "wire-cells-android"
        const val LIST_DELAY = 500L
    }

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    private val _state = MutableStateFlow(CellViewState())
    val state = _state.asStateFlow()

    private val uploadData = mutableMapOf<String, UploadData>()

    fun upload(uri: Uri) {
        viewModelScope.launch {
            when (val result = handleUriAssetUseCase.invoke(uri)) {
                is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> {
                    _uiMessage.emit("Asset too large")
                }

                is HandleUriAssetUseCase.Result.Failure.Unknown -> {
                    _uiMessage.emit("Unknown error")
                }

                is HandleUriAssetUseCase.Result.Success -> {

                    val node = CellNode(
                        uuid = UUID.randomUUID().toString(),
                        versionId = UUID.randomUUID().toString(),
                        path = "$CELL/${result.assetBundle.fileName}"
                    )

                    _state.update {
                        it.copy(
                            files = it.files + CellNodeUi(
                                node = node,
                                fileName = result.assetBundle.fileName,
                                uploadProgress = 0f
                            )
                        )
                    }

                    val uploadJob = uploadUseCase(
                        path = result.assetBundle.dataPath,
                        node = node,
                        size = result.assetBundle.dataSize,
                        progress = { progress ->
                            updateProgress(node.uuid, progress)
                        }
                    )

                    uploadData[node.uuid] = UploadData(
                        node = node,
                        progress = 0f,
                        job = uploadJob
                    )

                    uploadJob.await()
                        .onSuccess {
                            _uiMessage.emit("Upload successful")
                            uploadData.remove(node.uuid)
                            listFiles()
                        }
                        .onFailure { error ->
                            Log.e("CellViewModel", "Upload failed: $error")
                            _uiMessage.emit("Upload failed!")
                            setError(node.uuid)
                        }
                }
            }
        }
    }

    private fun updateProgress(uuid: String, progress: Float) {
        _state.update {
            it.copy(
                files = it.files.map { file ->
                    if (file.node.uuid == uuid) {
                        file.copy(uploadProgress = progress)
                    } else {
                        file
                    }
                }
            )
        }

        uploadData[uuid]?.let {
            uploadData[uuid] = it.copy(progress = progress)
        }
    }

    private fun setError(uuid: String) {
        _state.update {
            it.copy(
                files = it.files.map { file ->
                    if (file.node.uuid == uuid) {
                        file.copy(uploadError = true)
                    } else {
                        file
                    }
                }
            )
        }

        uploadData[uuid]?.let {
            uploadData[uuid] = it.copy(uploadError = true)
        }
    }

    internal fun listFiles() {
        viewModelScope.launch {
            listFilesUseCase(CELL)
                .onSuccess { files ->
                    _state.update {
                        it.copy(
                            files = files.map { node ->
                                if (uploadData.containsKey(node.uuid)) {
                                    CellNodeUi(
                                        node = node,
                                        fileName = node.path.substringAfterLast("/"),
                                        uploadProgress = uploadData[node.uuid]?.progress,
                                        uploadError = uploadData[node.uuid]?.uploadError ?: false
                                    )
                                } else {
                                    CellNodeUi(
                                        node = node,
                                        fileName = node.path.substringAfterLast("/"),
                                    )
                                }
                            }
                        )
                    }
                }
                .onFailure { error ->
                    Log.e("CellViewModel", "Failed to list files: $error")
                    _uiMessage.emit("Failed to list files")
                }
        }
    }

    fun deleteFile(uiNode: CellNodeUi) {
        viewModelScope.launch {

            val uploadData = uploadData[uiNode.node.uuid]

            if (uploadData != null) {
                uploadData.job.cancel()
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
                        delay(LIST_DELAY)
                        listFiles()
                    }
            }
        }
    }

    fun onFileClick(uiNode: CellNodeUi) {
        if (uiNode.node.isDraft && !uploadData.containsKey(uiNode.node.uuid)) {
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
}

data class CellViewState(
    val files: List<CellNodeUi> = emptyList(),
)

data class CellNodeUi(
    val node: CellNode,
    val fileName: String,
    val uploadProgress: Float? = null,
    val uploadError: Boolean = false,
)

private data class UploadData(
    val node: CellNode,
    val progress: Float,
    val uploadError: Boolean = false,
    val job: Job,
)
