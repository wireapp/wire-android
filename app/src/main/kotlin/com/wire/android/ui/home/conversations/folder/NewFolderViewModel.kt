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
package com.wire.android.ui.home.conversations.folder

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.model.SnackBarMessage
import com.wire.android.model.asSnackBarMessage
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.feature.conversation.folder.CreateConversationFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveUserFoldersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewFolderViewModel @Inject constructor(
    private val observeUserFolders: ObserveUserFoldersUseCase,
    private val createConversationFolder: CreateConversationFolderUseCase
) : ViewModel() {

    val textState: TextFieldState = TextFieldState()
    var folderNameState: FolderNameState by mutableStateOf(FolderNameState())
        private set

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                observeUserFolders(),
                textState.textAsFlow()
                    .dropWhile { it.isEmpty() }, // ignore first empty value to not show the error before the user typed anything
                ::Pair
            )
                .collect { (folders, text) ->
                    val nameExist = folders.any { it.name == text.trim() }
                    folderNameState = folderNameState.copy(
                        buttonEnabled = text.trim().isNotEmpty() && !nameExist && text.length <= NAME_MAX_COUNT,
                        error = when {
                            text.trim().isEmpty() -> FolderNameState.NameError.TextFieldError.NameEmptyError
                            text.length > NAME_MAX_COUNT -> FolderNameState.NameError.TextFieldError.NameExceedLimitError
                            nameExist -> FolderNameState.NameError.TextFieldError.NameAlreadyExistError
                            else -> FolderNameState.NameError.None
                        }
                    )
                }
        }
    }

    fun createFolder(folderName: String) {
        viewModelScope.launch {
            when (val result = createConversationFolder(folderName)) {
                is CreateConversationFolderUseCase.Result.Failure -> {
                    _infoMessage.emit(
                        UIText.StringResource(
                            R.string.new_folder_failure,
                            folderName,
                        ).asSnackBarMessage()
                    )
                }

                is CreateConversationFolderUseCase.Result.Success -> {
                    folderNameState = folderNameState.copy(
                        folderId = result.folderId
                    )
                }
            }
        }
    }

    companion object {
        const val NAME_MAX_COUNT = 64
    }
}

data class FolderNameState(
    val folderId: String? = null,
    val loading: Boolean = false,
    val buttonEnabled: Boolean = false,
    val error: NameError = NameError.None,
) {
    sealed interface NameError {
        data object None : NameError
        sealed interface TextFieldError : NameError {
            data object NameEmptyError : TextFieldError
            data object NameExceedLimitError : TextFieldError
            data object NameAlreadyExistError : TextFieldError
        }
    }
}
