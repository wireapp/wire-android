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
package com.wire.android.ui.home.conversationslist

import com.wire.android.BuildConfig
import com.wire.android.di.CurrentAccount
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UiTextResolver
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsWithEventsUseCase
import com.wire.kalium.logic.feature.conversation.RefreshConversationsWithoutMetadataUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class ConversationListViewModelFactory(
    private val dispatcher: DispatcherProvider,
    private val getConversationsPaginated: GetConversationsFromSearchUseCase,
    private val observeConversationListDetailsWithEvents: ObserveConversationListDetailsWithEventsUseCase,
    private val refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    private val refreshConversationsWithoutMetadata: RefreshConversationsWithoutMetadataUseCase,
    private val observeLegalHoldStateForSelfUser: ObserveLegalHoldStateForSelfUserUseCase,
    private val audioMessagePlayer: ConversationAudioMessagePlayer,
    @CurrentAccount private val currentAccount: UserId,
    private val userTypeMapper: UserTypeMapper,
    private val getSelfUser: GetSelfUserUseCase,
    private val uiTextResolver: UiTextResolver,
) {
    fun create(
        conversationsSource: ConversationsSource,
        usePagination: Boolean = BuildConfig.PAGINATED_CONVERSATION_LIST_ENABLED,
    ): ConversationListViewModelImpl = ConversationListViewModelImpl(
        conversationsSource = conversationsSource,
        usePagination = usePagination,
        dispatcher = dispatcher,
        getConversationsPaginated = getConversationsPaginated,
        observeConversationListDetailsWithEvents = observeConversationListDetailsWithEvents,
        refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
        refreshConversationsWithoutMetadata = refreshConversationsWithoutMetadata,
        observeLegalHoldStateForSelfUser = observeLegalHoldStateForSelfUser,
        audioMessagePlayer = audioMessagePlayer,
        currentAccount = currentAccount,
        userTypeMapper = userTypeMapper,
        getSelfUser = getSelfUser,
        uiTextResolver = uiTextResolver,
    )
}
