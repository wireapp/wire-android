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
package com.wire.android.ui.home.conversations

import androidx.lifecycle.SavedStateHandle
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelImpl
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import javax.inject.Inject

class ConversationCoreViewModelFactory @Inject constructor(
    private val conversationMessagesViewModelFactory: ConversationMessagesViewModel.Factory,
    private val messageComposerViewModelFactory: MessageComposerViewModel.Factory,
    private val sendMessageViewModelFactory: SendMessageViewModel.Factory,
    private val messageDraftViewModelFactory: MessageDraftViewModel.Factory,
    private val messageAttachmentsViewModelFactory: MessageAttachmentsViewModel.Factory,
    private val conversationMigrationViewModelFactory: ConversationMigrationViewModel.Factory,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val dispatchers: DispatcherProvider,
) {
    fun conversationMessagesViewModel(savedStateHandle: SavedStateHandle) =
        conversationMessagesViewModelFactory.create(savedStateHandle)

    fun messageComposerViewModel(savedStateHandle: SavedStateHandle) =
        messageComposerViewModelFactory.create(savedStateHandle)

    fun sendMessageViewModel(savedStateHandle: SavedStateHandle) =
        sendMessageViewModelFactory.create(savedStateHandle)

    fun messageDraftViewModel(savedStateHandle: SavedStateHandle) =
        messageDraftViewModelFactory.create(savedStateHandle)

    fun messageAttachmentsViewModel(savedStateHandle: SavedStateHandle) =
        messageAttachmentsViewModelFactory.create(savedStateHandle)

    fun conversationMigrationViewModel(savedStateHandle: SavedStateHandle) =
        conversationMigrationViewModelFactory.create(savedStateHandle)

    fun conversationAssetPathsViewModel() = ConversationAssetPathsViewModelImpl(
        getMessageAsset = getMessageAsset,
        dispatchers = dispatchers,
    )
}
