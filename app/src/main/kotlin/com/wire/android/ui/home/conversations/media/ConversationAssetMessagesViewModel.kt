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
import com.wire.android.mapper.UIAssetMapper
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.feature.asset.GetAssetMessagesForConversationUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
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
    private val getAssets: GetAssetMessagesForConversationUseCase,
    private val getPrivateAsset: GetMessageAssetUseCase,
    private val assetMapper: UIAssetMapper,
) : SavedStateViewModel(savedStateHandle) {

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId

    var viewState by mutableStateOf(ConversationAssetMessagesViewState())
        private set

    private var continueLoading by mutableStateOf(true)
    private var isLoading by mutableStateOf(false)

    init {
        loadAssets()
    }

    fun continueLoading(shouldContinue: Boolean) {
        if (shouldContinue) {
            if (!continueLoading) {
                continueLoading = true
                loadAssets()
            }
        } else {
            continueLoading = false
        }

    }

    private fun loadAssets() = viewModelScope.launch {
        if (isLoading)
            return@launch
        isLoading = true
        try {
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
                viewState = viewState.copy(messages = viewState.messages.plus(uiAssetList.map {
                    it.copy(
                        downloadStatus = if (it.downloadedAssetPath == null && it.downloadStatus != Message.DownloadStatus.FAILED_DOWNLOAD)
                            Message.DownloadStatus.DOWNLOAD_IN_PROGRESS else it.downloadStatus
                    )
                }).toImmutableList())

                if (uiAssetList.size >= BATCH_SIZE) {
                    val uiMessages = uiAssetList.map { uiAsset ->
                        if (uiAsset.downloadedAssetPath == null) {
                            val assetPath = withContext(dispatchers.io()) {
                                when (val asset = getPrivateAsset.invoke(uiAsset.conversationId, uiAsset.messageId).await()) {
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
                } else {
                    continueLoading = false
                }
            }
        } finally {
            isLoading = false
        }
    }

    companion object {
        const val BATCH_SIZE = 5
    }
}
