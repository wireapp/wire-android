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
import com.wire.android.ui.cells.navArgs
import com.wire.android.ui.common.ActionsViewModel
import com.wire.kalium.cells.domain.usecase.GetFoldersUseCase
import com.wire.kalium.cells.domain.usecase.MoveNodeUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoveToFolderViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val moveNodeUseCase: MoveNodeUseCase
) : ActionsViewModel<MoveToFolderViewAction>() {

    private val navArgs: MoveToFolderNavArgs = savedStateHandle.navArgs()

    private val currentPath: String = navArgs.currentPath
    private val nodeToMovePath: String = navArgs.nodeToMovePath
    private val nodeUuid: String = navArgs.uuid

    private fun isMoveAllowedToCurrentPath(): Boolean {
        val nodePath = nodeToMovePath.substringBeforeLast("/")
        return currentPath != nodePath
    }

    private val _state: MutableStateFlow<MoveToFolderViewState> = MutableStateFlow(
        MoveToFolderViewState(
            isAllowedToMoveToCurrentPath = isMoveAllowedToCurrentPath(),
            breadcrumbs = navArgs.breadcrumbs.toList(),
            isInRootFolder = !currentPath.contains("/")
        )
    )
    internal val state = _state.asStateFlow()

    init {
        loadFolders()
    }

    fun loadFolders() {
        viewModelScope.launch {
            getFoldersUseCase(currentPath)
                .onSuccess { folders ->
                    updateState {
                        copy(
                            screenState = MoveToFolderScreenState.SUCCESS,
                            folders = folders.map { it.toUiModel() },
                            isAllowedToMoveToCurrentPath = isMoveAllowedToCurrentPath(),
                        )
                    }
                }
                .onFailure { _ ->
                    updateState {
                        copy(
                            screenState = MoveToFolderScreenState.ERROR,
                        )
                    }
                }
        }
    }

    fun onMoveToFolderClick() = viewModelScope.launch {
        updateState { copy(screenState = MoveToFolderScreenState.LOADING_IN_FULL_SCREEN) }
        moveNodeUseCase(nodeUuid, nodeToMovePath, currentPath)
            .onSuccess {
                updateState { copy(screenState = MoveToFolderScreenState.SUCCESS) }
                sendAction(MoveToFolderViewAction.Success)
            }
            .onFailure {
                updateState { copy(screenState = MoveToFolderScreenState.ERROR) }
                sendAction(MoveToFolderViewAction.Failure)
            }
    }

    private fun updateState(block: MoveToFolderViewState.() -> MoveToFolderViewState) {
        _state.update { currentState ->
            block(currentState)
        }
    }

    fun onBreadcrumbClick(index: Int) {
        val steps = state.value.breadcrumbs.size - index - 1
        sendAction(MoveToFolderViewAction.NavigateToBreadcrumb(steps))
    }

    fun onCreateFolderClick() {
        sendAction(MoveToFolderViewAction.OpenCreateFolderScreen(currentPath))
    }

    fun onFolderClick(folder: CellNodeUi.Folder) {
        sendAction(
            MoveToFolderViewAction.OpenFolder(
                path = "$currentPath/${folder.name}",
                nodePath = nodeToMovePath,
                nodeUuid = nodeUuid,
                breadcrumbs = folder.name?.let { state.value.breadcrumbs + it } ?: emptyList(),
            )
        )
    }
}

data class MoveToFolderViewState(
    val screenState: MoveToFolderScreenState = MoveToFolderScreenState.LOADING_CONTENT,
    val folders: List<CellNodeUi.Folder> = emptyList(),
    val breadcrumbs: List<String> = emptyList(),
    val isAllowedToMoveToCurrentPath: Boolean = false,
    val isInRootFolder: Boolean = false,
)

sealed interface MoveToFolderViewAction {
    data object Success : MoveToFolderViewAction
    data object Failure : MoveToFolderViewAction
    data class NavigateToBreadcrumb(val steps: Int) : MoveToFolderViewAction
    data class OpenCreateFolderScreen(val currentPath: String) : MoveToFolderViewAction
    data class OpenFolder(
        val path: String,
        val nodePath: String,
        val nodeUuid: String,
        val breadcrumbs: List<String>
    ) : MoveToFolderViewAction
}
