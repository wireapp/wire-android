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
package com.wire.android.feature.cells.ui.rename

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.model.DisplayNameState
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.cells.domain.usecase.RenameNodeUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RenameNodeViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val renameNodeUseCase: RenameNodeUseCase,
) : ActionsViewModel<RenameNodeViewModelAction>() {

    private val navArgs: RenameNodeNavArgs = savedStateHandle.navArgs()

    fun isFolder(): Boolean? = navArgs.isFolder

    val textState: TextFieldState = TextFieldState(navArgs.nodeName ?: "")
    var displayNameState: DisplayNameState by mutableStateOf(DisplayNameState())
        private set

    init {
        viewModelScope.launch {
            textState.textAsFlow().collectLatest {
                displayNameState = displayNameState.copy(
                    saveEnabled = it.trim().isNotEmpty() && it.length <= NAME_MAX_COUNT && it.trim() != navArgs.nodeName,
                    error = when {
                        it.trim().isEmpty() -> DisplayNameState.NameError.TextFieldError.NameEmptyError
                        it.length > NAME_MAX_COUNT -> DisplayNameState.NameError.TextFieldError.NameExceedLimitError
                        else -> DisplayNameState.NameError.None
                    }
                )
            }
        }
    }

    fun renameNode() {
        displayNameState = displayNameState.copy(loading = true)
        viewModelScope.launch {
            renameNodeUseCase.invoke(navArgs.uuid!!, navArgs.currentPath!!, textState.text.toString())
                .onSuccess {
                    displayNameState = displayNameState.copy(
                        loading = false,
                        completed = DisplayNameState.Completed.Success,
                    )
                    sendAction(RenameNodeViewModelAction.Success)
                }
                .onFailure {
                    displayNameState = displayNameState.copy(
                        loading = false,
                        completed = DisplayNameState.Completed.Failure,
                    )
                    sendAction(RenameNodeViewModelAction.Failure)
                }
        }
    }

    companion object {
        const val NAME_MAX_COUNT = 64
    }
}

sealed interface RenameNodeViewModelAction {
    data object Success : RenameNodeViewModelAction
    data object Failure : RenameNodeViewModelAction
}
