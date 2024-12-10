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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelScopedPreview
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
interface ConversationFoldersVM {
    fun state(): ConversationFoldersState = ConversationFoldersState(persistentListOf())
}

@HiltViewModel
class ConversationFoldersVMImpl @Inject constructor(
    private val observeUserFoldersUseCase: ObserveUserFoldersUseCase,
) : ConversationFoldersVM, ViewModel() {

    private var state by mutableStateOf(ConversationFoldersState(persistentListOf()))

    override fun state(): ConversationFoldersState = state

    init {
        observeUserFolders()
    }

    private fun observeUserFolders() = viewModelScope.launch {
        observeUserFoldersUseCase()
            .collect { folders ->
                state = ConversationFoldersState(folders.toPersistentList())
            }
    }
}

data class ConversationFoldersState(val folders: PersistentList<ConversationFolder>)

@Serializable
object ConversationFoldersStateArgs : ScopedArgs {
    override val key = "ConversationFoldersStateArgsKey"
}
