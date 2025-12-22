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
package com.wire.android.feature.cells.ui.create.createfolder

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.ui.common.FileNameError
import com.wire.android.feature.cells.ui.common.validateFileName
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.cells.domain.usecase.create.CreateFolderUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateFolderViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val createFolderUseCase: CreateFolderUseCase,
) : ActionsViewModel<CreateFolderViewModelAction>() {

    private val navArgs: CreateFolderScreenNavArgs = savedStateHandle.navArgs()

    val folderNameTextFieldState: TextFieldState = TextFieldState()

    var viewState: CreateFolderViewState by mutableStateOf(CreateFolderViewState())
        private set

    init {
        viewModelScope.launch {
            folderNameTextFieldState.textAsFlow().map { it.trim() }.collectLatest { name ->
                val fileValidationResult = name.validateFileName().takeIf { name.isNotEmpty() }
                viewState = viewState.copy(
                    saveEnabled = fileValidationResult == null && name.isNotEmpty(),
                    error = fileValidationResult
                )
            }
        }
    }

    internal fun createFolder(folderName: String) = viewModelScope.launch {

        viewState = viewState.copy(loading = true)

        createFolderUseCase("${navArgs.uuid}/${folderName.trim()}")
            .onSuccess {
                sendAction(CreateFolderViewModelAction.Success)
            }
            .onFailure {
                sendAction(CreateFolderViewModelAction.Failure)
            }

        viewState = viewState.copy(loading = false)
    }
}

sealed interface CreateFolderViewModelAction {
    data object Success : CreateFolderViewModelAction
    data object Failure : CreateFolderViewModelAction
}

data class CreateFolderViewState(
    val loading: Boolean = false,
    val saveEnabled: Boolean = false,
    val error: FileNameError? = null,
)
