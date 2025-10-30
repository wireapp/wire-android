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

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.InvalidLinkDialogState
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.VisitLinkDialogState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.android.util.FileManager
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
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.SendTypingEventUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.message.ephemeral.EnqueueMessageSelfDeletionUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class MessageComposerViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val dispatchers: DispatcherProvider,
    private val isFileSharingEnabled: IsFileSharingEnabledUseCase,
    private val observeConversationInteractionAvailability: ObserveConversationInteractionAvailabilityUseCase,
    private val updateConversationReadDate: UpdateConversationReadDateUseCase,
    private val contactMapper: ContactMapper,
    private val membersToMention: MembersToMentionUseCase,
    private val enqueueMessageSelfDeletion: EnqueueMessageSelfDeletionUseCase,
    private val persistNewSelfDeletingStatus: PersistNewSelfDeletionTimerUseCase,
    private val sendTypingEvent: SendTypingEventUseCase,
    private val fileManager: FileManager,
    private val kaliumFileSystem: KaliumFileSystem,
    private val currentSessionFlowUseCase: CurrentSessionFlowUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val globalDataStore: GlobalDataStore,
) : ViewModel() {

    var messageComposerViewState = mutableStateOf(MessageComposerViewState())
        private set

    var tempWritableVideoUri: Uri? = null
        private set

    var tempWritableImageUri: Uri? = null
        private set

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId

    var visitLinkDialogState: VisitLinkDialogState by mutableStateOf(
        VisitLinkDialogState.Hidden
    )

    var invalidLinkDialogState: InvalidLinkDialogState by mutableStateOf(
        InvalidLinkDialogState.Hidden
    )

    init {
        initTempWritableVideoUri()
        initTempWritableImageUri()
        observeIsTypingAvailable()
        setFileSharingStatus()
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
            tempWritableVideoUri =
                fileManager.getTempWritableVideoUri(kaliumFileSystem.rootCachePath)
        }
    }

    private fun initTempWritableImageUri() {
        viewModelScope.launch {
            tempWritableImageUri =
                fileManager.getTempWritableImageUri(kaliumFileSystem.rootCachePath)
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

    fun updateConversationReadDate(utcISO: String) {
        viewModelScope.launch(dispatchers.io()) {
            updateConversationReadDate(conversationId, Instant.parse(utcISO))
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
