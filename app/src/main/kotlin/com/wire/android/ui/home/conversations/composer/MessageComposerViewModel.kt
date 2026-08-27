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

package com.wire.android.ui.home.conversations.composer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.InvalidLinkDialogState
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.VisitLinkDialogState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.EMPTY
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.conversation.InteractionAvailability
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.conversation.IsInteractionAvailableResult
import com.wire.kalium.logic.feature.conversation.IsSelfUserViewerOnConversationUseCase
import com.wire.kalium.logic.feature.conversation.MarkConversationAsReadLocallyUseCase
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.SendTypingEventUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.message.ephemeral.EnqueueMessageSelfDeletionUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import com.wire.content.external.CaptureKind
import com.wire.content.external.CaptureTargetProvider
import com.wire.content.external.ExternalContentReference
import com.wire.content.external.PlatformResult
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.ui.home.conversations.ConversationCoreManualViewModelFactoryGroup

@Suppress("LongParameterList", "TooManyFunctions")
@WireAssistedViewModelBinding(ConversationCoreManualViewModelFactoryGroup::class)
class MessageComposerViewModel @AssistedInject constructor(
    private val dispatchers: DispatcherProvider,
    private val isFileSharingEnabled: IsFileSharingEnabledUseCase,
    private val observeConversationInteractionAvailability: ObserveConversationInteractionAvailabilityUseCase,
    private val updateConversationReadDate: UpdateConversationReadDateUseCase,
    private val markConversationAsReadLocally: MarkConversationAsReadLocallyUseCase,
    private val contactMapper: ContactMapper,
    private val membersToMention: MembersToMentionUseCase,
    private val enqueueMessageSelfDeletion: EnqueueMessageSelfDeletionUseCase,
    private val persistNewSelfDeletingStatus: PersistNewSelfDeletionTimerUseCase,
    private val sendTypingEvent: SendTypingEventUseCase,
    private val captureTargetProvider: CaptureTargetProvider,
    private val kaliumFileSystem: KaliumFileSystem,
    private val currentSessionFlowUseCase: CurrentSessionFlowUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val globalDataStore: GlobalDataStore,
    private val isSelfUserViewerOnConversation: IsSelfUserViewerOnConversationUseCase,
    @Assisted navigationArgs: ConversationNavArgs,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(navigationArgs: ConversationNavArgs): MessageComposerViewModel
    }

    var messageComposerViewState = mutableStateOf(MessageComposerViewState())
        private set

    var tempWritableVideoReference: ExternalContentReference? = null
        private set

    var tempWritableImageReference: ExternalContentReference? = null
        private set

    private val conversationNavArgs = navigationArgs
    val conversationId: QualifiedID = conversationNavArgs.conversationId

    var visitLinkDialogState: VisitLinkDialogState by mutableStateOf(
        VisitLinkDialogState.Hidden
    )

    var invalidLinkDialogState: InvalidLinkDialogState by mutableStateOf(
        InvalidLinkDialogState.Hidden
    )

    private var lastReadInstant: Instant? = null

    init {
        initTempWritableVideoUri()
        initTempWritableImageUri()
        observeIsTypingAvailable()
        setFileSharingStatus()
        checkAttachmentOptionsAvailability()
        getEnterToSendState()
        observeCallState()
    }

    private fun getEnterToSendState() {
        viewModelScope.launch {
            globalDataStore.enterToSendFlow().first().also {
                messageComposerViewState.value = messageComposerViewState.value.copy(enterToSend = it)
            }
        }
    }

    private fun initTempWritableVideoUri() {
        viewModelScope.launch {
            tempWritableVideoReference =
                (
                    captureTargetProvider.createTarget(CaptureKind.VIDEO, kaliumFileSystem.rootCachePath)
                        as? PlatformResult.Success<ExternalContentReference>
                    )?.value
        }
    }

    private fun initTempWritableImageUri() {
        viewModelScope.launch {
            tempWritableImageReference =
                (
                    captureTargetProvider.createTarget(CaptureKind.IMAGE, kaliumFileSystem.rootCachePath)
                        as? PlatformResult.Success<ExternalContentReference>
                    )?.value
        }
    }

    private fun observeIsTypingAvailable() = viewModelScope.launch {
        currentSessionFlowUseCase()
            .flatMapLatest {
                when (it) {
                    is CurrentSessionResult.Success -> {
                        observeConversationInteractionAvailability(conversationId)
                            .mapLatest { result ->
                                when (result) {
                                    is IsInteractionAvailableResult.Failure -> InteractionAvailability.DISABLED
                                    is IsInteractionAvailableResult.Success -> result.interactionAvailability
                                }
                            }
                    }

                    else -> flowOf(InteractionAvailability.DISABLED)
                }
            }
            .collectLatest {
                messageComposerViewState.value = messageComposerViewState.value.copy(interactionAvailability = it)
            }
    }

    fun searchMembersToMention(searchQuery: String) {
        viewModelScope.launch(dispatchers.io()) {
            val members = membersToMention(conversationId, searchQuery).map {
                contactMapper.fromOtherUser(it.user as OtherUser)
            }

            messageComposerViewState.value =
                messageComposerViewState.value.copy(
                    mentionSearchResult = members,
                    mentionSearchQuery = searchQuery,
                )
        }
    }

    fun clearMentionSearchResult() {
        messageComposerViewState.value =
            messageComposerViewState.value.copy(
                mentionSearchResult = emptyList(),
                mentionSearchQuery = String.EMPTY,
            )
    }

    private fun setFileSharingStatus() {
        viewModelScope.launch {
            messageComposerViewState.value = when (isFileSharingEnabled().state) {
                FileSharingStatus.Value.Disabled ->
                    messageComposerViewState.value.copy(isFileSharingEnabled = false)

                is FileSharingStatus.Value.EnabledSome,
                FileSharingStatus.Value.EnabledAll ->
                    messageComposerViewState.value.copy(isFileSharingEnabled = true)
            }
        }
    }

    private fun checkAttachmentOptionsAvailability() {
        viewModelScope.launch {
            val areAttachmentOptionsEnabled = isSelfUserViewerOnConversation(conversationId)
            messageComposerViewState.value = messageComposerViewState.value.copy(
                areAttachmentOptionsEnabled = areAttachmentOptionsEnabled
            )
        }
    }

    fun updateConversationReadDate(instant: Instant) {
        lastReadInstant = instant
        viewModelScope.launch(NonCancellable) {
            updateConversationReadDate(conversationId, instant)
        }
    }

    /**
     * Called when the user leaves the conversation.
     * Immediately updates the local read date to clear unread badges
     * without waiting for the debounced update to complete.
     * If the user viewed messages, uses the last read timestamp.
     */
    fun onConversationClosed() {
        lastReadInstant?.let { instant ->
            viewModelScope.launch(NonCancellable) {
                markConversationAsReadLocally(conversationId, instant)
            }
        }
    }

    fun startSelfDeletion(uiMessage: UIMessage) {
        enqueueMessageSelfDeletion(conversationId, uiMessage.header.messageId)
    }

    fun updateSelfDeletingMessages(newSelfDeletionTimer: SelfDeletionTimer) =
        viewModelScope.launch {
            persistNewSelfDeletingStatus(conversationId, newSelfDeletionTimer)
        }

    fun hideVisitLinkDialog() {
        visitLinkDialogState = VisitLinkDialogState.Hidden
    }

    fun hideInvalidLinkError() {
        invalidLinkDialogState = InvalidLinkDialogState.Hidden
    }

    fun sendTypingEvent(typingIndicatorMode: TypingIndicatorMode) {
        viewModelScope.launch {
            sendTypingEvent(conversationId, typingIndicatorMode)
        }
    }

    private fun observeCallState() = viewModelScope.launch {
        observeEstablishedCalls()
            .map { it.isNotEmpty() }
            .distinctUntilChanged()
            .collectLatest { hasOngoingCalls ->
                messageComposerViewState.value = messageComposerViewState.value.copy(isCallOngoing = hasOngoingCalls)
            }
    }
}
