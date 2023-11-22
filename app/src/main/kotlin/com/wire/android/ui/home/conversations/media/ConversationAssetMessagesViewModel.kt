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
 *
 *
 */

package com.wire.android.ui.home.conversations.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.mapper.AssetMapper
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.asset.GetAssetMessagesByConversationUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase2
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class ConversationAssetMessagesViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val dispatchers: DispatcherProvider,
    private val getAssets: GetAssetMessagesByConversationUseCase,
    private val getPrivateAsset: GetMessageAssetUseCase2,
    private val assetMapper: AssetMapper,
) : SavedStateViewModel(savedStateHandle) {

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId

    var viewState by mutableStateOf(ConversationAssetMessagesViewState())
        private set

    init {
        loadAssets()
    }

    private fun loadAssets() = viewModelScope.launch {
        var continueLoading = true
        while (continueLoading) {
            val currentOffset = viewState.currentOffset
            val uiAssetList = withContext(dispatchers.io()) {
                getAssets.invoke(
                    conversationId = conversationId,
                    limit = BATCH_SIZE,
                    offset = currentOffset
                ).map(assetMapper::toUIAsset)
            }

            // imitate loading new asset batch
            viewState = viewState.copy(messages = viewState.messages.plus(uiAssetList).toImmutableList())

            if (uiAssetList.size >= BATCH_SIZE) {
                val uiMessages = uiAssetList.map { uiAsset ->
                        if (uiAsset.downloadedAssetPath == null) {
                            val assetPath = withContext(dispatchers.io()) {
                                when (val asset = getPrivateAsset.invoke(uiAsset.conversationId, uiAsset.messageId)) {
                                    is MessageAssetResult.Failure -> null
                                    is MessageAssetResult.Success -> asset.decodedAssetPath
                                }
                            }
                            uiAsset.copy(downloadedAssetPath = assetPath)
                        } else {
                            uiAsset
                        }
                    }

                viewState = viewState.copy(
                    messages = viewState.messages.dropLast(uiMessages.size).plus(uiMessages).toImmutableList(),
                    currentOffset = viewState.currentOffset + BATCH_SIZE
                )
                continueLoading = true
            } else {
                continueLoading = false
            }
        }
    }

    companion object {
        const val BATCH_SIZE = 10
    }
}
