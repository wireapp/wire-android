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
package com.wire.android.feature.cells.ui.movetofolder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.navigation.SavedStateViewModel
import com.wire.kalium.cells.domain.usecase.GetFoldersUseCase
import com.wire.kalium.cells.domain.usecase.MoveNodeUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoveToFolderViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val moveNodeUseCase: MoveNodeUseCase
) : SavedStateViewModel(savedStateHandle) {

    private val navArgs: MoveToFolderNavArgs = savedStateHandle.navArgs()

    private val _state: MutableStateFlow<MoveToFolderScreenState> =
        MutableStateFlow(MoveToFolderScreenState.LOADING_CONTENT)
    internal val state = _state.asStateFlow()

    private val _folders: MutableStateFlow<List<CellNodeUi.Folder>> = MutableStateFlow(listOf())
    internal val folders = _folders.asStateFlow()

    private val _actions = Channel<ActionUiEvent>(
        capacity = Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    internal val actions = _actions
        .receiveAsFlow()
        .flowOn(Dispatchers.Main.immediate)

    fun currentPath(): String = navArgs.currentPath
    fun nodeToMovePath(): String = navArgs.nodeToMovePath
    fun nodeUuid(): String = navArgs.uuid

    private fun sendAction(action: ActionUiEvent) {
        viewModelScope.launch { _actions.send(action) }
    }

    init {
        loadFolders()
    }

    fun loadFolders() {
        viewModelScope.launch {
            getFoldersUseCase(currentPath())
                .onSuccess { folders ->
                    _folders.emit(folders.map { it.toUiModel() })
                    _state.update { MoveToFolderScreenState.SUCCESS }
                }
                .onFailure { _ ->
                    _state.update { MoveToFolderScreenState.ERROR }
                }
        }
    }

    fun moveHere() {
        viewModelScope.launch {
            _state.update { MoveToFolderScreenState.LOADING_IN_FULL_SCREEN }
            moveNodeUseCase(nodeUuid(), nodeToMovePath(), currentPath())
                .onSuccess {
                    _state.update { MoveToFolderScreenState.SUCCESS }
                    sendAction(ActionUiEvent.MoveItemUiEvent.Success)
                }
                .onFailure {
                    _state.update { MoveToFolderScreenState.ERROR }
                    sendAction(ActionUiEvent.MoveItemUiEvent.Failure)
                }
        }
    }

    companion object {
        const val ALL_FOLDERS = ""
    }
}

sealed interface ActionUiEvent {
    sealed interface MoveItemUiEvent : ActionUiEvent {
        data object Success : MoveItemUiEvent
        data object Failure : MoveItemUiEvent
    }
}
