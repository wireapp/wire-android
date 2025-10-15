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
import com.wire.android.feature.cells.ui.rename.RenameNodeViewState.RenameError.None
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.cells.domain.usecase.RenameNodeFailure
import com.wire.kalium.cells.domain.usecase.RenameNodeUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.util.splitFileExtension
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class RenameNodeViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val renameNodeUseCase: RenameNodeUseCase,
) : ActionsViewModel<RenameNodeViewModelAction>() {

    private val navArgs: RenameNodeNavArgs = savedStateHandle.navArgs()

    private var clearErrorJob: Job? = null

    fun isFolder(): Boolean? = navArgs.isFolder

    private val originalFileName = navArgs.nodeName?.splitFileExtension()?.first ?: ""
    private val fileExtension: String = navArgs.nodeName?.splitFileExtension()?.second ?: ""
    val textState: TextFieldState = TextFieldState(navArgs.nodeName?.splitFileExtension()?.first ?: "")

    internal var viewState by mutableStateOf(RenameNodeViewState())
        private set

    init {
        viewModelScope.launch {
            textState.textAsFlow().collectLatest { name ->
                val validationError = name.validate()
                viewState = viewState.copy(
                        saveEnabled = validationError == None && name.trim() != originalFileName,
                        error = validationError,
                    )
            }
        }
    }

    fun renameNode(newName: String) {
        viewState = viewState.copy(loading = true)
        viewModelScope.launch {
            val newNameWithExtension = newName.trim() + fileExtension.takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty()
            renameNodeUseCase.invoke(
                uuid = navArgs.uuid!!,
                path = navArgs.currentPath!!,
                newName = newNameWithExtension
            )
                .onSuccess {
                    viewState = viewState.copy(
                            loading = false,
                            completed = RenameNodeViewState.Completed.Success,
                        )
                    sendAction(RenameNodeViewModelAction.Success)
                }
                .onFailure { failure ->
                    when (failure) {
                        RenameNodeFailure.FileAlreadyExists ->
                            viewState = viewState.copy(
                                    loading = false,
                                    error = RenameNodeViewState.RenameError.TextFieldError.NameAlreadyExist,
                                    completed = RenameNodeViewState.Completed.Failure,
                                )
                        else -> sendAction(RenameNodeViewModelAction.Failure)
                    }
                }
        }
    }

    private fun CharSequence.validate() = when {
        length > NAME_MAX_COUNT -> RenameNodeViewState.RenameError.TextFieldError.NameExceedLimit
        trim().isEmpty() -> RenameNodeViewState.RenameError.TextFieldError.NameEmpty
        contains("/") || contains(".") -> RenameNodeViewState.RenameError.TextFieldError.InvalidName
        else -> None
    }

    internal fun onMaxLengthExceeded() {
        viewState = viewState.copy(
                error = RenameNodeViewState.RenameError.TextFieldError.NameExceedLimit
            )
        clearErrorJob?.cancel()
        clearErrorJob = viewModelScope.launch {
            delay(2.seconds)
            viewState = viewState.copy(error = None)
        }
    }

    companion object {
        const val NAME_MAX_COUNT = 64
    }
}

internal data class RenameNodeViewState(
    val loading: Boolean = false,
    val saveEnabled: Boolean = false,
    val error: RenameError = None,
    val completed: Completed = Completed.None,
) {
    enum class Completed {
        None, Success, Failure
    }
    sealed interface RenameError {
        data object None : RenameError
        sealed interface TextFieldError : RenameError {
            data object NameEmpty : TextFieldError
            data object NameExceedLimit : TextFieldError
            data object NameAlreadyExist : TextFieldError
            data object InvalidName : TextFieldError
        }
    }
}

sealed interface RenameNodeViewModelAction {
    data object Success : RenameNodeViewModelAction
    data object Failure : RenameNodeViewModelAction
}
