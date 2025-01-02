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
import androidx.lifecycle.ViewModel
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@ViewModelScopedPreview
interface MoveConversationToFolderVM {
    val infoMessage: SharedFlow<UIText>
        get() = MutableSharedFlow()
    fun actionableState(): MoveConversationToFolderState = MoveConversationToFolderState()
}

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class MoveConversationToFolderVMImpl @Inject constructor(
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle
) : MoveConversationToFolderVM, ViewModel() {

    var state: MoveConversationToFolderState by mutableStateOf(MoveConversationToFolderState())

    private val _infoMessage = MutableSharedFlow<UIText>()
    override val infoMessage = _infoMessage.asSharedFlow()

    override fun actionableState(): MoveConversationToFolderState = state

}

data class MoveConversationToFolderState(
    val isPerformingAction: Boolean = false
)

