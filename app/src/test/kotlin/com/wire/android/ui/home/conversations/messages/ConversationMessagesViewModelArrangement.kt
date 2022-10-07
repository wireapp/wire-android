package com.wire.android.ui.home.conversations.messages

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.util.FileManager
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.asset.UpdateDownloadStatusResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.ToggleReactionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import okio.Path

class ConversationMessagesViewModelArrangement {

    val conversationId: ConversationId = ConversationId("some-dummy-value", "some.dummy.domain")

    private val messagesChannel = Channel<PagingData<UIMessage>>(capacity = Channel.UNLIMITED)

    val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)

    @MockK
    lateinit var qualifiedIdMapper: QualifiedIdMapper

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

    private val viewModel: ConversationMessagesViewModel by lazy {
        ConversationMessagesViewModel(
            qualifiedIdMapper,
            savedStateHandle,
            observeConversationDetails,
            getMessageAsset,
            getMessageById,
            updateAssetMessageDownloadStatus,
            fileManager,
            TestDispatcherProvider(),
            getMessagesForConversationUseCase,
            toggleReaction
        )
    }

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.get<String>(any()) } returns conversationId.toString()
        every {
            qualifiedIdMapper.fromStringToQualifiedID("some-dummy-value@some.dummy.domain")
        } returns QualifiedID("some-dummy-value", "some.dummy.domain")
        coEvery { observeConversationDetails(any()) } returns flowOf()
        coEvery { getMessagesForConversationUseCase(any()) } returns messagesChannel.consumeAsFlow()
        coEvery { updateAssetMessageDownloadStatus(any(), any(), any()) } returns UpdateDownloadStatusResult.Success
    }

    fun withSuccessfulOpenAssetMessage(
        assetName: String,
        assetDataPath: Path,
        assetSize: Long,
        messageId: String
    ) = apply {
        viewModel.showOnAssetDownloadedDialog(assetName, assetDataPath, assetSize, messageId)
        every { fileManager.openWithExternalApp(any(), any(), any()) }.answers {
            viewModel.hideOnAssetDownloadedDialog()
        }
    }

    fun withGetMessageByIdReturning(message: Message) = apply {
        coEvery { getMessageById(any(), any()) } returns GetMessageByIdUseCase.Result.Success(message)
    }

    fun withGetMessageAssetUseCaseReturning(decodedAssetPath: Path, assetSize: Long) = apply {
        coEvery { getMessageAsset(any(), any()) } returns MessageAssetResult.Success(decodedAssetPath, assetSize)
    }

    suspend fun withPaginatedMessagesReturning(pagingDataFlow: PagingData<UIMessage>) = apply {
        messagesChannel.send(pagingDataFlow)
    }

    fun withSuccessfulSaveAssetMessage(
        assetName: String,
        assetDataPath: Path,
        assetSize: Long,
        messageId: String
    ) = apply {
        viewModel.showOnAssetDownloadedDialog(assetName, assetDataPath, assetSize, messageId)
        coEvery { fileManager.saveToExternalStorage(any(), any(), any(), any()) }.answers {
            viewModel.hideOnAssetDownloadedDialog()
        }
    }

    suspend fun withConversationDetailUpdate(conversationDetails: ConversationDetails) = apply {
        coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow().map {
            ObserveConversationDetailsUseCase.Result.Success(it)
        }
        conversationDetailsChannel.send(conversationDetails)
        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("id@domain")
        } returns QualifiedID("id", "domain")
    }

    fun arrange() = this to viewModel
}
