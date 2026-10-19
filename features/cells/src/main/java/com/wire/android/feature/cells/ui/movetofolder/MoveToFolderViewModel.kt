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

import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.ui.common.ActionsViewModel
import com.wire.kalium.cells.domain.model.NodeLocator
import com.wire.kalium.cells.domain.usecase.GetFoldersUseCase
import com.wire.kalium.cells.domain.usecase.MoveNodesUseCase
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MoveToFolderViewModel @AssistedInject constructor(
    @Assisted private val navArgs: MoveToFolderNavArgs,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val moveNodesUseCase: MoveNodesUseCase,
    private val nodeMoveEvents: CellNodeMoveEvents,
) : ActionsViewModel<MoveToFolderViewAction>() {

    @AssistedFactory
    interface Factory {
        fun create(navArgs: MoveToFolderNavArgs): MoveToFolderViewModel
    }

    private val currentPath: String = navArgs.currentPath
    private val nodesToMove: List<NodeToMove> = navArgs.nodesToMove
    private val uuidsToMove: Set<String> = nodesToMove.map { it.uuid }.toSet()

    // Moving into the folder the nodes are already in would be a no-op.
    private fun isMoveAllowedToCurrentPath(): Boolean =
        nodesToMove.none { it.path.substringBeforeLast("/") == currentPath }

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
                            // A folder can't be moved into itself, so the folders being moved are not offered as destinations.
                            folders = folders.filter { it.uuid !in uuidsToMove }.map { it.toUiModel() },
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

    /**
     * Moves all the nodes in a single request. There is no rollback: if it fails midway (e.g. lost connection),
     * the nodes already moved stay moved and nothing is retried.
     */
    fun onMoveToFolderClick() {
        if (isMoving()) return
        // Set synchronously so a second tap before the coroutine runs is ignored too.
        updateState { copy(screenState = MoveToFolderScreenState.LOADING_IN_FULL_SCREEN) }
        viewModelScope.launch { moveNodes() }
    }

    private suspend fun moveNodes() {
        val result = moveNodesUseCase(nodesToMove.map { NodeLocator(uuid = it.uuid, path = it.path) }, currentPath)
        // Refresh even on failure: without rollback, part of the nodes may have been moved.
        nodeMoveEvents.onNodesMoved(nodesToMove.map { it.path.substringBeforeLast("/") }.toSet() + currentPath)
        result
            .onSuccess {
                updateState { copy(screenState = MoveToFolderScreenState.SUCCESS) }
                sendAction(
                    MoveToFolderViewAction.Success(
                        movedCount = nodesToMove.size,
                        destinationFolderName = state.value.breadcrumbs.lastOrNull(),
                    )
                )
            }
            .onFailure { failure ->
                // Kept in the state, not sent as an action, so the error dialog survives configuration changes.
                updateState {
                    copy(
                        screenState = MoveToFolderScreenState.ERROR,
                        moveFailure = MoveFailure(
                            totalCount = nodesToMove.size,
                            isNoNetworkConnection = failure is NetworkFailure.NoNetworkConnection,
                        ),
                    )
                }
            }
    }

    /**
     * While a move is in flight, navigation is ignored: leaving the screen would clear this ViewModel and cancel the move
     * before the folders are refreshed and the result is reported.
     */
    fun isMoving(): Boolean = state.value.screenState == MoveToFolderScreenState.LOADING_IN_FULL_SCREEN

    private fun updateState(block: MoveToFolderViewState.() -> MoveToFolderViewState) {
        _state.update { currentState ->
            block(currentState)
        }
    }

    fun onBreadcrumbClick(index: Int) {
        if (isMoving()) return
        val steps = state.value.breadcrumbs.size - index - 1
        sendAction(MoveToFolderViewAction.NavigateToBreadcrumb(steps))
    }

    fun onCreateFolderClick() {
        if (isMoving()) return
        sendAction(MoveToFolderViewAction.OpenCreateFolderScreen(currentPath))
    }

    fun onFolderClick(folder: CellNodeUi.Folder) {
        if (isMoving()) return
        sendAction(
            MoveToFolderViewAction.OpenFolder(
                path = "$currentPath/${folder.name}",
                nodesToMove = nodesToMove,
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
    val moveFailure: MoveFailure? = null,
)

/**
 * The move of [totalCount] nodes failed. Without rollback, some of them may still have been moved.
 * @param isNoNetworkConnection the device was offline, as opposed to a server side error.
 */
data class MoveFailure(
    val totalCount: Int,
    val isNoNetworkConnection: Boolean,
)

sealed interface MoveToFolderViewAction {
    /** @param destinationFolderName null when moved into the conversation root, which has no folder name. */
    data class Success(val movedCount: Int, val destinationFolderName: String?) : MoveToFolderViewAction
    data class NavigateToBreadcrumb(val steps: Int) : MoveToFolderViewAction
    data class OpenCreateFolderScreen(val currentPath: String) : MoveToFolderViewAction
    data class OpenFolder(
        val path: String,
        val nodesToMove: List<NodeToMove>,
        val breadcrumbs: List<String>
    ) : MoveToFolderViewAction
}
