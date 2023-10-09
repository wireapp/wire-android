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

package com.wire.android.ui.home.conversations.messages

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.android.util.FileManager
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.asset.UpdateDownloadStatusResult
import com.wire.kalium.logic.feature.conversation.GetConversationUnreadEventsCountUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.ToggleReactionUseCase
import com.wire.kalium.logic.feature.sessionreset.ResetSessionResult
import com.wire.kalium.logic.feature.sessionreset.ResetSessionUseCase
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import okio.Path

class ConversationMessagesViewModelArrangement {

    val conversationId: ConversationId = ConversationId("some-dummy-value", "some.dummy.domain")

    private val messagesChannel = Channel<PagingData<UIMessage>>(capacity = Channel.UNLIMITED)

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var getMessagesForConversationUseCase: GetMessagesForConversationUseCase

    @MockK
    lateinit var getMessageById: GetMessageByIdUseCase

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    lateinit var fileManager: FileManager

    @MockK
    lateinit var getMessageAsset: GetMessageAssetUseCase

    @MockK
    lateinit var updateAssetMessageDownloadStatus: UpdateAssetMessageDownloadStatusUseCase

    @MockK
    lateinit var toggleReaction: ToggleReactionUseCase

    @MockK
    lateinit var resetSession: ResetSessionUseCase

    @MockK
    lateinit var conversationAudioMessagePlayer: ConversationAudioMessagePlayer

    @MockK
    lateinit var getConversationUnreadEventsCount: GetConversationUnreadEventsCountUseCase

    private val viewModel: ConversationMessagesViewModel by lazy {
        ConversationMessagesViewModel(
            savedStateHandle,
            observeConversationDetails,
            getMessageAsset,
            getMessageById,
            updateAssetMessageDownloadStatus,
            fileManager,
            TestDispatcherProvider(),
            getMessagesForConversationUseCase,
            toggleReaction,
            resetSession,
            conversationAudioMessagePlayer,
            getConversationUnreadEventsCount
        )
    }

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(conversationId = conversationId)
        coEvery { toggleReaction(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { observeConversationDetails(any()) } returns flowOf()
        coEvery { getMessagesForConversationUseCase(any(), any()) } returns messagesChannel.consumeAsFlow()
        coEvery { getConversationUnreadEventsCount(any()) } returns GetConversationUnreadEventsCountUseCase.Result.Success(0L)
        coEvery { updateAssetMessageDownloadStatus(any(), any(), any()) } returns UpdateDownloadStatusResult.Success
    }

    fun withSuccessfulOpenAssetMessage(
        assetMimeType: String,
        assetName: String,
        assetDataPath: Path,
        assetSize: Long,
        messageId: String
    ) = apply {
        val assetBundle = AssetBundle(assetMimeType, assetDataPath, assetSize, assetName, AttachmentType.fromMimeTypeString(assetMimeType))
        viewModel.showOnAssetDownloadedDialog(assetBundle, messageId)
        every { fileManager.openWithExternalApp(any(), any(), any()) }.answers {
            viewModel.hideOnAssetDownloadedDialog()
        }
    }

    fun withConversationUnreadEventsCount(result: GetConversationUnreadEventsCountUseCase.Result) = apply {
        coEvery { getConversationUnreadEventsCount(any()) } returns result
    }

    fun withGetMessageByIdReturning(message: Message) = apply {
        coEvery { getMessageById(any(), any()) } returns GetMessageByIdUseCase.Result.Success(message)
    }

    fun withGetMessageAssetUseCaseReturning(decodedAssetPath: Path, assetSize: Long, assetName: String = "name") = apply {
        coEvery { getMessageAsset(any(), any()) } returns CompletableDeferred(
            MessageAssetResult.Success(
                decodedAssetPath,
                assetSize,
                assetName
            )
        )
    }

    fun withObservableAudioMessagesState(audioFlow: Flow<Map<String, AudioState>>) = apply {
        coEvery { conversationAudioMessagePlayer.observableAudioMessagesState } returns audioFlow
    }

    suspend fun withPaginatedMessagesReturning(pagingDataFlow: PagingData<UIMessage>) = apply {
        messagesChannel.send(pagingDataFlow)
    }

    suspend fun withResetSessionResult(resetSessionResult: ResetSessionResult = ResetSessionResult.Success) = apply {
        coEvery { resetSession(any(), any(), any()) } returns resetSessionResult
    }

    fun withSuccessfulSaveAssetMessage(
        assetMimeType: String,
        assetName: String,
        assetDataPath: Path,
        assetSize: Long,
        messageId: String
    ) = apply {
        val assetBundle = AssetBundle(assetMimeType, assetDataPath, assetSize, assetName, AttachmentType.fromMimeTypeString(assetMimeType))
        viewModel.showOnAssetDownloadedDialog(assetBundle, messageId)
        coEvery { fileManager.saveToExternalStorage(any(), any(), any(), any(), any()) }.answers {
            viewModel.hideOnAssetDownloadedDialog()
        }
    }

    fun arrange() = this to viewModel
}
