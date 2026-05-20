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
package com.wire.android.ui.home.conversations.details.editselfdeletingmessages

import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.messagetimer.UpdateMessageTimerUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dev.zacsweers.metro.Inject

@Inject
class EditSelfDeletingMessagesViewModelFactory(
    private val dispatcher: DispatcherProvider,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val updateMessageTimer: UpdateMessageTimerUseCase,
    private val selfUser: ObserveSelfUserUseCase,
    private val conversationDetails: ObserveConversationDetailsUseCase,
) {
    fun create(args: EditSelfDeletingMessagesNavArgs): EditSelfDeletingMessagesViewModel = EditSelfDeletingMessagesViewModel(
        editSelfDeletingMessagesNavArgs = args,
        dispatcher = dispatcher,
        observeConversationMembers = observeConversationMembers,
        observeSelfDeletionTimerSettingsForConversation = observeSelfDeletionTimerSettingsForConversation,
        updateMessageTimer = updateMessageTimer,
        selfUser = selfUser,
        conversationDetails = conversationDetails,
    )
}
