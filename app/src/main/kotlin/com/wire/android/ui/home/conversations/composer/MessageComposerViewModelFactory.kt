/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.conversation.MarkConversationAsReadLocallyUseCase
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.SendTypingEventUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.message.ephemeral.EnqueueMessageSelfDeletionUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class MessageComposerViewModelFactory(
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
    private val tempWritableAttachmentUriProvider: TempWritableAttachmentUriProvider,
    private val currentSessionFlowUseCase: CurrentSessionFlowUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val globalDataStore: GlobalDataStore,
) {
    fun create(conversationNavArgs: ConversationNavArgs): MessageComposerViewModel = MessageComposerViewModel(
        conversationNavArgs = conversationNavArgs,
        dispatchers = dispatchers,
        isFileSharingEnabled = isFileSharingEnabled,
        observeConversationInteractionAvailability = observeConversationInteractionAvailability,
        updateConversationReadDate = updateConversationReadDate,
        markConversationAsReadLocally = markConversationAsReadLocally,
        contactMapper = contactMapper,
        membersToMention = membersToMention,
        enqueueMessageSelfDeletion = enqueueMessageSelfDeletion,
        persistNewSelfDeletingStatus = persistNewSelfDeletingStatus,
        sendTypingEvent = sendTypingEvent,
        tempWritableAttachmentUriProvider = tempWritableAttachmentUriProvider,
        currentSessionFlowUseCase = currentSessionFlowUseCase,
        observeEstablishedCalls = observeEstablishedCalls,
        globalDataStore = globalDataStore,
    )
}
