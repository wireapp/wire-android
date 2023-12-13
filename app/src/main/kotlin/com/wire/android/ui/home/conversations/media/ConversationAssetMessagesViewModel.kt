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
import com.wire.android.ui.home.conversations.usecase.GetAssetMessagesFromConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.feature.asset.GetImageAssetMessagesForConversationUseCase
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
    private val getImageMessages: GetImageAssetMessagesForConversationUseCase,
    private val getAssetMessages: GetAssetMessagesFromConversationUseCase,
    private val getPrivateAsset: GetMessageAssetUseCase,
    private val assetMapper: UIAssetMapper,
) : SavedStateViewModel(savedStateHandle) {

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId

    var viewState by mutableStateOf(ConversationAssetMessagesViewState())
        private set

    private var continueLoading = true
    private var isLoading = false
    private var currentOffset: Int = 0

    init {
        loadImages()
        loadAssets()
    }

    private fun loadAssets() = viewModelScope.launch {
        val assetMessages = withContext(dispatchers.io()) {
            getAssetMessages.invoke(
                conversationId = conversationId
            )
        }

        viewState = viewState.copy(
            assetMessages = assetMessages
        )
    }

    fun continueLoading(shouldContinue: Boolean) {
        if (shouldContinue) {
            if (!continueLoading) {
                continueLoading = true
                loadImages()
            }
        } else {
            continueLoading = false
        }
    }

    private fun loadImages() = viewModelScope.launch {
        if (isLoading) {
            return@launch
        }
        isLoading = true
        try {
            while (continueLoading) {
                val uiAssetList = withContext(dispatchers.io()) {
                    getImageMessages.invoke(
                        conversationId = conversationId,
                        limit = BATCH_SIZE,
                        offset = currentOffset
                    ).map(assetMapper::toUIAsset)
                }

                // imitate loading new asset batch
                viewState = viewState.copy(imageMessages = viewState.imageMessages.plus(uiAssetList.map {
                    it.copy(
                        downloadStatus = if (it.assetPath == null && it.downloadStatus != Message.DownloadStatus.FAILED_DOWNLOAD) {
                            Message.DownloadStatus.DOWNLOAD_IN_PROGRESS
                        } else {
                            it.downloadStatus
                        }
                    )
                }).toImmutableList())

                if (uiAssetList.size >= BATCH_SIZE) {
                    val uiMessages = uiAssetList.map { uiAsset ->
                        if (uiAsset.assetPath == null) {
                            val assetPath = withContext(dispatchers.io()) {
                                when (val asset = getPrivateAsset.invoke(uiAsset.conversationId, uiAsset.messageId).await()) {
                                    is MessageAssetResult.Failure -> null
                                    is MessageAssetResult.Success -> asset.decodedAssetPath
                                }
                            }
                            uiAsset.copy(assetPath = assetPath)
                        } else {
                            uiAsset
                        }
                    }
                    currentOffset += BATCH_SIZE

                    viewState = viewState.copy(
                        imageMessages = viewState.imageMessages.dropLast(uiMessages.size).plus(uiMessages).toImmutableList(),
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
