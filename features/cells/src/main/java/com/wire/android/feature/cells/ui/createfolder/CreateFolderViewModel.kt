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
package com.wire.android.feature.cells.ui.createfolder

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.feature.cells.ui.rename.RenameNodeViewModel.Companion.NAME_MAX_COUNT
import com.wire.android.model.DisplayNameState
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.cells.domain.usecase.CreateFolderUseCase
import com.wire.kalium.common.functional.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateFolderViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val createFolderUseCase: CreateFolderUseCase,
) : ViewModel() {

    private val navArgs: CreateFolderScreenNavArgs = savedStateHandle.navArgs()

    var createFolderState: CreateFolderState by mutableStateOf(CreateFolderState.Default)
        private set

    val fileNameTextFieldState: TextFieldState = TextFieldState()

    var displayNameState: DisplayNameState by mutableStateOf(DisplayNameState())
        private set

    init {
        viewModelScope.launch {
            fileNameTextFieldState.textAsFlow().collectLatest {
                displayNameState = displayNameState.copy(
                    saveEnabled = it.trim().isNotEmpty() && it.length <= NAME_MAX_COUNT &&
                            !it.contains("/") && !it.contains("."),
                    error = when {
                        it.length > NAME_MAX_COUNT -> DisplayNameState.NameError.TextFieldError.NameExceedLimitError
                        it.contains("/") || it.contains(".") -> DisplayNameState.NameError.TextFieldError.InvalidNameError
                        else -> DisplayNameState.NameError.None
                    }
                )
            }
        }
    }

    internal fun createFolder(folderName: String) {
        viewModelScope.launch {
            createFolderState = createFolderUseCase("${navArgs.uuid}/$folderName").fold(
                { CreateFolderState.Failure },
                { CreateFolderState.Success },
            )
        }
    }
}

enum class CreateFolderState {
    Default, Success, Failure
}
