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
import com.wire.kalium.logic.feature.cells.usecase.DeleteFromCellUseCase
import com.wire.kalium.logic.feature.cells.usecase.ListCellFilesUseCase
import com.wire.kalium.logic.feature.cells.usecase.UploadToCellUseCase
import com.wire.kalium.logic.functional.onFailure
import com.wire.kalium.logic.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CellViewModel @Inject constructor(
    private val listFilesUseCase: ListCellFilesUseCase,
    private val uploadUseCase: UploadToCellUseCase,
    private val deleteFromCellUseCase: DeleteFromCellUseCase,
    private val handleUriAssetUseCase: HandleUriAssetUseCase,
) : ViewModel() {

    private companion object {
        const val CELL = "wire-cells-android"
    }

    private val _uploadResult = MutableSharedFlow<String>()
    val uploadResultMessage = _uploadResult.asSharedFlow()

    private val _state = MutableStateFlow(CellViewState())
    val state = _state.asStateFlow()

    init {
        listFiles()
    }

    fun upload(uri: Uri) {
        viewModelScope.launch {

            when (val result = handleUriAssetUseCase.invoke(uri)) {
                is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> {
                    _uploadResult.emit("Asset too large")
                }

                is HandleUriAssetUseCase.Result.Failure.Unknown -> {
                    _uploadResult.emit("Unknown error")
                }

                is HandleUriAssetUseCase.Result.Success -> {

                    _state.update {
                        it.copy(
                            files = it.files + CellFile(
                                fileName = result.assetBundle.fileName,
                                uploadProgress = 0f
                            )
                        )
                    }

                    uploadUseCase(
                        cellName = CELL,
                        fileName = result.assetBundle.fileName,
                        path = result.assetBundle.dataPath,
                        onProgressUpdate = { uploaded ->
                            val progress = uploaded / result.assetBundle.dataSize.toFloat()
                            updateProgress(result.assetBundle.fileName, progress)
                        }
                    )
                        .onSuccess {
                            _uploadResult.emit("Upload successful")
                            delay(500)
                            listFiles()
                        }
                        .onFailure { error ->
                            Log.e("CellViewModel", "Upload failed: $error")
                            _uploadResult.emit("Upload failed!")
                        }
                }
            }
        }
    }

    private fun updateProgress(fileName: String, progress: Float) {
        _state.update {
            it.copy(files = it.files.map { file ->
                if (file.fileName == fileName) {
                    file.copy(uploadProgress = progress)
                } else {
                    file
                }
            })
        }
    }

    private fun listFiles() {
        viewModelScope.launch {
            listFilesUseCase(CELL)
                .onSuccess { files ->
                    _state.update {
                        it.copy(files = files.map { file ->
                            CellFile(fileName = file.substringAfterLast("/"))
                        })
                    }
                }
                .onFailure { error ->
                    Log.e("CellViewModel", "Failed to list files: $error")
                }
        }
    }

    fun deleteFile(fileName: String) {
        viewModelScope.launch {
            deleteFromCellUseCase(CELL, fileName)
                .onSuccess {
                    listFiles()
                }
                .onFailure { error ->
                    Log.e("CellViewModel", "Failed to delete file: $error")
                }
        }
    }
}

data class CellViewState(
    val files: List<CellFile> = emptyList(),
)

data class CellFile(
    val fileName: String,
    val uploadProgress: Float? = null
)
