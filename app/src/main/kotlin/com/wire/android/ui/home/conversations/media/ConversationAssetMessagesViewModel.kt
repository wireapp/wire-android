/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.usecase.GetAssetMessagesFromConversationUseCase
import com.wire.android.ui.home.conversations.usecase.GetImageAssetMessagesFromConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.QualifiedID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class ConversationAssetMessagesViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val getImageMessages: GetImageAssetMessagesFromConversationUseCase,
    private val getAssetMessages: GetAssetMessagesFromConversationUseCase,
) : SavedStateViewModel(savedStateHandle) {

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId

    var viewState by mutableStateOf(ConversationAssetMessagesViewState())
        private set

    init {
        loadImages()
        loadAssets()
    }

    private fun loadAssets() = viewModelScope.launch {
        val assetsResult = getAssetMessages.invoke(
            conversationId = conversationId,
            initialOffset = 0
        )

        viewState = viewState.copy(
            assetMessages = assetsResult
        )
    }

    private fun loadImages() = viewModelScope.launch {
        val imageAssetsResult = getImageMessages.invoke(
            conversationId = conversationId,
            initialOffset = 0
        )

        viewState = viewState.copy(
            imageMessages = imageAssetsResult
        )
    }
}
