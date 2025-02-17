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

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.kalium.cells.domain.usecase.ObserveCellFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CellViewModel @Inject constructor(
    private val observeCellFilesUseCase: ObserveCellFilesUseCase,
) : ViewModel() {

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    private val _state = MutableStateFlow(CellViewState())
    val state = _state.asStateFlow()

    fun loadFiles() {
        viewModelScope.launch {
            observeCellFilesUseCase().collectLatest { files ->

                val allFiles = files.map { conversation ->
                    buildList {
                        add(CellListHeader(conversation.conversationTitle))
                        conversation.files.map { file ->
                            add(
                                CellNodeItem(
                                    AttachmentDraftUi(
                                        uuid = file.uuid,
                                        fileName = file.path.substringAfterLast("/"),
                                        localFilePath = "",
                                        fileSize = file.size ?: 0,
                                        showDraftLabel = file.isDraft,
                                    )
                                )
                            )
                        }
                    }
                }.flatten()

                _state.update { currentState ->
                    currentState.copy(
                        files = allFiles
                    )
                }
            }
        }
    }
}

@Immutable
data class CellViewState(
    val files: List<CellsListItem> = emptyList(),
)

sealed interface CellsListItem
data class CellListHeader(val title: String) : CellsListItem
data class CellNodeItem(val node: AttachmentDraftUi) : CellsListItem
