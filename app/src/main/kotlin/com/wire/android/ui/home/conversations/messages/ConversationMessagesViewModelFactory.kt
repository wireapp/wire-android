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
package com.wire.android.ui.home.conversations.messages

import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.ObserveAssetStatusesUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageTransferStatusUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ClearUsersTypingEventsUseCase
import com.wire.kalium.logic.feature.conversation.GetConversationUnreadEventsCountUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.FetchOlderNomadMessagesByConversationUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.GetSearchedConversationMessagePositionUseCase
import com.wire.kalium.logic.feature.message.ToggleReactionUseCase
import com.wire.kalium.logic.feature.sessionreset.ResetSessionUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class ConversationMessagesViewModelFactory(
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val getMessageByIdUseCase: GetMessageByIdUseCase,
    private val updateAssetMessageDownloadStatus: UpdateAssetMessageTransferStatusUseCase,
    private val observeAssetStatusesUseCase: ObserveAssetStatusesUseCase,
    private val assetFileGateway: ConversationAssetFileGateway,
    private val dispatchers: DispatcherProvider,
    private val getMessageForConversation: GetMessagesForConversationUseCase,
    private val fetchOlderNomadMessages: FetchOlderNomadMessagesByConversationUseCase,
    private val toggleReaction: ToggleReactionUseCase,
    private val resetSession: ResetSessionUseCase,
    private val audioMessagePlayer: ConversationAudioMessagePlayer,
    private val getConversationUnreadEventsCount: GetConversationUnreadEventsCountUseCase,
    private val clearUsersTypingEvents: ClearUsersTypingEventsUseCase,
    private val getSearchedConversationMessagePosition: GetSearchedConversationMessagePositionUseCase,
    private val deleteMessage: DeleteMessageUseCase,
    private val isWireCellFeatureEnabled: IsWireCellsEnabledUseCase,
) {
    fun create(conversationNavArgs: ConversationNavArgs): ConversationMessagesViewModel = ConversationMessagesViewModel(
        conversationNavArgs = conversationNavArgs,
        observeConversationDetails = observeConversationDetails,
        getMessageAsset = getMessageAsset,
        getMessageByIdUseCase = getMessageByIdUseCase,
        updateAssetMessageDownloadStatus = updateAssetMessageDownloadStatus,
        observeAssetStatusesUseCase = observeAssetStatusesUseCase,
        assetFileGateway = assetFileGateway,
        dispatchers = dispatchers,
        getMessageForConversation = getMessageForConversation,
        fetchOlderNomadMessages = fetchOlderNomadMessages,
        toggleReaction = toggleReaction,
        resetSession = resetSession,
        audioMessagePlayer = audioMessagePlayer,
        getConversationUnreadEventsCount = getConversationUnreadEventsCount,
        clearUsersTypingEvents = clearUsersTypingEvents,
        getSearchedConversationMessagePosition = getSearchedConversationMessagePosition,
        deleteMessage = deleteMessage,
        isWireCellFeatureEnabled = isWireCellFeatureEnabled,
    )
}
