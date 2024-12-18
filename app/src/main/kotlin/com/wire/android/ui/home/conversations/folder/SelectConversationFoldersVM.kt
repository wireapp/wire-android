/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.conversation.ConversationFolder
import com.wire.kalium.logic.feature.conversation.folder.ObserveUserFoldersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@ViewModelScopedPreview
interface SelectConversationFoldersVM {
    fun state(): SelectConversationFoldersState = SelectConversationFoldersState(persistentListOf())
}

@HiltViewModel
class SelectConversationFoldersVMImpl @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val observeUserFoldersUseCase: ObserveUserFoldersUseCase,
) : SelectConversationFoldersVM, SavedStateViewModel(savedStateHandle) {

    private var state by mutableStateOf(SelectConversationFoldersState(persistentListOf()))

    override fun state(): SelectConversationFoldersState = state

    private val navArgs: SelectConversationFoldersStateArgs = savedStateHandle.navArgs()
    private val currentFolderId = navArgs.currentFolderId

    init {
        observeUserFolders()
    }

    private fun observeUserFolders() = viewModelScope.launch {
        observeUserFoldersUseCase()
            .collect { folders ->
                state = SelectConversationFoldersState(folders.toPersistentList(), currentFolderId)
            }
    }
}

data class SelectConversationFoldersState(val folders: PersistentList<ConversationFolder>, val currentFolderId: String? = null)

@Serializable
data class SelectConversationFoldersStateArgs(val currentFolderId: String?) : ScopedArgs {
    override val key = "$ARGS_KEY:$currentFolderId"

    companion object {
        const val ARGS_KEY = "SelectConversationFoldersStateArgsKey"
    }
}
